package cli.manager.discount;

import application.usecase.DiscountManagementUseCase;
import domain.pricing.Discount;
import domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * CLI interface for discount management - accessible only to managers
 */
public class DiscountManagementCLI {
    private final DiscountManagementUseCase discountUseCase;
    private final User currentUser;
    private Scanner scanner;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DiscountManagementCLI(User currentUser, DiscountManagementUseCase discountUseCase) {
        this.discountUseCase = discountUseCase;
        this.currentUser = currentUser;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        this.scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> createDiscount();
                case "2" -> updateDiscount();
                case "3" -> deleteDiscount();
                case "4" -> viewAllDiscounts();
                case "5" -> viewDiscountsByBatch();
                case "6" -> viewActiveDiscounts();
                case "0" -> { return; }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== DISCOUNT MANAGEMENT ===");
        System.out.println("1) Create New Discount");
        System.out.println("2) Update Discount");
        System.out.println("3) Delete Discount");
        System.out.println("4) View All Discounts");
        System.out.println("5) View Discounts by Batch");
        System.out.println("6) View Active Discounts");
        System.out.println("0) Back to Manager Menu");
        System.out.print("Choose option: ");
    }

    private void createDiscount() {
        System.out.println("\n=== CREATE NEW DISCOUNT ===");

        try {
            long batchId = getLongInput("Enter Batch ID: ");
            Discount.DiscountType type = getDiscountType();
            BigDecimal value = getDiscountValue(type);
            LocalDate startDate = getDateInput("Enter start date (yyyy-MM-dd): ");
            LocalDate endDate = getDateInput("Enter end date (yyyy-MM-dd): ");
            boolean isActive = getBooleanInput("Is discount active? (y/n): ");

            System.out.print("Enter description (optional): ");
            String description = scanner.nextLine().trim();
            if (description.isEmpty()) {
                description = null;
            }

            DiscountManagementUseCase.CreateDiscountRequest request =
                new DiscountManagementUseCase.CreateDiscountRequest(
                    batchId, type, value, startDate, endDate, isActive, description
                );

            DiscountManagementUseCase.CreateDiscountResult result =
                discountUseCase.createDiscount(request, currentUser);

            if (result.isSuccess()) {
                System.out.println("Discount created successfully!");
                printDiscountDetails(result.getDiscount());
            } else {
                System.out.println("Failed to create discount: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updateDiscount() {
        System.out.println("\n=== UPDATE DISCOUNT ===");

        try {
            long discountId = getLongInput("Enter Discount ID to update: ");

            // First check if discount exists
            var existingDiscounts = discountUseCase.getAllDiscounts(currentUser);
            var existingDiscount = existingDiscounts.stream()
                .filter(d -> d.getId() == discountId)
                .findFirst();

            if (existingDiscount.isEmpty()) {
                System.out.println("Discount not found!");
                return;
            }

            System.out.println("Current discount details:");
            printDiscountDetails(existingDiscount.get());
            System.out.println("\nEnter new values (press Enter to keep current value):");

            long batchId = getLongInputWithDefault("Enter Batch ID", existingDiscount.get().getBatchId());
            Discount.DiscountType type = getDiscountTypeWithDefault(existingDiscount.get().getType());
            BigDecimal value = getDiscountValueWithDefault(type, existingDiscount.get().getValue());
            LocalDate startDate = getDateInputWithDefault("Enter start date", existingDiscount.get().getStartDate());
            LocalDate endDate = getDateInputWithDefault("Enter end date", existingDiscount.get().getEndDate());
            boolean isActive = getBooleanInputWithDefault("Is discount active?", existingDiscount.get().isActive());

            System.out.print("Enter description [" + existingDiscount.get().getDescription() + "]: ");
            String description = scanner.nextLine().trim();
            if (description.isEmpty()) {
                description = existingDiscount.get().getDescription();
            }

            DiscountManagementUseCase.UpdateDiscountRequest request =
                new DiscountManagementUseCase.UpdateDiscountRequest(
                    discountId, batchId, type, value, startDate, endDate, isActive, description
                );

            DiscountManagementUseCase.UpdateDiscountResult result =
                discountUseCase.updateDiscount(request, currentUser);

            if (result.isSuccess()) {
                System.out.println("Discount updated successfully!");
                printDiscountDetails(result.getDiscount());
            } else {
                System.out.println("Failed to update discount: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void deleteDiscount() {
        System.out.println("\n=== DELETE DISCOUNT ===");

        try {
            long discountId = getLongInput("Enter Discount ID to delete: ");

            System.out.print("Are you sure you want to delete this discount? (y/n): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (!confirmation.equals("y") && !confirmation.equals("yes")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            DiscountManagementUseCase.DeleteDiscountResult result =
                discountUseCase.deleteDiscount(discountId, currentUser);

            if (result.isSuccess()) {
                System.out.println("Discount deleted successfully!");
            } else {
                System.out.println("Failed to delete discount: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
    }

    private void viewAllDiscounts() {
        System.out.println("\n=== ALL DISCOUNTS ===");

        try {
            List<Discount> discounts = discountUseCase.getAllDiscounts(currentUser);

            if (discounts.isEmpty()) {
                System.out.println("No discounts found.");
                return;
            }

            printDiscountsTable(discounts);

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
    }

    private void viewDiscountsByBatch() {
        System.out.println("\n=== DISCOUNTS BY BATCH ===");

        try {
            long batchId = getLongInput("Enter Batch ID: ");
            List<Discount> discounts = discountUseCase.getDiscountsForBatch(batchId, currentUser);

            if (discounts.isEmpty()) {
                System.out.println("No discounts found for batch " + batchId);
                return;
            }

            printDiscountsTable(discounts);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewActiveDiscounts() {
        System.out.println("\n=== ACTIVE DISCOUNTS ===");

        try {
            List<Discount> allDiscounts = discountUseCase.getAllDiscounts(currentUser);
            List<Discount> activeDiscounts = allDiscounts.stream()
                .filter(d -> d.isValidForDate(LocalDate.now()))
                .toList();

            if (activeDiscounts.isEmpty()) {
                System.out.println("No active discounts found.");
                return;
            }

            printDiscountsTable(activeDiscounts);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void printDiscountsTable(List<Discount> discounts) {
        System.out.printf("%-5s %-8s %-12s %-10s %-12s %-12s %-8s %-30s%n",
            "ID", "Batch", "Type", "Value", "Start", "End", "Active", "Description");
        System.out.println("=".repeat(100));

        for (Discount discount : discounts) {
            System.out.printf("%-5d %-8d %-12s %-10s %-12s %-12s %-8s %-30s%n",
                discount.getId(),
                discount.getBatchId(),
                discount.getType(),
                formatDiscountValue(discount),
                discount.getStartDate().format(dateFormatter),
                discount.getEndDate().format(dateFormatter),
                discount.isActive() ? "Yes" : "No",
                discount.getDescription() != null ? discount.getDescription() : "");
        }
    }

    private void printDiscountDetails(Discount discount) {
        System.out.println("Discount Details:");
        System.out.println("  ID: " + discount.getId());
        System.out.println("  Batch ID: " + discount.getBatchId());
        System.out.println("  Type: " + discount.getType());
        System.out.println("  Value: " + formatDiscountValue(discount));
        System.out.println("  Start Date: " + discount.getStartDate().format(dateFormatter));
        System.out.println("  End Date: " + discount.getEndDate().format(dateFormatter));
        System.out.println("  Active: " + (discount.isActive() ? "Yes" : "No"));
        System.out.println("  Description: " + (discount.getDescription() != null ? discount.getDescription() : "None"));
    }

    private String formatDiscountValue(Discount discount) {
        if (discount.getType() == Discount.DiscountType.PERCENTAGE) {
            return discount.getValue() + "%";
        } else {
            return "Rs." + discount.getValue();
        }
    }

    // Helper methods for input
    private long getLongInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Long.parseLong(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private long getLongInputWithDefault(String prompt, long defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, using default value: " + defaultValue);
            return defaultValue;
        }
    }

    private Discount.DiscountType getDiscountType() {
        while (true) {
            System.out.print("Enter discount type (1=Percentage, 2=Fixed Amount): ");
            String input = scanner.nextLine().trim();
            switch (input) {
                case "1" -> { return Discount.DiscountType.PERCENTAGE; }
                case "2" -> { return Discount.DiscountType.FIXED_AMOUNT; }
                default -> System.out.println("Please enter 1 or 2.");
            }
        }
    }

    private Discount.DiscountType getDiscountTypeWithDefault(Discount.DiscountType defaultType) {
        System.out.print("Enter discount type (1=Percentage, 2=Fixed Amount) [" + defaultType + "]: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultType;
        }
        switch (input) {
            case "1" -> { return Discount.DiscountType.PERCENTAGE; }
            case "2" -> { return Discount.DiscountType.FIXED_AMOUNT; }
            default -> {
                System.out.println("Invalid input, using default: " + defaultType);
                return defaultType;
            }
        }
    }

    private BigDecimal getDiscountValue(Discount.DiscountType type) {
        while (true) {
            String prompt = type == Discount.DiscountType.PERCENTAGE
                ? "Enter percentage (0-100): "
                : "Enter fixed amount: $";
            System.out.print(prompt);

            try {
                BigDecimal value = new BigDecimal(scanner.nextLine().trim());
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Value must be positive.");
                    continue;
                }
                if (type == Discount.DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    System.out.println("Percentage cannot exceed 100%.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private BigDecimal getDiscountValueWithDefault(Discount.DiscountType type, BigDecimal defaultValue) {
        String prompt = type == Discount.DiscountType.PERCENTAGE
            ? "Enter percentage (0-100) [" + defaultValue + "%]: "
            : "Enter fixed amount [$" + defaultValue + "]: $";
        System.out.print(prompt);

        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }

        try {
            BigDecimal value = new BigDecimal(input);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Invalid value, using default: " + defaultValue);
                return defaultValue;
            }
            if (type == Discount.DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
                System.out.println("Invalid percentage, using default: " + defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, using default: " + defaultValue);
            return defaultValue;
        }
    }

    private LocalDate getDateInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim(), dateFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("Please enter date in format yyyy-MM-dd (e.g., 2025-09-21).");
            }
        }
    }

    private LocalDate getDateInputWithDefault(String prompt, LocalDate defaultDate) {
        System.out.print(prompt + " [" + defaultDate.format(dateFormatter) + "]: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultDate;
        }

        try {
            return LocalDate.parse(input, dateFormatter);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format, using default: " + defaultDate.format(dateFormatter));
            return defaultDate;
        }
    }

    private boolean getBooleanInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            switch (input) {
                case "y", "yes", "true" -> { return true; }
                case "n", "no", "false" -> { return false; }
                default -> System.out.println("Please enter y/n or yes/no.");
            }
        }
    }

    private boolean getBooleanInputWithDefault(String prompt, boolean defaultValue) {
        System.out.print(prompt + " [" + (defaultValue ? "y" : "n") + "]: ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.isEmpty()) {
            return defaultValue;
        }

        switch (input) {
            case "y", "yes", "true" -> { return true; }
            case "n", "no", "false" -> { return false; }
            default -> {
                System.out.println("Invalid input, using default: " + (defaultValue ? "yes" : "no"));
                return defaultValue;
            }
        }
    }
}
