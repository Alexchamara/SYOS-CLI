package cli.manager.screens;

import application.reports.ReportService;
import application.reports.dto.DailySalesRow;
import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;
import cli.manager.table.TablePrinter;

import java.util.List;
import java.util.Scanner;

public class DailySalesScreen {
    private final ReportService reportService;
    private final Scanner scanner;

    public DailySalesScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("\n=== Daily Sales Report ===");

        ReportFilters filters = FiltersPrompt.promptForDateFilter(scanner);

        while (true) {
            List<DailySalesRow> data = reportService.dailySales(filters);

            // Convert to Object[] for table printing
            List<Object[]> rows = data.stream()
                .map(row -> new Object[]{
                    row.code(), row.name(), row.location(), row.qtySold(),
                    row.gross(), row.discount(), row.net()
                })
                .toList();

            // Show all data without pagination
            TablePrinter.printDailySalesTable(rows, 0, 1);

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
