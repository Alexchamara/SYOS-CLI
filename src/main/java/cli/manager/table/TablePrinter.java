package cli.manager.table;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import application.reports.dto.DailySalesRow;
import application.reports.dto.ReorderRow;
import application.reports.dto.StockBatchRow;

public final class TablePrinter {
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0");

    private TablePrinter() { throw new AssertionError("Utility class - do not instantiate"); }

    static {
        MONEY_FORMAT.setMinimumFractionDigits(2);
        MONEY_FORMAT.setMaximumFractionDigits(2);
    }

    public static void printDailySalesTable(List<DailySalesRow> rows) {
        String[] headers = {"Product Code", "Product Name", "Location", "Qty Sold", "Gross", "Discount", "Net"};
        int[] widths = {14, 25, 12, 10, 12, 12, 12};
        printTableHeader(headers, widths);

        if (rows == null || rows.isEmpty()) {
            printCustomEmptyMessage("No sales data to display", widths);
            return;
        }

        for (DailySalesRow r : rows) {
            printRow(new String[]{
                truncate(r.code(), widths[0]),
                truncate(r.name(), widths[1]),
                truncate(r.location(), widths[2]),
                formatNumber(r.qtySold()),
                formatMoney(r.gross()),
                formatMoney(r.discount()),
                formatMoney(r.net())
            }, widths);
        }
        // No page footer in tests; keep simple
    }

    public static void printReorderTable(List<ReorderRow> rows) {
        String[] headers = {"Product Code", "Product Name", "Batch ID", "Location", "Quantity", "Expiry", "Status"};
        int[] widths = {14, 25, 12, 12, 10, 12, 12};
        printTableHeader(headers, widths);

        if (rows == null || rows.isEmpty()) {
            printCustomEmptyMessage("No reorder data to display", widths);
            return;
        }

        for (ReorderRow r : rows) {
            printRow(new String[]{
                truncate(r.code(), widths[0]),
                truncate(r.name(), widths[1]),
                truncate(r.batchId(), widths[2]),
                truncate(r.location(), widths[3]),
                formatNumber(r.quantity()),
                r.expiry() != null ? r.expiry().toString() : "No Expiry",
                truncate(r.status(), widths[6])
            }, widths);
        }
    }

    public static void printStockBatchTable(List<StockBatchRow> rows) {
        String[] headers = {"Product Code", "Product Name", "Batch ID", "Expiry", "Received", "Quantity", "Location"};
        int[] widths = {14, 25, 12, 12, 16, 10, 12};
        printTableHeader(headers, widths);

        if (rows == null || rows.isEmpty()) {
            printCustomEmptyMessage("No batch data to display", widths);
            return;
        }

        for (StockBatchRow r : rows) {
            printRow(new String[]{
                truncate(r.code(), widths[0]),
                truncate(r.name(), widths[1]),
                truncate(r.batchId(), widths[2]),
                r.expiry() != null ? r.expiry().toString() : "No Expiry",
                r.receivedAt() != null ? r.receivedAt().toString() : "",
                formatNumber(r.qty()),
                truncate(r.location(), widths[6])
            }, widths);
        }
    }

