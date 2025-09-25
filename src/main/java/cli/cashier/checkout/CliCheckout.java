package cli.cashier.checkout;

import application.services.AvailabilityService;
import application.services.MainStoreService;
import application.services.ShortageEventService;
import application.services.DiscountService;
import application.usecase.CheckoutUseCase;
import application.usecase.CheckoutUseCase.CashItem;
import application.usecase.QuoteUseCase;
import cli.bill.BillPrinter;
import domain.inventory.StockLocation;
import domain.policies.BatchSelectionStrategy;
import domain.pricing.DiscountPolicy;
import domain.pricing.NoDiscount;
import domain.pricing.PercentDiscount;
import domain.pricing.BatchDiscountPolicy;
import domain.shared.Currency;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class CliCheckout {
    private final CheckoutUseCase checkout;
    private final BatchSelectionStrategy strategyDefault;
    private final QuoteUseCase quote;
    private final AvailabilityService availability;
    private final MainStoreService mainStoreService;
    private final ShortageEventService shortageEvents;
    private final DiscountService discountService;

    public CliCheckout(CheckoutUseCase checkout,
                       BatchSelectionStrategy strategyDefault,
                       QuoteUseCase quote,
                       AvailabilityService availability,
                       MainStoreService mainStoreService,
                       ShortageEventService shortageEvents,
                       DiscountService discountService) {
        this.checkout = checkout;
        this.strategyDefault = strategyDefault;
        this.quote = quote;
        this.availability = availability;
        this.mainStoreService = mainStoreService;
        this.shortageEvents = shortageEvents;
        this.discountService = discountService;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SYOS Checkout (CLI) ===");
        System.out.println("Commands:");
        System.out.println("  - Enter product code to add items");
        System.out.println("  - 'view' to see cart contents");
        System.out.println("  - 'remove' to remove items from cart");
        System.out.println("  - 'done' to proceed to checkout");

        List<CashItem> cart = new ArrayList<>();

        while (true) {
            System.out.print("\nCart (" + cart.size() + " items) > Enter command or product code: ");
            String input = sc.next().trim();

            if (input.equalsIgnoreCase("done")) {
                break;
            } else if (input.equalsIgnoreCase("view")) {
                viewCart(cart);
                continue;
            } else if (input.equalsIgnoreCase("remove")) {
                removeFromCart(sc, cart);
                continue;
            }

            String code = input;
            if (!quote.productExists(code)) {
                System.out.println("Invalid code: " + code);
                continue;
            }

            System.out.print("Qty: ");
            int qty = sc.nextInt();
            if (qty <= 0) {
                System.out.println("Qty must be > 0");
                continue;
            }

            int finalQuantity = handleItemRestocking(sc, code, qty);
            if (finalQuantity > 0) {
                addToCart(cart, code, finalQuantity);
                System.out.println("Added " + finalQuantity + " x " + code + " to cart");
            } else {
                System.out.println("Item not added to cart");
            }
        }

        if (cart.isEmpty()) {
            System.out.println("Cart empty, aborting.");
            return;
        }

        continueNormalCheckout(sc, cart);
    }

    /**
     * Display cart contents with item details including discount information
     */
    private void viewCart(List<CashItem> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty");
            return;
        }

        System.out.println("\n=== CART CONTENTS ===");
        System.out.printf("%-5s %-15s %-10s %-12s %-12s%n", "No.", "Product Code", "Quantity", "Unit Price", "Discounts");
        System.out.println("---------------------------------------------------------------");

        for (int i = 0; i < cart.size(); i++) {
            CashItem item = cart.get(i);

            var activeDiscounts = discountService.getActiveDiscountsForProduct(item.code(), java.time.LocalDate.now());
            String discountInfo = "None";

            if (!activeDiscounts.isEmpty()) {
                var bestDiscount = activeDiscounts.get(0); // Get the first active discount
                if (bestDiscount.getType() == domain.pricing.Discount.DiscountType.PERCENTAGE) {
                    discountInfo = bestDiscount.getValue() + "% off";
                } else {
                    discountInfo = "$" + bestDiscount.getValue() + " off";
                }
            }

            System.out.printf("%-5d %-15s %-10d %-12s %-12s%n",
                (i + 1), item.code(), item.qty(), "Check quote", discountInfo);
        }

        System.out.println("---------------------------------------------------------------");
        System.out.println("Total items: " + cart.size());

        try {
            var previewWithDiscounts = quote.preview(cart, new BatchDiscountPolicy(discountService));
            var previewWithoutDiscounts = quote.preview(cart, new NoDiscount());

            System.out.println("Subtotal (before discounts): " + Currency.formatSimple(previewWithoutDiscounts.total()));
            if (previewWithDiscounts.discount().amount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                System.out.println("Total discount: -" + Currency.formatSimple(previewWithDiscounts.discount()));
                System.out.println("Final total: " + Currency.formatSimple(previewWithDiscounts.total()));
            } else {
                System.out.println("No discounts available");
                System.out.println("Total: " + Currency.formatSimple(previewWithoutDiscounts.total()));
            }
        } catch (Exception e) {
            System.out.println("Could not calculate preview total");
        }
    }

    /**
     * Remove items from cart
     */
    private void removeFromCart(Scanner sc, List<CashItem> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty - nothing to remove");
            return;
        }

        viewCart(cart);

        System.out.print("Enter item number to remove (1-" + cart.size() + ") or 'cancel': ");
        String input = sc.next().trim();

        if (input.equalsIgnoreCase("cancel")) {
            return;
        }

        try {
            int itemNumber = Integer.parseInt(input);
            if (itemNumber < 1 || itemNumber > cart.size()) {
                System.out.println("Invalid item number");
                return;
            }

            CashItem removedItem = cart.remove(itemNumber - 1);
            System.out.println("Removed " + removedItem.qty() + " x " + removedItem.code() + " from cart");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number");
        }
    }

    /**
     * Add item to cart, consolidating quantities if the same product already exists
     */
    private void addToCart(List<CashItem> cart, String code, int quantity) {
        for (int i = 0; i < cart.size(); i++) {
            CashItem existingItem = cart.get(i);
            if (existingItem.code().equals(code)) {
                int newQuantity = existingItem.qty() + quantity;
                cart.set(i, new CashItem(code, newQuantity));
                System.out.println("Updated existing item. New quantity: " + newQuantity);
                return;
            }
        }

        cart.add(new CashItem(code, quantity));
    }

    /**
     * Handles the restocking logic
     */
    private int handleItemRestocking(Scanner sc, String code, int requestedQty) {
        // Step 1: Check SHELF stock
        int shelfStock = availability.available(code, StockLocation.SHELF);

        System.out.println("=== Stock Check for " + code + " ===");
        System.out.println("SHELF stock: " + shelfStock + ", Requested: " + requestedQty);

        if (shelfStock >= requestedQty) {
            System.out.println("Sufficient stock on SHELF, proceeding with checkout");
            return requestedQty;
        }

        // Step 2: SHELF insufficient, check MAIN_STORE
        System.out.println("Insufficient stock on SHELF (need " + (requestedQty - shelfStock) + " more)");

        int mainStoreStock = mainStoreService.getAvailableQuantity(code);
        System.out.println("MAIN_STORE stock: " + mainStoreStock);

        int neededFromMain = requestedQty - shelfStock;

        if (mainStoreStock >= neededFromMain) {
            // MAIN_STORE has enough, ask cashier to transfer
            System.out.println("MAIN_STORE has sufficient stock to fulfill the request");
            System.out.print("Transfer " + neededFromMain + " units from MAIN_STORE to SHELF? [y/N]: ");

            if ("y".equalsIgnoreCase(sc.next())) {
                try {
                    System.out.println("Transferring " + neededFromMain + " units from MAIN_STORE to SHELF...");
                    transferFromMainToShelf(code, neededFromMain);
                    System.out.println("Transfer completed successfully!");
                    return requestedQty;
                } catch (Exception e) {
                    System.out.println("Transfer failed: " + e.getMessage());
                    return handlePartialAvailability(sc, code, shelfStock);
                }
            } else {
                System.out.println("Transfer declined by cashier");
                return handlePartialAvailability(sc, code, shelfStock);
            }
        }

        // Step 3: MAIN_STORE insufficient, check WEB
        System.out.println("MAIN_STORE insufficient (has " + mainStoreStock + ", need " + neededFromMain + ")");

        int webStock = availability.available(code, StockLocation.WEB);
        System.out.println("WEB stock: " + webStock);

        int totalAvailable = shelfStock + mainStoreStock + webStock;
        System.out.println("Total available across all locations: " + totalAvailable);

        if (totalAvailable < requestedQty) {
            // Not enough stock anywhere
            System.out.println("Insufficient total stock across all locations!");
            String msg = String.format("URGENT: Product %s insufficient stock (SHELF: %d + MAIN: %d + WEB: %d = %d, Required: %d)",
                    code, shelfStock, mainStoreStock, webStock, totalAvailable, requestedQty);
            System.out.println(msg);

            try {
                shortageEvents.record(msg);
                System.out.println("Manager notified with high priority");
            } catch (Throwable e) {
                System.out.println("Failed to notify manager: " + e.getMessage());
            }

            return handlePartialAvailability(sc, code, totalAvailable);
        }

        // Calculate how much we need from WEB
        int stillNeedFromWeb = neededFromMain - mainStoreStock;

        if (webStock >= stillNeedFromWeb) {
            // WEB has enough, ask cashier for two-step transfer
            System.out.println("WEB has sufficient stock (" + webStock + " available)");
            System.out.println("This requires a two-step transfer:");
            System.out.println("  Step 1: WEB → MAIN_STORE (" + stillNeedFromWeb + " units)");
            System.out.println("  Step 2: MAIN_STORE → SHELF (" + neededFromMain + " units)");
            System.out.print("Proceed with two-step transfer? [y/N]: ");

            if ("y".equalsIgnoreCase(sc.next())) {
                try {
                    // Step 1: Transfer from WEB to MAIN_STORE
                    System.out.println("Step 1: Transferring " + stillNeedFromWeb + " units from WEB to MAIN_STORE...");
                    transferFromWebToMain(code, stillNeedFromWeb);
                    System.out.println("Step 1 completed");

                    // Step 2: Transfer from MAIN_STORE to SHELF
                    System.out.println("Step 2: Transferring " + neededFromMain + " units from MAIN_STORE to SHELF...");
                    transferFromMainToShelf(code, neededFromMain);
                    System.out.println("Step 2 completed");

                    System.out.println("Two-step transfer completed successfully!");
                    return requestedQty;

                } catch (Exception e) {
                    System.out.println("Transfer failed: " + e.getMessage());
                    return handlePartialAvailability(sc, code, shelfStock);
                }
            } else {
                System.out.println("Two-step transfer declined by cashier");
                return handlePartialAvailability(sc, code, shelfStock);
            }
        } else {
            // Even WEB doesn't have enough
            System.out.println("Even WEB doesn't have sufficient stock");
            return handlePartialAvailability(sc, code, totalAvailable);
        }
    }

    /**
     * Handles partial availability scenarios - offers cashier to use available quantity
     */
    private int handlePartialAvailability(Scanner sc, String code, int availableQty) {
        if (availableQty > 0) {
            System.out.print("Use only available quantity (" + availableQty + ") from SHELF? [y/N]: ");
            if ("y".equalsIgnoreCase(sc.next())) {
                return availableQty;
            }
        }
        return 0;
    }

    /**
     * Transfer stock from MAIN_STORE to SHELF
     */
    private void transferFromMainToShelf(String productCode, int quantity) throws Exception {
        availability.transferStock(productCode, StockLocation.MAIN_STORE, StockLocation.SHELF, quantity);
    }

    /**
     * Transfer stock from WEB to MAIN_STORE
     */
    private void transferFromWebToMain(String productCode, int quantity) throws Exception {
        availability.transferStock(productCode, StockLocation.WEB, StockLocation.MAIN_STORE, quantity);
    }

    private void continueNormalCheckout(Scanner sc, List<CashItem> cart) {
        StockLocation loc = StockLocation.SHELF;

        // Apply automatic batch-based discounts - no manual discount entry for cashiers
        DiscountPolicy policy = new BatchDiscountPolicy(discountService);

        System.out.println("Checking for automatic discounts...");

        // PREVIEW (pre-bill) with automatic discounts applied
        var q = quote.preview(cart, policy);
        BillPrinter.printPreview(q);

        if (q.discount().amount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            System.out.println("Automatic discounts applied: " + Currency.formatSimple(q.discount()));
        } else {
            System.out.println("No discounts available for current items.");
        }

        long needCents = q.total().amount().movePointRight(2).longValueExact();
        long cash;
        while (true) {
            System.out.print("Cash (cents, e.g., 10000 = " + Currency.SYMBOL + "100.00): ");
            cash = sc.nextLong();
            if (cash < needCents) {
                System.out.println("Insufficient cash. Need at least " + q.total().amount().toPlainString());
                continue;
            }
            break;
        }

        System.out.print("Confirm checkout? [y/N]: ");
        if (!"y".equalsIgnoreCase(sc.next())) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            var bill = checkout.checkoutCash(cart, cash, loc, policy, "COUNTER");
            BillPrinter.print(bill);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
    }
}
