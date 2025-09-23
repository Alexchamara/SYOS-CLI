package cli.manager.screens;

import application.services.ReportService;
import application.reports.dto.ReorderRow;
import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;
import cli.manager.table.TablePrinter;

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
        System.out.println("\n=== Reorder Report ===");

        // Prompt for threshold
        System.out.print("Enter reorder threshold (default 50): ");
        String thresholdInput = scanner.nextLine().trim();
        int threshold = thresholdInput.isEmpty() ? 50 : Integer.parseInt(thresholdInput);

        ReportFilters filters = FiltersPrompt.promptForDateFilter(scanner);

        while (true) {
            List<ReorderRow> data = reportService.reorder(filters, threshold);

            // Convert to Object[] for table printing
            List<Object[]> rows = data.stream()
                .map(row -> new Object[]{
                    row.code(), row.name(), row.batchId(), row.location(),
                    row.quantity(), row.expiry(), row.status()
                })
                .toList();

            // Show all data without pagination
            TablePrinter.printReorderTable(rows, 0, 1);

            System.out.println("\nFilters: " + formatFilters(filters));
            System.out.print("Command ([f]ilter [t]hreshold [q]uit): ");

            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "f", "filter" -> {
                    filters = FiltersPrompt.promptForDateFilter(scanner);
                }
                case "t", "threshold" -> {
                    System.out.print("Enter new threshold: ");
                    try {
                        threshold = Integer.parseInt(scanner.nextLine().trim());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid threshold format");
                    }
                }
                case "q", "quit" -> { return; }
                default -> System.out.println("Unknown command: " + command);
            }
        }
    }

    private String formatFilters(ReportFilters filters) {
        if (filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY) {
            return String.format("Date: %s", filters.day());
        } else {
            return String.format("Date Range: %s to %s", filters.fromDate(), filters.toDate());
        }
    }
}
