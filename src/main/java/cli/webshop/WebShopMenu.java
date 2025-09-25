package cli.webshop;

import application.usecase.*;
import application.services.DiscountService;
import domain.model.CardDetails;
import domain.pricing.DiscountPolicy;
import domain.pricing.NoDiscount;
import domain.pricing.PercentDiscount;
import domain.pricing.BatchDiscountPolicy;

import java.util.Scanner;

/**
 * CLI interface for WEB shop functionality with automatic discount support
 */
public final class WebShopMenu {
    private final Scanner scanner;
    private final AuthenticationUseCase authUseCase;
    private final SearchProductUseCase searchUseCase;
    private final OnlineCartUseCase onlineCartUseCase;
    private final CheckoutUseCase checkoutUseCase;
    private final DiscountService discountService;

    private Long currentUserId = null;

    public WebShopMenu(
            Scanner scanner,
            AuthenticationUseCase authUseCase,
            AuthenticationUseCase unused,
            SearchProductUseCase searchUseCase,
            OnlineCartUseCase onlineCartUseCase,
            CheckoutUseCase checkoutUseCase,
            DiscountService discountService) {
        this.scanner = scanner;
        this.authUseCase = authUseCase;
        this.searchUseCase = searchUseCase;
        this.onlineCartUseCase = onlineCartUseCase;
        this.checkoutUseCase = checkoutUseCase;
        this.discountService = discountService;
    }

