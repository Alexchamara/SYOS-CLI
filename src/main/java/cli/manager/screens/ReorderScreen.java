package cli.manager.screens;

import application.services.ReportService;
import application.reports.dto.ReorderRow;
import cli.manager.filters.ReportFilters;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class ReorderScreen {
    private final ReportService reportService;
    private final Scanner scanner;

    public ReorderScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        while (true) {
            displayMenu();

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> generateStandardReorderReport();
                case "2" -> generateCustomThresholdReport();
                case "0" -> { return; }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n=== REORDER REPORT ===");
        System.out.println("1. Standard Reorder (< 50)");
        System.out.println("2. Custom Threshold");
        System.out.println("0. Back");
        System.out.print("Choose an option: ");
    }

    private void generateStandardReorderReport() {
        try {
            ReportFilters filters = new ReportFilters(ReportFilters.DateMode.SINGLE_DAY, LocalDate.now(), null, null);
            List<ReorderRow> data = reportService.reorder(filters, 50);
            displayReorderReport(data, 50);
        } catch (Exception e) {
            System.out.println("Error generating reorder report: " + e.getMessage());
        }
    }

    private void generateCustomThresholdReport() {
        try {
            int threshold = promptForThreshold();
            if (threshold > 0) {
                ReportFilters filters = new ReportFilters(ReportFilters.DateMode.SINGLE_DAY, LocalDate.now(), null, null);
                List<ReorderRow> data = reportService.reorder(filters, threshold);
                displayReorderReport(data, threshold);
            }
        } catch (Exception e) {
            System.out.println("Error generating reorder report: " + e.getMessage());
        }
    }

    private int promptForThreshold() {
        while (true) {
            System.out.print("Enter reorder threshold: ");
            try {
                String input = scanner.nextLine().trim();
                int threshold = Integer.parseInt(input);
                if (threshold <= 0) {
                    System.out.println("Threshold must be positive. Please try again.");
                    continue;
                }
                return threshold;
            } catch (NumberFormatException e) {
                System.out.println("Invalid threshold. Please enter a valid number.");
            }
        }
    }

    private void displayReorderReport(List<ReorderRow> data, int threshold) {
        System.out.println("\n=== Reorder Report (Threshold: " + threshold + ") ===");

        if (data.isEmpty()) {
            System.out.println("No items need reordering at this threshold.");
            return;
        }

        System.out.printf("%-10s %-20s %-10s %-12s %-8s %-12s %-10s%n",
            "Code", "Name", "Batch", "Location", "Qty", "Expiry", "Status");
        System.out.println("=".repeat(85));

        for (ReorderRow row : data) {
            System.out.printf("%-10s %-20s %-10s %-12s %-8d %-12s %-10s%n",
                row.code(),
                truncate(row.name(), 20),
                row.batchId(),
                row.location(),
                row.quantity(),
                row.expiry(),
                row.status());
        }

        System.out.println("\nTotal items needing reorder: " + data.size());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}
