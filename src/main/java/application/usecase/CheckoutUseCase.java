package application.usecase;

import application.services.AvailabilityService;
import application.services.BillNumberService;
import application.services.OrderIdService;
import application.services.ShortageEventService;
import domain.billing.Bill;
import domain.billing.BillLine;
import domain.events.EventPublisher;
import domain.events.LowStockEvent;
import domain.inventory.StockLocation;
import domain.model.CardDetails;
import domain.policies.BatchSelectionStrategy;
import domain.policies.FefoStrategy;
import domain.pricing.DiscountPolicy;
import domain.repository.*;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;
import infrastructure.concurrency.Tx;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified checkout use case supporting both cash (POS) and card (web) payments
 */
public final class CheckoutUseCase {
    private final Tx tx;

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AvailabilityService availabilityService;
    private final QuoteUseCase quoteUseCase;
    private final ShortageEventService shortageService;

    // Cash checkout dependencies
    private final BillRepository billRepository;
    private final BatchSelectionStrategy batchStrategy;
    private final BillNumberService billNumberService;
    private final EventPublisher eventPublisher;

    // Card checkout dependencies
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final FefoStrategy fefoStrategy;

    private final int lowStockThreshold = 50;

    public CheckoutUseCase(
            Tx tx,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            AvailabilityService availabilityService,
            QuoteUseCase quoteUseCase,
            ShortageEventService shortageService,
            BillRepository billRepository,
            BatchSelectionStrategy batchStrategy,
            BillNumberService billNumberService,
            EventPublisher eventPublisher,
            CartRepository cartRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            FefoStrategy fefoStrategy) {
        this.tx = tx;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.availabilityService = availabilityService;
        this.quoteUseCase = quoteUseCase;
        this.shortageService = shortageService;
        this.billRepository = billRepository;
        this.batchStrategy = batchStrategy;
        this.billNumberService = billNumberService;
        this.eventPublisher = eventPublisher;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.fefoStrategy = fefoStrategy;
    }

    // Additional constructor for cash-only scenarios
    public CheckoutUseCase(
            Tx tx,
            ProductRepository productRepository,
            BillRepository billRepository,
            BatchSelectionStrategy batchStrategy,
            BillNumberService billNumberService,
            InventoryRepository inventoryRepository,
            EventPublisher eventPublisher) {
        this.tx = tx;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.billRepository = billRepository;
        this.batchStrategy = batchStrategy;
        this.billNumberService = billNumberService;
        this.eventPublisher = eventPublisher;

        // Set optional dependencies to null for cash-only mode
        this.availabilityService = null;
        this.quoteUseCase = null;
        this.shortageService = null;
        this.cartRepository = null;
        this.orderRepository = null;
        this.paymentRepository = null;
        this.fefoStrategy = null;
    }

    /**
     * Cash checkout for POS/Counter transactions
     */
    public Bill checkoutCash(List<CashItem> cart, long cashCents, StockLocation location, DiscountPolicy discountPolicy, String scope) {
        return tx.inTx(con -> {
            // 1) Build bill lines from product master
            String serial = billNumberService.next("COUNTER");
            var builder = new Bill.Builder().serial(serial);
            List<BillLine> lines = new ArrayList<>();

            for (CashItem it : cart) {
                var prod = productRepository.findByCode(new Code(it.code())).orElseThrow(() -> new IllegalArgumentException("Unknown product: " + it.code()));
                var line = new BillLine(prod.code(), prod.name(), new Quantity(it.qty()), prod.price());
                builder.addLine(line);
                lines.add(line);
            }

            // 2) Apply discount from the chosen policy
            var discount = discountPolicy.discountFor(lines);

            // 3) Set discount & cash; build validates Cash >= Total
            builder.discount(discount).cash(Money.of(cashCents));
            Bill bill = builder.build();

            // 4) Persist bill
            billRepository.save(con, bill);

            // 5) Deduct inventory per line.
            for (var l : bill.lines()) {
                Code code = l.productCode();
                int qty = l.qty().value();
                if (location == StockLocation.SHELF) {
                    int remain = inventoryRepository.remainingQuantity(con, l.productCode().value(), location.name());
                    if (remain < lowStockThreshold) {
                        eventPublisher.publish(new LowStockEvent(l.productCode(), remain));
                    }
                }
                if (location == StockLocation.SHELF) {
                    // Try to deduct as much as possible from SHELF
                    int takenShelf = batchStrategy.deductUpTo(con, code, qty, StockLocation.SHELF);
                    int remaining = qty - takenShelf;
                    if (remaining > 0) {
                        batchStrategy.deduct(con, code, remaining, StockLocation.WEB);
                    }
                } else {
                    batchStrategy.deduct(con, code, qty, location);
                }
            }

            return bill;
        });
    }

