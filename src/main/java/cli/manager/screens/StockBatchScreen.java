package cli.manager.screens;

import application.services.ReportService;
import application.reports.dto.StockBatchRow;
import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;
import cli.manager.table.TablePrinter;

import java.util.List;
import java.util.Scanner;

public class StockBatchScreen {
    private final ReportService reportService;
    private final Scanner scanner;

    public StockBatchScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("\n=== Stock Report (Batch-wise) ===");

        ReportFilters filters = FiltersPrompt.promptForDateFilter(scanner);

        while (true) {
            List<StockBatchRow> data = reportService.stockBatches(filters);

            // Convert to Object[] for table printing
            List<Object[]> rows = data.stream()
                .map(row -> new Object[]{
                    row.code(), row.name(), row.batchId(),
                    row.expiry(), row.qty(), row.location()
                })
                .toList();

            // Show all data without pagination
            TablePrinter.printStockBatchTable(rows, 0, 1);

            System.out.println("\nFilters: " + formatFilters(filters));
            System.out.print("Command ([f]ilter [q]uit): ");

            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "f", "filter" -> {
                    filters = FiltersPrompt.promptForDateFilter(scanner);
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
