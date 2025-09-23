package cli.manager.screens;

import application.services.ReportService;
import application.reports.dto.ReshelveRow;
import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;
import cli.manager.table.TablePrinter;

import java.util.List;
import java.util.Scanner;

public class ReshelveScreen {
    private final ReportService reportService;
    private final Scanner scanner;

    public ReshelveScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("\n=== End-of-Day Reshelve List ===");

        ReportFilters filters = FiltersPrompt.promptForDateFilter(scanner);

        while (true) {
            List<ReshelveRow> data = reportService.reshelve(filters);

            // Convert to Object[] for table printing
            List<Object[]> rows = data.stream()
                .map(row -> new Object[]{
                    row.id(), row.happenedAt(), row.productCode(),
                    row.productName(), row.fromLocation(), row.toLocation(),
                    row.quantity(), row.note()
                })
                .toList();

            // Show all data without pagination
            TablePrinter.printReshelveTable(rows, 0, 1);

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