    /**
     * Card checkout for Web shop transactions
     */
    public CardCheckoutResult checkoutCard(long userId, DiscountPolicy discountPolicy, CardDetails cardDetails) {
        return tx.inTx(con -> {
            // 1) Load WEB cart items
            long cartId = cartRepository.getOrCreateCart(userId);
            List<CartRepository.CartItem> cartItems = cartRepository.items(cartId);

            if (cartItems.isEmpty()) {
                throw new IllegalStateException("Cart is empty");
            }

            // 2) Availability check at WEB for whole cart
            for (CartRepository.CartItem item : cartItems) {
                int available = availabilityService.available(item.productCode, StockLocation.WEB);
                if (available < item.qty) {
                    throw new InsufficientStockException(
                        String.format("Insufficient stock for %s. Available: %d, Required: %d",
                                     item.productCode, available, item.qty));
                }
            }

            // 3) Create Preview Bill using Quote builder
            List<QuoteUseCase.Item> quoteItems = cartItems.stream()
                    .map(item -> {
                        var product = productRepository.findByCode(new Code(item.productCode));
                        if (product.isEmpty()) {
                            throw new IllegalArgumentException("Product not found: " + item.productCode);
                        }
                        return new QuoteUseCase.Item(item.productCode, product.get().name(),
                                                   product.get().price(), item.qty);
                    })
                    .collect(Collectors.toList());

            QuoteUseCase.Quote quote = quoteUseCase.quote(quoteItems, discountPolicy);

            // 4) Get next ONLINE serial
            long billSerial = orderRepository.nextBillSerial("ONLINE");

            // 5) Save PREVIEW order + lines
            long orderId = orderRepository.savePreview("ONLINE", "WEB", userId, quote);
            orderRepository.saveLines(orderId, quote.lines());

            // 6) Validate card
            if (!cardDetails.isValid()) {
                throw new InvalidCardException("Card details are invalid");
            }

            // 7. Record payment
            String authRef = "AUTH-" + billSerial;
            paymentRepository.saveCard(orderId, cardDetails.getLast4(), authRef);

            // 8. Deduct inventory from WEB using FEFO → FIFO strategy
            for (CartRepository.CartItem item : cartItems) {
                fefoStrategy.deduct(con, new Code(item.productCode), item.qty, StockLocation.WEB);
            }

            // 9. Save FINAL order
            orderRepository.saveFinal(orderId, quote);

            // 10. Clear the cart
            cartRepository.clearCart(cartId);

            // 11. Check for low stock and trigger notifications
            for (CartRepository.CartItem item : cartItems) {
                int remaining = availabilityService.available(item.productCode, StockLocation.WEB);
                if (remaining < 50) {
                    shortageService.record("Low stock alert: " + item.productCode + " at WEB location has only " + remaining + " units remaining");
                }
            }

            return new CardCheckoutResult(orderId, billSerial, quote);
        });
    }

    // Data transfer objects
    public record CashItem(String code, int qty) {}

    public static class CardCheckoutResult {
        public final long orderId;
        public final long billSerial;
        public final QuoteUseCase.Quote quote;
        public final String formattedOrderId;

        public CardCheckoutResult(long orderId, long billSerial, QuoteUseCase.Quote quote) {
            this.orderId = orderId;
            this.billSerial = billSerial;
            this.quote = quote;
            this.formattedOrderId = OrderIdService.generateOrderId("WEB");
        }
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    public static class InvalidCardException extends RuntimeException {
        public InvalidCardException(String message) {
            super(message);
        }
    }
}
