package cli.manager;

import application.usecase.ReceiveFromSupplierUseCase;
import application.usecase.BatchManagementUseCase;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public final class ReceiveToMainCLI {
    private final ReceiveFromSupplierUseCase usecase;

    public ReceiveToMainCLI(ReceiveFromSupplierUseCase usecase) { this.usecase = usecase; }

    public void run() {
        var sc = new Scanner(System.in);
        while (true) {
            displayMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> receiveBatchFlow(sc);
                case "2" -> viewRecentReceipts();
                case "0" -> { return; }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println();
        System.out.println("=== RECEIVE TO MAIN STORE ===");
        System.out.println("1. Receive New Batch");
        System.out.println("2. View Recent Receipts");
        System.out.println("0. Back to Manager Menu");
        System.out.print("Select an option: ");
    }

    private void receiveBatchFlow(Scanner sc) {
        try {
            String code = promptProductCode(sc);
            int qty = promptQuantity(sc);
            LocalDate expiry = promptExpiry(sc);

            long id = usecase.receive(code, qty, expiry);
            System.out.println("Batch received successfully");
            System.out.println("Batch ID: " + id);
        } catch (Exception e) {
            System.out.println("Error receiving batch: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private String promptProductCode(Scanner sc) {
        while (true) {
            System.out.print("Product code: ");
            String code = sc.nextLine().trim();
            if (code.isEmpty()) {
                System.out.println("Product code cannot be empty");
                continue;
            }
            return code;
        }
    }

    private int promptQuantity(Scanner sc) {
        while (true) {
            System.out.print("Qty: ");
            String input = sc.nextLine().trim();
            int qty;
            try {
                qty = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid quantity");
                continue;
            }
            if (qty <= 0) {
                System.out.println("Quantity must be positive");
                continue;
            }
            return qty;
        }
    }

    private LocalDate promptExpiry(Scanner sc) {
        while (true) {
            System.out.print("Expiry (YYYY-MM-DD or 'none'): ");
            String exp = sc.nextLine().trim();
            if ("none".equalsIgnoreCase(exp)) return null;
            try {
                return LocalDate.parse(exp);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format");
            }
        }
    }

    private void viewRecentReceipts() {
        try {
            List<BatchManagementUseCase.BatchInfo> recent = usecase.getRecentBatches(10);
            if (recent.isEmpty()) {
                System.out.println("No recent receipts found");
                return;
            }
            System.out.println("Recent Receipts:");
            for (var b : recent) {
                System.out.println("- " + b.productCode() + " (qty: " + b.quantity() + ")");
            }
        } catch (Exception e) {
            System.out.println("Error fetching recent receipts: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}