    public void start() {
        System.out.println("\n=== WEB SHOP ===");

        while (true) {
            displayMenu();

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (currentUserId == null) {
                    // Not logged in menu: 1=Register, 2=Login, 3=Search, 9=Exit
                    switch (choice) {
                        case 1 -> register();
                        case 2 -> login();
                        case 3 -> searchProducts();
                        case 9 -> {
                            System.out.println("Exiting WEB shop...");
                            return;
                        }
                        default -> System.out.println("Invalid choice. Please try again.");
                    }
                } else {
                    // Logged in menu: 1=Search, 2=ViewCart, 3=AddCart, 4=RemoveCart, 5=Checkout, 6=Logout, 9=Exit
                    switch (choice) {
                        case 1 -> searchProducts();
                        case 2 -> viewCart();
                        case 3 -> addToCart();
                        case 4 -> removeFromCart();
                        case 5 -> checkout();
                        case 6 -> logout();
                        case 9 -> {
                            System.out.println("Exiting WEB shop...");
                            return;
                        }
                        default -> System.out.println("Invalid choice. Please try again.");
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n--- WEB SHOP MENU ---");
        if (currentUserId == null) {
            System.out.println("Not logged in");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Search Products");
            System.out.println("9. Exit");
        } else {
            System.out.println("Logged in as user ID: " + currentUserId);
            System.out.println("1. Search Products");
            System.out.println("2. View Cart");
            System.out.println("3. Add to Cart");
            System.out.println("4. Remove from Cart");
            System.out.println("5. Checkout (CARD)");
            System.out.println("6. Logout");
            System.out.println("9. Exit");
        }
        System.out.print("Choose an option: ");
    }

    private void register() {
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Full Name: ");
        String fullName = scanner.nextLine().trim();

        try {
            long userId = authUseCase.registerWebUser(email, password, fullName);
            System.out.println("Registration successful! User ID: " + userId);
            System.out.println("You can now login with your email and password.");
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void login() {
        while (currentUserId == null) {
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            try {
                var session = authUseCase.loginWebUser(email, password);
                currentUserId = session.userId();
                System.out.println("Login successful! Welcome back.");
            } catch (Exception e) {
                System.out.println("Login failed: " + e.getMessage());
                System.out.println("\n1. Try again");
                System.out.println("2. Return to main menu");
                System.out.print("Choose option (1 or 2): ");
                String retryChoice = scanner.nextLine().trim();

                if ("2".equals(retryChoice)) {
                    System.out.println("Returning to main menu...");
                    return;
                }
            }
        }
    }

    private void searchProducts() {
        System.out.println("1. Search by name");
        System.out.println("2. Search by category");
        System.out.print("Choose search type: ");

        try {
            int searchType = Integer.parseInt(scanner.nextLine().trim());

            if (searchType == 1) {
                System.out.print("Enter product name: ");
                String searchTerm = scanner.nextLine().trim();

                var results = searchUseCase.searchByName(searchTerm);
                displaySearchResults(results);

            } else if (searchType == 2) {
                System.out.print("Enter category: ");
                String category = scanner.nextLine().trim();

                var results = searchUseCase.searchByCategory(category);
                displaySearchResults(results);

            } else {
                System.out.println("Invalid search type.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    private void displaySearchResults(java.util.List<SearchProductUseCase.ProductSearchResult> results) {
        if (results.isEmpty()) {
            System.out.println("No products found.");
            return;
        }

        System.out.println("\n--- Search Results ---");
        System.out.printf("%-15s %-30s %-10s %-10s %-10s%n", "Code", "Name", "Price", "Stock", "Available");
        System.out.println("-".repeat(75));

        for (var result : results) {
            System.out.printf("%-15s %-30s Rs.%-8.2f %-10d %-10s%n",
                    result.code,
                    result.name,
                    result.priceCents / 100.0,
                    result.webStock,
                    result.inStock ? "Yes" : "No");
        }
    }

    private void viewCart() {
        requireLogin();

        try {
            var cartView = onlineCartUseCase.viewCart(currentUserId);

            if (cartView.lines.isEmpty()) {
                System.out.println("Your cart is empty.");
                return;
            }

            System.out.println("\n--- Your Cart ---");
            System.out.printf("%-15s %-30s %-10s %-8s %-10s%n", "Code", "Name", "Price", "Qty", "Total");
            System.out.println("-".repeat(75));

            for (var line : cartView.lines) {
                System.out.printf("%-15s %-30s Rs.%-8.2f %-8d Rs.%-8.2f%n",
                        line.productCode,
                        line.productName,
                        line.unitPrice.cents() / 100.0,
                        line.qty,
                        line.lineTotal.cents() / 100.0);
            }

            System.out.println("-".repeat(75));
            System.out.printf("Subtotal: Rs.%.2f%n", cartView.subtotal.cents() / 100.0);

        } catch (Exception e) {
            System.out.println("Error viewing cart: " + e.getMessage());
        }
    }

    private void addToCart() {
        requireLogin();

        System.out.println("Add product to cart:");
        System.out.println("1. Enter product code directly");
        System.out.println("2. Search by product name");
        System.out.print("Choose option: ");

        try {
            int option = Integer.parseInt(scanner.nextLine().trim());
            String productCode = null;

            if (option == 1) {
                System.out.print("Product code: ");
                productCode = scanner.nextLine().trim();
            } else if (option == 2) {
                System.out.print("Enter product name to search: ");
                String searchTerm = scanner.nextLine().trim();

                var results = searchUseCase.searchByName(searchTerm);
                if (results.isEmpty()) {
                    System.out.println("No products found matching '" + searchTerm + "'");
                    return;
                }

                System.out.println("\n--- Search Results ---");
                System.out.printf("%-4s %-15s %-30s %-10s %-10s%n", "#", "Code", "Name", "Price", "Stock");
                System.out.println("-".repeat(75));

                for (int i = 0; i < results.size(); i++) {
                    var result = results.get(i);
                    System.out.printf("%-4d %-15s %-30s Rs.%-8.2f %-10d%n",
                            (i + 1),
                            result.code,
                            result.name,
                            result.priceCents / 100.0,
                            result.webStock);
                }

                System.out.print("Select product number (1-" + results.size() + "): ");
                int selection = Integer.parseInt(scanner.nextLine().trim());

                if (selection < 1 || selection > results.size()) {
                    System.out.println("Invalid selection.");
                    return;
                }

                productCode = results.get(selection - 1).code;
                System.out.println("Selected: " + results.get(selection - 1).name + " (" + productCode + ")");
            } else {
                System.out.println("Invalid option.");
                return;
            }

            System.out.print("Quantity: ");
            int qty = Integer.parseInt(scanner.nextLine().trim());

            onlineCartUseCase.addToCart(currentUserId, productCode, qty);
            System.out.println("Product added to cart successfully!");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Error adding to cart: " + e.getMessage());
        }
    }

    private void removeFromCart() {
        requireLogin();

        System.out.print("Product code to remove: ");
        String productCode = scanner.nextLine().trim();

        try {
            onlineCartUseCase.removeFromCart(currentUserId, productCode);
            System.out.println("Product removed from cart successfully!");
        } catch (Exception e) {
            System.out.println("Error removing from cart: " + e.getMessage());
        }
    }

    private void checkout() {
        requireLogin();

        try {
            var cartView = onlineCartUseCase.viewCart(currentUserId);

            if (cartView.lines.isEmpty()) {
                System.out.println("Your cart is empty. Cannot checkout.");
                return;
            }

            DiscountPolicy discountPolicy = new BatchDiscountPolicy(discountService);
            System.out.println("Checking for automatic discounts...");

            displayPreBill(cartView, discountPolicy);

            System.out.print("Proceed to payment? (y/n): ");
            String proceed = scanner.nextLine().trim().toLowerCase();

            if (!"y".equals(proceed) && !"yes".equals(proceed)) {
                System.out.println("Checkout cancelled.");
                return;
            }

            CardDetails cardDetails = getCardDetails();
            if (cardDetails == null) {
                return; // User cancelled or invalid card
            }

            var result = checkoutUseCase.checkoutCard(currentUserId, discountPolicy, cardDetails);

            displayFinalBill(result, cardDetails);

        } catch (CheckoutUseCase.InsufficientStockException e) {
            System.out.println("Checkout failed: " + e.getMessage());
        } catch (CheckoutUseCase.InvalidCardException e) {
            System.out.println("Payment failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Checkout error: " + e.getMessage());
        }
    }

    private void displayPreBill(OnlineCartUseCase.CartView cartView, DiscountPolicy discountPolicy) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                        PRE-BILL SUMMARY");
        System.out.println("=".repeat(70));

        System.out.printf("%-15s %-25s %-10s %-8s %-12s%n",
            "Product Code", "Product Name", "Unit Price", "Qty", "Line Total");
        System.out.println("-".repeat(70));

        for (var line : cartView.lines) {
            System.out.printf("%-15s %-25s Rs.%-8.2f %-8d Rs.%-10.2f%n",
                    line.productCode,
                    truncate(line.productName, 24),
                    line.unitPrice.cents() / 100.0,
                    line.qty,
                    line.lineTotal.cents() / 100.0);
        }

        System.out.println("-".repeat(70));

        double subtotal = cartView.subtotal.cents() / 100.0;
        System.out.printf("%-55s Rs.%12.2f%n", "Subtotal:", subtotal);

        if (cartView.totalDiscount != null && cartView.totalDiscount.cents() > 0) {
            double discountAmount = cartView.totalDiscount.cents() / 100.0;
            double finalTotal = cartView.discountedSubtotal.cents() / 100.0;

            System.out.printf("%-55s Rs.%12.2f%n", "Automatic Discount Applied:", discountAmount);
            System.out.println("-".repeat(70));
            System.out.printf("%-55s Rs.%12.2f%n", "TOTAL AFTER DISCOUNT:", finalTotal);
        } else {
            if (discountPolicy instanceof PercentDiscount percentDiscount) {
                double discountPercent = percentDiscount.getPercentage();
                double discountAmount = subtotal * (discountPercent / 100.0);
                double finalTotal = subtotal - discountAmount;

                System.out.printf("%-55s %10.1f%%%n", "Manual Discount Applied:", discountPercent);
                System.out.printf("%-55s Rs.%12.2f%n", "Discount Amount:", discountAmount);
                System.out.println("-".repeat(70));
                System.out.printf("%-55s Rs.%12.2f%n", "TOTAL AFTER DISCOUNT:", finalTotal);
            } else {
                System.out.printf("%-55s %14s%n", "Discount Applied:", "None");
                System.out.println("-".repeat(70));
                System.out.printf("%-55s Rs.%12.2f%n", "TOTAL AMOUNT:", subtotal);
            }
        }

        System.out.println("=".repeat(70));
        System.out.println("Payment Method: CARD");
        System.out.println("Location: WEB SHOP");
        System.out.println("=".repeat(70));
    }

    private void displayFinalBill(CheckoutUseCase.CardCheckoutResult result, CardDetails cardDetails) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                      CHECKOUT SUCCESSFUL");
        System.out.println("                         FINAL BILL");
        System.out.println("=".repeat(70));

        System.out.printf("Order ID: %s%n", result.formattedOrderId);
        System.out.printf("Bill Serial: O-%06d%n", result.billSerial);
        System.out.printf("Transaction Date: %s%n", java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Payment Method: CARD ****" + cardDetails.getLast4());
        System.out.println("Location: WEB SHOP");

        System.out.println("-".repeat(70));
        System.out.printf("%-15s %-25s %-10s %-8s %-12s%n",
            "Product Code", "Product Name", "Unit Price", "Qty", "Line Total");
        System.out.println("-".repeat(70));

        for (var line : result.quote.lines()) {
            System.out.printf("%-15s %-25s Rs.%-8.2f %-8d Rs.%-10.2f%n",
                    line.productCode().value(),
                    truncate(line.name(), 24),
                    line.unitPrice().cents() / 100.0,
                    line.qty().value(),
                    line.lineTotal().cents() / 100.0);
        }

        System.out.println("-".repeat(70));

        double subtotal = result.quote.subtotal().cents() / 100.0;
        double discount = result.quote.discount().cents() / 100.0;
        double total = result.quote.total().cents() / 100.0;

        System.out.printf("%-55s Rs.%12.2f%n", "Subtotal:", subtotal);

        if (discount > 0) {
            System.out.printf("%-55s Rs.%12.2f%n", "Discount:", discount);
        }

        System.out.println("=".repeat(70));
        System.out.printf("%-55s Rs.%12.2f%n", "TOTAL PAID:", total);
        System.out.println("=".repeat(70));

        System.out.println("Status: PAYMENT SUCCESSFUL");
        System.out.println("Your cart has been cleared.");
        System.out.println("Thank you for shopping with us!");
        System.out.println("=".repeat(70));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private CardDetails getCardDetails() {
        System.out.println("\n--- Card Payment ---");

        System.out.print("Card Number (16 digits): ");
        String cardNumber = scanner.nextLine().trim();

        System.out.print("Expiry Month (1-12): ");
        String expMonthStr = scanner.nextLine().trim();

        System.out.print("Expiry Year (YYYY): ");
        String expYearStr = scanner.nextLine().trim();

        System.out.print("CVV (3 digits): ");
        String cvv = scanner.nextLine().trim();

        try {
            int expMonth = Integer.parseInt(expMonthStr);
            int expYear = Integer.parseInt(expYearStr);

            CardDetails cardDetails = new CardDetails(cardNumber, expMonth, expYear, cvv);

            if (!cardDetails.isValid()) {
                System.out.println("Invalid card details. Please check:");
                System.out.println("- Card number must be exactly 16 digits");
                System.out.println("- CVV must be exactly 3 digits");
                System.out.println("- Expiry must be in the future");
                return null;
            }

            return cardDetails;

        } catch (NumberFormatException e) {
            System.out.println("Invalid expiry date format.");
            return null;
        }
    }

    private void logout() {
        currentUserId = null;
        System.out.println("Logged out successfully.");
    }

    private void requireLogin() {
        if (currentUserId == null) {
            throw new IllegalStateException("Please login first.");
        }
    }

    private void viewCartWithDiscounts() {
        if (currentUserId == null) {
            System.out.println("Please login first.");
            return;
        }

        try {
            var cartView = onlineCartUseCase.viewCart(currentUserId);

            if (cartView.lines.isEmpty()) {
                System.out.println("Your cart is empty.");
                return;
            }

            System.out.println("\n=== YOUR CART ===");
            System.out.printf("%-15s %-25s %-10s %-8s %-12s %-12s %-20s%n",
                "Product Code", "Product Name", "Unit Price", "Qty", "Line Total", "Final Price", "Discount");
            System.out.println("=".repeat(120));

            for (var line : cartView.lines) {
                String discountDisplay = line.discountInfo != null ? line.discountInfo : "No discount";

                System.out.printf("%-15s %-25s $%-9.2f %-8d $%-11.2f $%-11.2f %-20s%n",
                    line.productCode,
                    line.productName.length() > 25 ? line.productName.substring(0, 22) + "..." : line.productName,
                    line.unitPrice.amount().doubleValue(),
                    line.qty,
                    line.lineTotal.amount().doubleValue(),
                    line.discountedLineTotal.amount().doubleValue(),
                    discountDisplay.length() > 20 ? discountDisplay.substring(0, 17) + "..." : discountDisplay
                );
            }

            System.out.println("=".repeat(120));
            System.out.printf("%-70s $%-11.2f%n", "Subtotal:", cartView.subtotal.amount().doubleValue());

            if (cartView.totalDiscount.amount().doubleValue() > 0) {
                System.out.printf("%-70s -$%-10.2f%n", "Total Discount:", cartView.totalDiscount.amount().doubleValue());
                System.out.printf("%-70s $%-11.2f%n", "Final Total:", cartView.discountedSubtotal.amount().doubleValue());
            } else {
                System.out.printf("%-70s $%-11.2f%n", "Total:", cartView.subtotal.amount().doubleValue());
            }

        } catch (Exception e) {
            System.out.println("Error viewing cart: " + e.getMessage());
        }
    }
}
