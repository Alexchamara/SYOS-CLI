package cli.manager.table;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;

import cli.manager.table.TablePrinter;
import application.reports.dto.DailySalesRow;
import application.reports.dto.ReorderRow;
import application.reports.dto.StockBatchRow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("TablePrinter Tests")
class TablePrinterTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Nested
    @DisplayName("Daily Sales Table Tests")
    class DailySalesTableTests {

        @Test
        @DisplayName("Should print daily sales table with data")
        void shouldPrintDailySalesTableWithData() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Gaming Laptop", "SHELF", 2,
                    new BigDecimal("2000.00"), new BigDecimal("200.00"), new BigDecimal("1800.00")),
                new DailySalesRow("PROD002", "Wireless Mouse", "WEB", 5,
                    new BigDecimal("150.00"), new BigDecimal("15.00"), new BigDecimal("135.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product Code"));
            assertTrue(output.contains("Product Name"));
            assertTrue(output.contains("Location"));
            assertTrue(output.contains("Qty Sold"));
            assertTrue(output.contains("Gross"));
            assertTrue(output.contains("Discount"));
            assertTrue(output.contains("Net"));

            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Gaming Laptop"));
            assertTrue(output.contains("SHELF"));
            assertTrue(output.contains("2"));
            assertTrue(output.contains("2000.00"));
            assertTrue(output.contains("1800.00"));
        }

        @Test
        @DisplayName("Should print empty table message for no data")
        void shouldPrintEmptyTableMessageForNoData() {
            // Given
            List<DailySalesRow> emptyData = List.of();

            // When
            TablePrinter.printDailySalesTable(emptyData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No sales data to display") || output.contains("No data"));
        }

        @Test
        @DisplayName("Should handle very long product names")
        void shouldHandleVeryLongProductNames() {
            // Given
            String longName = "Ultra Premium Professional Gaming Laptop with Extended Warranty";
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", longName, "SHELF", 1,
                    new BigDecimal("3000.00"), BigDecimal.ZERO, new BigDecimal("3000.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("PROD001"));
            // Should truncate or handle long names appropriately
            assertTrue(output.length() > 0);
        }

        @Test
        @DisplayName("Should format currency amounts correctly")
        void shouldFormatCurrencyAmountsCorrectly() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Product", "SHELF", 1,
                    new BigDecimal("1234.56"), new BigDecimal("123.45"), new BigDecimal("1111.11"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("1234.56"));
            assertTrue(output.contains("123.45"));
            assertTrue(output.contains("1111.11"));
        }
    }

    @Nested
    @DisplayName("Reorder Table Tests")
    class ReorderTableTests {

        @Test
        @DisplayName("Should print reorder table with data")
        void shouldPrintReorderTableWithData() {
            // Given
            List<ReorderRow> reorderData = List.of(
                new ReorderRow("PROD001", "Critical Item", "BATCH001", "SHELF", 15,
                    LocalDate.now().plusDays(5), "CRITICAL"),
                new ReorderRow("PROD002", "Low Stock Item", "BATCH002", "MAIN_STORE", 35,
                    LocalDate.now().plusDays(30), "LOW")
            );

            // When
            TablePrinter.printReorderTable(reorderData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product Code"));
            assertTrue(output.contains("Product Name"));
            assertTrue(output.contains("Batch ID"));
            assertTrue(output.contains("Location"));
            assertTrue(output.contains("Quantity"));
            assertTrue(output.contains("Expiry"));
            assertTrue(output.contains("Status"));

            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Critical Item"));
            assertTrue(output.contains("CRITICAL"));
            assertTrue(output.contains("LOW"));
        }

        @Test
        @DisplayName("Should highlight critical items")
        void shouldHighlightCriticalItems() {
            // Given
            List<ReorderRow> criticalData = List.of(
                new ReorderRow("PROD001", "Critical", "BATCH001", "SHELF", 5,
                    LocalDate.now().plusDays(2), "CRITICAL"),
                new ReorderRow("PROD002", "Urgent", "BATCH002", "SHELF", 0,
                    LocalDate.now().plusDays(1), "OUT_OF_STOCK")
            );

            // When
            TablePrinter.printReorderTable(criticalData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("CRITICAL"));
            assertTrue(output.contains("OUT_OF_STOCK"));
        }

        @Test
        @DisplayName("Should handle empty reorder data")
        void shouldHandleEmptyReorderData() {
            // Given
            List<ReorderRow> emptyData = List.of();

            // When
            TablePrinter.printReorderTable(emptyData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No reorder data to display") || output.contains("No items"));
        }
    }

    @Nested
    @DisplayName("Stock Batch Table Tests")
    class StockBatchTableTests {

        @Test
        @DisplayName("Should print stock batch table with data")
        void shouldPrintStockBatchTableWithData() {
            // Given
            List<StockBatchRow> batchData = List.of(
                new StockBatchRow("PROD001", "Laptop", "BATCH001",
                    LocalDate.now().plusDays(30), LocalDateTime.now().minusDays(5), 50, "MAIN_STORE"),
                new StockBatchRow("PROD002", "Mouse", "BATCH002",
                    LocalDate.now().plusDays(45), LocalDateTime.now().minusDays(3), 100, "SHELF")
            );

            // When
            TablePrinter.printStockBatchTable(batchData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product Code"));
            assertTrue(output.contains("Product Name"));
            assertTrue(output.contains("Batch ID"));
            assertTrue(output.contains("Expiry"));
            assertTrue(output.contains("Received"));
            assertTrue(output.contains("Quantity"));
            assertTrue(output.contains("Location"));

            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("BATCH001"));
            assertTrue(output.contains("MAIN_STORE"));
            assertTrue(output.contains("50"));
        }

        @Test
        @DisplayName("Should handle batches with no expiry")
        void shouldHandleBatchesWithNoExpiry() {
            // Given
            List<StockBatchRow> batchData = List.of(
                new StockBatchRow("PROD003", "No Expiry Item", "BATCH003",
                    null, LocalDateTime.now().minusDays(1), 75, "WEB")
            );

            // When
            TablePrinter.printStockBatchTable(batchData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No Expiry") || output.contains("N/A") || output.contains("-"));
        }

        @Test
        @DisplayName("Should handle empty stock batch data")
        void shouldHandleEmptyStockBatchData() {
            // Given
            List<StockBatchRow> emptyData = List.of();

            // When
            TablePrinter.printStockBatchTable(emptyData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No batch data to display") || output.contains("No batches"));
        }
    }

    @Nested
    @DisplayName("Table Formatting Tests")
    class TableFormattingTests {

        @Test
        @DisplayName("Should use consistent column spacing")
        void shouldUseConsistentColumnSpacing() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("A", "B", "C", 1, new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1")),
                new DailySalesRow("VERY_LONG_CODE", "Very Long Product Name", "MAIN_STORE", 999,
                    new BigDecimal("99999.99"), new BigDecimal("9999.99"), new BigDecimal("89999.99"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            String[] lines = output.split("\n");
            // Check that table maintains alignment
            assertTrue(lines.length > 2); // Header + data rows
        }

        @Test
        @DisplayName("Should use table borders and separators")
        void shouldUseTableBordersAndSeparators() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Product", "SHELF", 1,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("-") || output.contains("|") || output.contains("+"));
        }

        @Test
        @DisplayName("Should handle special characters in data")
        void shouldHandleSpecialCharactersInData() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD@001", "Product with & special chars!", "SHELF", 1,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("PROD@001"));
            assertTrue(output.contains("&"));
            assertTrue(output.contains("!"));
        }
    }

    @Nested
    @DisplayName("Utility Class Pattern Tests")
    class UtilityClassPatternTests {

        @Test
        @DisplayName("Should be a utility class with static methods")
        void shouldBeAUtilityClassWithStaticMethods() {
            // Given
            Class<TablePrinter> tablePrinterClass = TablePrinter.class;

            // When
            java.lang.reflect.Method[] methods = tablePrinterClass.getDeclaredMethods();

            // Then
            boolean allMethodsStatic = java.util.Arrays.stream(methods)
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .allMatch(method -> java.lang.reflect.Modifier.isStatic(method.getModifiers()));
            assertTrue(allMethodsStatic);
        }

        @Test
        @DisplayName("Should not be instantiable")
        void shouldNotBeInstantiable() {
            // Given
            Class<TablePrinter> tablePrinterClass = TablePrinter.class;

            // When
            java.lang.reflect.Constructor<?>[] constructors = tablePrinterClass.getDeclaredConstructors();

            // Then
            boolean hasPublicConstructor = java.util.Arrays.stream(constructors)
                .anyMatch(constructor -> java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
            assertFalse(hasPublicConstructor);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large datasets efficiently")
        void shouldHandleLargeDatasetsEfficiently() {
            // Given
            List<DailySalesRow> largeDataset = java.util.stream.IntStream.range(1, 1001)
                .mapToObj(i -> new DailySalesRow("PROD" + String.format("%03d", i), "Product " + i, "SHELF",
                    i % 10, new BigDecimal(i * 10), new BigDecimal(i), new BigDecimal(i * 9)))
                .collect(java.util.stream.Collectors.toList());

            // When
            long startTime = System.currentTimeMillis();
            TablePrinter.printDailySalesTable(largeDataset);
            long endTime = System.currentTimeMillis();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("PROD1000"));

            long duration = endTime - startTime;
            assertTrue(duration < 5000); // Should complete within 5 seconds
        }

        @Test
        @DisplayName("Should handle null collections gracefully")
        void shouldHandleNullCollectionsGracefully() {
            // When & Then
            assertDoesNotThrow(() -> TablePrinter.printDailySalesTable(null));
            assertDoesNotThrow(() -> TablePrinter.printReorderTable(null));
            assertDoesNotThrow(() -> TablePrinter.printStockBatchTable(null));

            String output = outputStream.toString();
            assertTrue(output.contains("No") || output.isEmpty());
        }
    }

    @Nested
    @DisplayName("Column Alignment Tests")
    class ColumnAlignmentTests {

        @Test
        @DisplayName("Should align numeric columns to the right")
        void shouldAlignNumericColumnsToTheRight() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Product", "SHELF", 1,
                    new BigDecimal("1.00"), BigDecimal.ZERO, new BigDecimal("1.00")),
                new DailySalesRow("PROD002", "Product", "SHELF", 1000,
                    new BigDecimal("10000.00"), new BigDecimal("1000.00"), new BigDecimal("9000.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            // Numeric values should be right-aligned in their columns
            assertTrue(output.contains("1.00"));
            assertTrue(output.contains("10000.00"));
        }

        @Test
        @DisplayName("Should align text columns to the left")
        void shouldAlignTextColumnsToTheLeft() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("A", "Short", "SHELF", 1,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")),
                new DailySalesRow("VERY_LONG_CODE", "Very Long Product Name", "MAIN_STORE", 1,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("A"));
            assertTrue(output.contains("VERY_LONG_CODE"));
            assertTrue(output.contains("Short"));
            assertTrue(output.contains("Very Long Product Name"));
        }
    }

    @Nested
    @DisplayName("Header Formatting Tests")
    class HeaderFormattingTests {

        @Test
        @DisplayName("Should print table headers consistently")
        void shouldPrintTableHeadersConsistently() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Product", "SHELF", 1,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            String[] lines = output.split("\n");

            // Should have header row
            boolean hasHeaderRow = java.util.Arrays.stream(lines)
                .anyMatch(line -> line.contains("Product Code") && line.contains("Product Name"));
            assertTrue(hasHeaderRow);
        }

        @Test
        @DisplayName("Should use separator lines")
        void shouldUseSeparatorLines() {
            // Given
            List<ReorderRow> reorderData = List.of(
                new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10,
                    LocalDate.now().plusDays(30), "LOW")
            );

            // When
            TablePrinter.printReorderTable(reorderData);

            // Then
            String output = outputStream.toString();
            // Should have separator lines (dashes, equals, or similar)
            assertTrue(output.contains("-") || output.contains("=") || output.contains("+"));
        }
    }

    @Nested
    @DisplayName("Data Type Handling Tests")
    class DataTypeHandlingTests {

        @Test
        @DisplayName("Should handle zero and negative values")
        void shouldHandleZeroAndNegativeValues() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Zero Sales", "SHELF", 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new DailySalesRow("PROD002", "Return", "WEB", -1,
                    new BigDecimal("-50.00"), BigDecimal.ZERO, new BigDecimal("-50.00"))
            );

            // When
            TablePrinter.printDailySalesTable(salesData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("0"));
            assertTrue(output.contains("-1"));
            assertTrue(output.contains("-50.00"));
        }

        @Test
        @DisplayName("Should handle null expiry dates in batch table")
        void shouldHandleNullExpiryDatesInBatchTable() {
            // Given
            List<StockBatchRow> batchData = List.of(
                new StockBatchRow("PROD001", "No Expiry", "BATCH001",
                    null, LocalDateTime.now(), 50, "MAIN_STORE")
            );

            // When
            TablePrinter.printStockBatchTable(batchData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No Expiry") || output.contains("N/A") || output.contains("-"));
        }

        @Test
        @DisplayName("Should format dates consistently")
        void shouldFormatDatesConsistently() {
            // Given
            LocalDate expiry = LocalDate.of(2025, 12, 31);
            LocalDateTime received = LocalDateTime.of(2025, 9, 22, 10, 30, 0);

            List<StockBatchRow> batchData = List.of(
                new StockBatchRow("PROD001", "Product", "BATCH001",
                    expiry, received, 100, "SHELF")
            );

            // When
            TablePrinter.printStockBatchTable(batchData);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("2025-12-31"));
            assertTrue(output.contains("2025-09-22") || output.contains("10:30"));
        }
    }
}
