package application.usecase;

import application.services.DiscountService;
import domain.repository.CartRepository;
import domain.repository.ProductRepository;
import domain.shared.Money;
import domain.shared.Code;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified use case for online cart operations (add, view, remove) with discount support
 */
public final class OnlineCartUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;

    public OnlineCartUseCase(CartRepository cartRepository, ProductRepository productRepository, DiscountService discountService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.discountService = discountService;
    }

    /**
     * Add a product to the cart
     */
    public void addToCart(long userId, String productCode, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        // Verify product exists
        var product = productRepository.findByCode(new Code(productCode));
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productCode);
        }

        long cartId = cartRepository.getOrCreateCart(userId);
        cartRepository.upsertItem(cartId, productCode, qty);
    }

    /**
     * View cart contents with discount information
     */
    public CartView viewCart(long userId) {
        long cartId = cartRepository.getOrCreateCart(userId);
        List<CartRepository.CartItem> items = cartRepository.items(cartId);

        List<CartLineView> lines = items.stream()
                .map(this::toCartLineView)
                .collect(Collectors.toList());

        Money subtotal = lines.stream()
                .map(line -> line.lineTotal)
                .reduce(Money.of(0), Money::plus);

        Money discountedSubtotal = lines.stream()
                .map(line -> line.discountedLineTotal)
                .reduce(Money.of(0), Money::plus);

        Money totalDiscount = subtotal.minus(discountedSubtotal);

        return new CartView(lines, subtotal, discountedSubtotal, totalDiscount);
    }

    /**
     * Remove an item from the cart
     */
    public void removeFromCart(long userId, String productCode) {
        long cartId = cartRepository.getOrCreateCart(userId);
        cartRepository.removeItem(cartId, productCode);
    }

    private CartLineView toCartLineView(CartRepository.CartItem item) {
        var product = productRepository.findByCode(new Code(item.productCode));
        if (product.isEmpty()) {
            throw new IllegalStateException("Product not found: " + item.productCode);
        }

        Money unitPrice = product.get().price();
        Money lineTotal = unitPrice.times(item.qty);

        // Get active discounts for this product
        var activeDiscounts = discountService.getActiveDiscountsForProduct(item.productCode, LocalDate.now());

        String discountInfo = null;
        Money discountedUnitPrice = unitPrice;
        Money discountedLineTotal = lineTotal;

        if (!activeDiscounts.isEmpty()) {
            // Apply the best discount (highest discount amount)
            var bestDiscount = activeDiscounts.stream()
                .max((d1, d2) -> {
                    Money discount1 = d1.calculateDiscountAmount(unitPrice);
                    Money discount2 = d2.calculateDiscountAmount(unitPrice);
                    return discount1.amount().compareTo(discount2.amount());
                })
                .orElse(null);

            if (bestDiscount != null) {
                discountedUnitPrice = bestDiscount.applyDiscount(unitPrice);
                discountedLineTotal = discountedUnitPrice.times(item.qty);

                String discountType = bestDiscount.getType().name().equals("PERCENTAGE")
                    ? bestDiscount.getValue() + "% off"
                    : "$" + bestDiscount.getValue() + " off";

                discountInfo = discountType +
                    (bestDiscount.getDescription() != null ? " (" + bestDiscount.getDescription() + ")" : "");
            }
        }

        return new CartLineView(
            product.get().code().value(),
            product.get().name(),
            unitPrice,
            discountedUnitPrice,
            item.qty,
            lineTotal,
            discountedLineTotal,
            discountInfo
        );
    }

    public static class CartView {
        public final List<CartLineView> lines;
        public final Money subtotal;
        public final Money discountedSubtotal;
        public final Money totalDiscount;

        public CartView(List<CartLineView> lines, Money subtotal, Money discountedSubtotal, Money totalDiscount) {
            this.lines = lines;
            this.subtotal = subtotal;
            this.discountedSubtotal = discountedSubtotal;
            this.totalDiscount = totalDiscount;
        }

        // Backward compatibility constructor
        public CartView(List<CartLineView> lines, Money subtotal) {
            this(lines, subtotal, subtotal, Money.of(0));
        }

        // Getter methods for test compatibility
        public List<CartLineView> lines() {
            return lines;
        }

        public Money subtotal() {
            return subtotal;
        }

        public Money discountedSubtotal() {
            return discountedSubtotal;
        }

        public Money totalDiscount() {
            return totalDiscount;
        }
    }

    public static class CartLineView {
        public final String productCode;
        public final String productName;
        public final Money unitPrice;
        public final Money discountedUnitPrice;
        public final int qty;
        public final Money lineTotal;
        public final Money discountedLineTotal;
        public final String discountInfo;

        public CartLineView(String productCode, String productName, Money unitPrice, Money discountedUnitPrice,
                           int qty, Money lineTotal, Money discountedLineTotal, String discountInfo) {
            this.productCode = productCode;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.discountedUnitPrice = discountedUnitPrice;
            this.qty = qty;
            this.lineTotal = lineTotal;
            this.discountedLineTotal = discountedLineTotal;
            this.discountInfo = discountInfo;
        }

        // Backward compatibility constructor
        public CartLineView(String productCode, String productName, Money unitPrice, int qty, Money lineTotal) {
            this(productCode, productName, unitPrice, unitPrice, qty, lineTotal, lineTotal, null);
        }
    }
}