    public static void printDailySalesTable(List<Object[]> rows, int pageIndex, int totalPages) {
        String[] headers = {"#", "Code", "Name", "Location", "Qty", "Gross", "Discount", "Net"};
        int[] widths = {4, 10, 26, 8, 8, 12, 12, 12};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    truncate((String) row[0], widths[1]),
                    truncate((String) row[1], widths[2]),
                    truncate((String) row[2], widths[3]),
                    formatNumber((Integer) row[3]),
                    formatMoney((BigDecimal) row[4]),
                    formatMoney((BigDecimal) row[5]),
                    formatMoney((BigDecimal) row[6])
                }, widths);
            }

            // Print totals
            BigDecimal totalQty = rows.stream()
                .map(row -> BigDecimal.valueOf((Integer) row[3]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalGross = rows.stream().map(row -> (BigDecimal) row[4]).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDiscount = rows.stream().map(row -> (BigDecimal) row[5]).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalNet = rows.stream().map(row -> (BigDecimal) row[6]).reduce(BigDecimal.ZERO, BigDecimal::add);

            printSeparator(widths);
            printRow(new String[]{
                "", "", "TOTALS",
                formatNumber(totalQty.intValue()),
                formatMoney(totalGross),
                formatMoney(totalDiscount),
                formatMoney(totalNet)
            }, widths);
        }

        printFooter(pageIndex, totalPages);
    }

    public static void printReshelveTable(List<Object[]> rows, int pageIndex, int totalPages) {
        String[] headers = {"#", "ID", "Time", "Code", "Product Name", "From", "To", "Qty", "Note"};
        int[] widths = {4, 6, 16, 10, 20, 8, 8, 6, 15};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    String.valueOf(row[0]),
                    truncate(row[1].toString(), widths[2]),
                    truncate((String) row[2], widths[3]),
                    truncate((String) row[3], widths[4]),
                    truncate((String) row[4], widths[5]),
                    truncate((String) row[5], widths[6]),
                    formatNumber((Integer) row[6]),
                    truncate((String) row[7], widths[8])
                }, widths);
            }

            int totalQty = rows.stream().mapToInt(row -> (Integer) row[6]).sum();

            printSeparator(widths);
            printRow(new String[]{
                "", "", "", "", "TOTALS", "", "",
                formatNumber(totalQty), ""
            }, widths);
        }

        printFooter(pageIndex, totalPages);
    }

    public static void printReorderTable(List<Object[]> rows, int pageIndex, int totalPages) {
        String[] headers = {"#", "Code", "Name", "BatchId", "Location", "Qty", "Expiry", "Status"};
        int[] widths = {4, 10, 20, 8, 10, 6, 10, 8};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    truncate((String) row[0], widths[1]),
                    truncate((String) row[1], widths[2]),
                    truncate((String) row[2], widths[3]),
                    truncate((String) row[3], widths[4]),
                    formatNumber((Integer) row[4]),
                    row[5] != null ? row[5].toString() : "No Expiry",
                    truncate((String) row[6], widths[7])
                }, widths);
            }

            int totalQty = rows.stream().mapToInt(row -> (Integer) row[4]).sum();

            printSeparator(widths);
            printRow(new String[]{
                "", "", "", "TOTALS", "",
                formatNumber(totalQty), "", ""
            }, widths);
        }

        printFooter(pageIndex, totalPages);
    }

    public static void printStockBatchTable(List<Object[]> rows, int pageIndex, int totalPages) {
        String[] headers = {"#", "Code", "Name", "BatchId", "Expiry", "Qty", "Location"};
        int[] widths = {4, 10, 20, 8, 10, 8, 10};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    truncate((String) row[0], widths[1]),
                    truncate((String) row[1], widths[2]),
                    String.valueOf(row[2]),
                    row[3] != null ? row[3].toString() : "No Expiry",
                    formatNumber((Integer) row[4]),
                    truncate((String) row[5], widths[6])
                }, widths);
            }

            int totalQty = rows.stream().mapToInt(row -> (Integer) row[4]).sum();

            printSeparator(widths);
            printRow(new String[]{
                "", "", "", "", "TOTALS",
                formatNumber(totalQty), ""
            }, widths);
        }

        printFooter(pageIndex, totalPages);
    }

    public static void printBillHeaderTable(List<Object[]> rows, int pageIndex, int totalPages) {
        String[] headers = {"#", "Serial", "Type", "Store", "DateTime", "Net", "Payment"};
        int[] widths = {4, 8, 8, 8, 16, 12, 12};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    String.valueOf(row[1]),
                    (String) row[2],
                    (String) row[3],
                    truncate(row[4].toString(), widths[4]),
                    formatMoney((BigDecimal) row[5]),
                    truncate((String) row[6], widths[6])
                }, widths);
            }
        }

        printFooter(pageIndex, totalPages);
        System.out.println("Commands: [n]ext [p]rev [s]ort [o <rowNo>] [q]uit");
    }

    public static void printBillLinesTable(List<Object[]> rows, BigDecimal billTotal) {
        String[] headers = {"#", "Code", "Name", "Qty", "Unit", "LineTotal"};
        int[] widths = {4, 10, 25, 6, 10, 12};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    truncate((String) row[0], widths[1]),
                    truncate((String) row[1], widths[2]),
                    formatNumber((Integer) row[2]),
                    formatMoney((BigDecimal) row[3]),
                    formatMoney((BigDecimal) row[4])
                }, widths);
            }

            int totalQty = rows.stream().mapToInt(row -> (Integer) row[2]).sum();
            BigDecimal totalAmount = rows.stream().map(row -> (BigDecimal) row[4]).reduce(BigDecimal.ZERO, BigDecimal::add);

            printSeparator(widths);
            printRow(new String[]{
                "", "", "TOTALS",
                formatNumber(totalQty),
                "",
                formatMoney(totalAmount)
            }, widths);
        }

        System.out.println("\nPress any key to return to bill list...");
    }

    public static void printOrderHeaderTable(List<Object[]> rows, int pageIndex, int totalPages) {
        String[] headers = {"#", "Serial", "Type", "Store", "DateTime", "Net", "Payment"};
        int[] widths = {4, 8, 8, 8, 16, 12, 12};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    String.valueOf(row[1]),
                    (String) row[2],
                    (String) row[3],
                    truncate(row[4].toString(), widths[4]),
                    formatMoney((BigDecimal) row[5]),
                    truncate((String) row[6], widths[6])
                }, widths);
            }
        }

        printFooter(pageIndex, totalPages);
        System.out.println("Commands: [n]ext [p]rev [s]ort [o <rowNo>] [q]uit");
    }

    public static void printOrderLineTable(List<Object[]> rows) {
        String[] headers = {"#", "Code", "Name", "Qty", "Unit", "LineTotal"};
        int[] widths = {4, 10, 25, 6, 10, 12};

        printTableHeader(headers, widths);

        if (rows.isEmpty()) {
            printEmptyMessage(widths);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                printRow(new String[]{
                    String.valueOf(i + 1),
                    truncate((String) row[0], widths[1]),
                    truncate((String) row[1], widths[2]),
                    formatNumber((Integer) row[3]),
                    formatMoney((BigDecimal) row[2]),
                    formatMoney((BigDecimal) row[4])
                }, widths);
            }

            int totalQty = rows.stream().mapToInt(row -> (Integer) row[3]).sum();
            BigDecimal totalAmount = rows.stream().map(row -> (BigDecimal) row[4]).reduce(BigDecimal.ZERO, BigDecimal::add);

            printSeparator(widths);
            printRow(new String[]{
                "", "", "TOTALS",
                formatNumber(totalQty),
                "",
                formatMoney(totalAmount)
            }, widths);
        }

        System.out.println("\nPress any key to return to order list...");
    }

    private static void printTableHeader(String[] headers, int[] widths) {
        printSeparator(widths);
        printRow(headers, widths);
        printSeparator(widths);
    }

    private static void printRow(String[] values, int[] widths) {
        System.out.print("|");
        for (int i = 0; i < values.length; i++) {
            String value = values[i] != null ? values[i] : "";
            if (i >= 3 && i < values.length - 1) { // Right-align numeric columns
                System.out.printf(" %" + (widths[i] - 1) + "s |", value);
            } else {
                System.out.printf(" %-" + (widths[i] - 1) + "s |", value);
            }
        }
        System.out.println();
    }

    private static void printSeparator(int[] widths) {
        System.out.print("+");
        for (int width : widths) {
            System.out.print("-".repeat(width) + "+");
        }
        System.out.println();
    }

    private static void printEmptyMessage(int[] widths) {
        int totalWidth = widths.length + 1; // +1 for separators
        for (int width : widths) {
            totalWidth += width;
        }
        String message = "No records for the selected filters.";
        System.out.printf("|%-" + (totalWidth - 2) + "s|%n", message);
        printSeparator(widths);
    }

    private static void printCustomEmptyMessage(String message, int[] widths) {
        int totalWidth = widths.length + 1;
        for (int width : widths) totalWidth += width;
        System.out.printf("|%-" + (totalWidth - 2) + "s|%n", message);
        printSeparator(widths);
    }

    private static void printFooter(int pageIndex, int totalPages) {
        System.out.println();
        System.out.printf("Page %d/%d   Commands: [n]ext [p]rev [s]ort [q]uit%n",
            pageIndex + 1, Math.max(1, totalPages));
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength - 1) return text;
        return text.substring(0, maxLength - 2) + "…";
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        return MONEY_FORMAT.format(amount).replace("$", "");
    }

    private static String formatNumber(int number) {
        return NUMBER_FORMAT.format(number);
    }
}
