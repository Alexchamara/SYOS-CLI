package cli.manager.screens;

import application.services.ReportService;
import application.reports.dto.OrderHeaderRow;
import application.reports.dto.OrderLineRow;
import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;
import cli.manager.table.TablePrinter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class OrderReportScreen {
    private final ReportService reportService;
    private final Scanner scanner;

    public OrderReportScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("\n=== Order Report (Web Orders) ===");

        ReportFilters filters = FiltersPrompt.promptForDateFilter(scanner);

        while (true) {
            List<OrderHeaderRow> data = reportService.orders(filters);

            // Convert to Object[] for table printing
            List<Object[]> rows = data.stream()
                .map(row -> new Object[]{
                    row.rowNo(), row.serial(), row.type(), row.store(),
                    row.createdAt(), row.netTotal(), row.paymentSummary()
                })
                .toList();

            TablePrinter.printOrderHeaderTable(rows, 0, 1);

            System.out.println("\nFilters: " + formatFilters(filters));
            System.out.print("Command ([d]etails [f]ilter [q]uit): ");

            String command = scanner.nextLine().trim().toLowerCase();

            if (command.startsWith("d ") || command.startsWith("details ")) {
                try {
                    String[] parts = command.split("\\s+", 2);
                    if (parts.length < 2) {
                        System.out.println("Usage: details <order_id>");
                        continue;
                    }
                    long orderId = Long.parseLong(parts[1]);
                    showOrderDetails(orderId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid order ID format");
                }
            } else {
                switch (command) {
                    case "f", "filter" -> {
                        filters = FiltersPrompt.promptForDateFilter(scanner);
                    }
                    case "q", "quit" -> { return; }
                    default -> System.out.println("Unknown command: " + command);
                }
            }
        }
    }

    private void showOrderDetails(long orderId) {
        List<OrderLineRow> lines = reportService.orderLines(orderId);

        if (lines.isEmpty()) {
            System.out.println("No order found with ID: " + orderId);
            return;
        }

        System.out.println("\n=== Order Details (ID: " + orderId + ") ===");

        List<Object[]> rows = lines.stream()
            .map(line -> new Object[]{
                line.productCode(), line.name(), line.qty(),
                line.unitPrice(), line.lineTotal()
            })
            .toList();

        TablePrinter.printOrderLineTable(rows);

        BigDecimal total = lines.stream()
            .map(OrderLineRow::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Total: $" + total);

        System.out.print("Press Enter to continue...");
        new Scanner(System.in).nextLine();
    }

    private String formatFilters(ReportFilters filters) {
        if (filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY) {
            return String.format("Date: %s", filters.day());
        } else {
            return String.format("Date Range: %s to %s", filters.fromDate(), filters.toDate());
        }
    }
}
