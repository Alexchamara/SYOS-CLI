package cli.manager.screens;

import application.reports.ReportService;
import application.reports.dto.BillHeaderRow;
import application.reports.dto.BillLineRow;
import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;
import cli.manager.table.TablePrinter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class BillReportScreen {
    private final ReportService reportService;
    private final Scanner scanner;

    public BillReportScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("\n=== Bill Report (Finalized Bills) ===");

        ReportFilters filters = FiltersPrompt.promptForDateFilter(scanner);

        while (true) {
            List<BillHeaderRow> data = reportService.bills(filters);

            // Convert to Object[] for table printing
            List<Object[]> rows = data.stream()
                .map(row -> new Object[]{
                    row.rowNo(), row.serial(), row.type(), row.store(),
                    row.createdAt(), row.netTotal(), row.paymentSummary()
                })
                .toList();

            // Show all data without pagination
            TablePrinter.printBillHeaderTable(rows, 0, 1);

            System.out.println("\nFilters: " + formatFilters(filters));
            System.out.print("Command ([d]etails [f]ilter [q]uit): ");

            String command = scanner.nextLine().trim().toLowerCase();

            if (command.startsWith("d ") || command.startsWith("details ")) {
                try {
                    String[] parts = command.split("\\s+", 2);
                    if (parts.length < 2) {
                        System.out.println("Usage: details <bill_id>");
                        continue;
                    }
                    long billId = Long.parseLong(parts[1]);
                    showBillDetails(billId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid bill ID format");
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

    private void showBillDetails(long billId) {
        List<BillLineRow> lines = reportService.billLines(billId);

        if (lines.isEmpty()) {
            System.out.println("No bill found with ID: " + billId);
            return;
        }

        System.out.println("\n=== Bill Details (ID: " + billId + ") ===");

        List<Object[]> rows = lines.stream()
            .map(line -> new Object[]{
                line.productCode(), line.name(), line.qty(),
                line.unitPrice(), line.lineTotal()
            })
            .toList();

        BigDecimal total = lines.stream()
            .map(BillLineRow::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        TablePrinter.printBillLinesTable(rows, total);

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
