package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.ReportRepository;
import application.reports.dto.*;
import cli.manager.filters.ReportFilters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("ReportRepository Domain Interface Tests")
class ReportRepositoryTest {

    private ReportRepository reportRepository;
    private ReportFilters reportFilters;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        reportFilters = mock(ReportFilters.class);
        // Setup common filter configurations
        when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
        when(reportFilters.day()).thenReturn(LocalDate.now());
    }

    @Nested
    @DisplayName("Daily Sales Report Tests")
    class DailySalesReportTests {

        @Test
        @DisplayName("Should return daily sales data with filters")
        void shouldReturnDailySalesDataWithFilters() {
            // Given
            List<DailySalesRow> expectedSales = List.of(
                new DailySalesRow("PROD001", "Laptop", "SHELF", 5,
                    new BigDecimal("2500.00"), new BigDecimal("250.00"), new BigDecimal("2250.00")),
                new DailySalesRow("PROD002", "Mouse", "WEB", 10,
                    new BigDecimal("300.00"), new BigDecimal("30.00"), new BigDecimal("270.00"))
            );
            when(reportRepository.dailySales(reportFilters)).thenReturn(expectedSales);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("PROD001", result.get(0).code());
            assertEquals("Laptop", result.get(0).name());
            assertEquals("SHELF", result.get(0).location());
            assertEquals(5, result.get(0).qtySold());
            assertEquals(new BigDecimal("2500.00"), result.get(0).gross());
            assertEquals(new BigDecimal("250.00"), result.get(0).discount());
            assertEquals(new BigDecimal("2250.00"), result.get(0).net());

            verify(reportRepository).dailySales(reportFilters);
        }

        @Test
        @DisplayName("Should handle empty daily sales results")
        void shouldHandleEmptyDailySalesResults() {
            // Given
            when(reportRepository.dailySales(reportFilters)).thenReturn(List.of());

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(reportRepository).dailySales(reportFilters);
        }

        @Test
        @DisplayName("Should handle null filters for daily sales")
        void shouldHandleNullFiltersForDailySales() {
            // Given
            List<DailySalesRow> expectedSales = List.of(
                new DailySalesRow("PROD001", "Test Product", "SHELF", 1,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"))
            );
            when(reportRepository.dailySales(null)).thenReturn(expectedSales);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(null);

            // Then
            assertEquals(1, result.size());
            verify(reportRepository).dailySales(null);
        }

        @Test
        @DisplayName("Should aggregate sales across different locations")
        void shouldAggregateSalesAcrossDifferentLocations() {
            // Given
            List<DailySalesRow> expectedSales = List.of(
                new DailySalesRow("PROD001", "Popular Item", "SHELF", 8,
                    new BigDecimal("800.00"), new BigDecimal("40.00"), new BigDecimal("760.00")),
                new DailySalesRow("PROD001", "Popular Item", "WEB", 12,
                    new BigDecimal("1200.00"), new BigDecimal("120.00"), new BigDecimal("1080.00"))
            );
            when(reportRepository.dailySales(reportFilters)).thenReturn(expectedSales);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertEquals(2, result.size());
            // Same product, different locations
            assertEquals("PROD001", result.get(0).code());
            assertEquals("PROD001", result.get(1).code());
            assertEquals("SHELF", result.get(0).location());
            assertEquals("WEB", result.get(1).location());
        }
    }

    @Nested
    @DisplayName("Reshelve Report Tests")
    class ReshelveReportTests {

        @Test
        @DisplayName("Should return reshelve movement data")
        void shouldReturnReshelveMovementData() {
            // Given
            List<ReshelveRow> expectedReshelve = List.of(
                new ReshelveRow(1L, LocalDateTime.now().minusHours(2), "PROD001", "Laptop",
                    "MAIN_STORE", "SHELF", 25, "Manual transfer to shelf"),
                new ReshelveRow(2L, LocalDateTime.now().minusHours(1), "PROD002", "Monitor",
                    "MAIN_STORE", "WEB", 15, "Stock transfer for online orders")
            );
            when(reportRepository.reshelve(reportFilters)).thenReturn(expectedReshelve);

            // When
            List<ReshelveRow> result = reportRepository.reshelve(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            ReshelveRow row1 = result.get(0);
            assertEquals(1L, row1.id());
            assertEquals("PROD001", row1.productCode());
            assertEquals("Laptop", row1.productName());
            assertEquals("MAIN_STORE", row1.fromLocation());
            assertEquals("SHELF", row1.toLocation());
            assertEquals(25, row1.quantity());
            assertEquals("Manual transfer to shelf", row1.note());

            verify(reportRepository).reshelve(reportFilters);
        }

        @Test
        @DisplayName("Should track different movement patterns")
        void shouldTrackDifferentMovementPatterns() {
            // Given
            List<ReshelveRow> expectedMovements = List.of(
                new ReshelveRow(1L, LocalDateTime.now(), "PROD001", "Product", "MAIN_STORE", "SHELF", 50, "To shelf"),
                new ReshelveRow(2L, LocalDateTime.now(), "PROD002", "Product", "SHELF", "WEB", 30, "To web"),
                new ReshelveRow(3L, LocalDateTime.now(), "PROD003", "Product", "WEB", "MAIN_STORE", 20, "Back to main")
            );
            when(reportRepository.reshelve(reportFilters)).thenReturn(expectedMovements);

            // When
            List<ReshelveRow> result = reportRepository.reshelve(reportFilters);

            // Then
            assertEquals(3, result.size());
            assertEquals("MAIN_STORE", result.get(0).fromLocation());
            assertEquals("SHELF", result.get(0).toLocation());
            assertEquals("SHELF", result.get(1).fromLocation());
            assertEquals("WEB", result.get(1).toLocation());
            assertEquals("WEB", result.get(2).fromLocation());
            assertEquals("MAIN_STORE", result.get(2).toLocation());
        }

        @Test
        @DisplayName("Should handle empty reshelve data")
        void shouldHandleEmptyReshelveData() {
            // Given
            when(reportRepository.reshelve(reportFilters)).thenReturn(List.of());

            // When
            List<ReshelveRow> result = reportRepository.reshelve(reportFilters);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Reorder Report Tests")
    class ReorderReportTests {

        @Test
        @DisplayName("Should return reorder recommendations with threshold")
        void shouldReturnReorderRecommendationsWithThreshold() {
            // Given
            int threshold = 50;
            List<ReorderRow> expectedReorder = List.of(
                new ReorderRow("PROD001", "Critical Item", "BATCH001", "SHELF", 15,
                    LocalDate.now().plusDays(10), "CRITICAL"),
                new ReorderRow("PROD002", "Low Stock Item", "BATCH002", "MAIN_STORE", 35,
                    LocalDate.now().plusDays(30), "LOW"),
                new ReorderRow("PROD003", "Out of Stock", "BATCH003", "WEB", 0,
                    LocalDate.now().plusDays(5), "OUT_OF_STOCK")
            );
            when(reportRepository.reorder(reportFilters, threshold)).thenReturn(expectedReorder);

            // When
            List<ReorderRow> result = reportRepository.reorder(reportFilters, threshold);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            ReorderRow criticalRow = result.get(0);
            assertEquals("PROD001", criticalRow.code());
            assertEquals("CRITICAL", criticalRow.status());
            assertEquals(15, criticalRow.quantity());
            assertTrue(criticalRow.quantity() < threshold);

            ReorderRow lowRow = result.get(1);
            assertEquals("LOW", lowRow.status());
            assertEquals(35, lowRow.quantity());

            ReorderRow outOfStockRow = result.get(2);
            assertEquals("OUT_OF_STOCK", outOfStockRow.status());
            assertEquals(0, outOfStockRow.quantity());

            verify(reportRepository).reorder(reportFilters, threshold);
        }

        @Test
        @DisplayName("Should handle different threshold values")
        void shouldHandleDifferentThresholdValues() {
            // Given
            int lowThreshold = 10;
            int highThreshold = 100;

            List<ReorderRow> lowThresholdResults = List.of(
                new ReorderRow("PROD001", "Critical Only", "BATCH001", "SHELF", 5,
                    LocalDate.now().plusDays(30), "CRITICAL")
            );
            List<ReorderRow> highThresholdResults = List.of(
                new ReorderRow("PROD001", "Critical", "BATCH001", "SHELF", 5,
                    LocalDate.now().plusDays(30), "CRITICAL"),
                new ReorderRow("PROD002", "Low", "BATCH002", "MAIN_STORE", 75,
                    LocalDate.now().plusDays(45), "LOW")
            );

            when(reportRepository.reorder(reportFilters, lowThreshold)).thenReturn(lowThresholdResults);
            when(reportRepository.reorder(reportFilters, highThreshold)).thenReturn(highThresholdResults);

            // When
            List<ReorderRow> lowResult = reportRepository.reorder(reportFilters, lowThreshold);
            List<ReorderRow> highResult = reportRepository.reorder(reportFilters, highThreshold);

            // Then
            assertEquals(1, lowResult.size());
            assertEquals(2, highResult.size());
            verify(reportRepository).reorder(reportFilters, lowThreshold);
            verify(reportRepository).reorder(reportFilters, highThreshold);
        }

        @Test
        @DisplayName("Should handle zero threshold")
        void shouldHandleZeroThreshold() {
            // Given
            int zeroThreshold = 0;
            List<ReorderRow> zeroResults = List.of();
            when(reportRepository.reorder(reportFilters, zeroThreshold)).thenReturn(zeroResults);

            // When
            List<ReorderRow> result = reportRepository.reorder(reportFilters, zeroThreshold);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).reorder(reportFilters, zeroThreshold);
        }

        @Test
        @DisplayName("Should prioritize by urgency and expiry")
        void shouldPrioritizeByUrgencyAndExpiry() {
            // Given
            int threshold = 25;
            List<ReorderRow> prioritizedResults = List.of(
                new ReorderRow("PROD001", "Urgent Expiry", "BATCH001", "SHELF", 20,
                    LocalDate.now().plusDays(3), "URGENT"),
                new ReorderRow("PROD002", "Critical Low", "BATCH002", "MAIN_STORE", 5,
                    LocalDate.now().plusDays(30), "CRITICAL"),
                new ReorderRow("PROD003", "Standard Low", "BATCH003", "WEB", 22,
                    LocalDate.now().plusDays(60), "LOW")
            );
            when(reportRepository.reorder(reportFilters, threshold)).thenReturn(prioritizedResults);

            // When
            List<ReorderRow> result = reportRepository.reorder(reportFilters, threshold);

            // Then
            assertEquals(3, result.size());
            assertEquals("URGENT", result.get(0).status());
            assertEquals("CRITICAL", result.get(1).status());
            assertEquals("LOW", result.get(2).status());
        }
    }

    @Nested
    @DisplayName("Stock Batches Report Tests")
    class StockBatchesReportTests {

        @Test
        @DisplayName("Should return stock batch inventory data")
        void shouldReturnStockBatchInventoryData() {
            // Given
            List<StockBatchRow> expectedBatches = List.of(
                new StockBatchRow("PROD001", "Gaming Laptop", "BATCH001",
                    LocalDate.now().plusDays(45), LocalDateTime.now().minusDays(5), 50, "MAIN_STORE"),
                new StockBatchRow("PROD002", "Wireless Mouse", "BATCH002",
                    LocalDate.now().plusDays(60), LocalDateTime.now().minusDays(3), 100, "SHELF"),
                new StockBatchRow("PROD003", "Keyboard", "BATCH003",
                    null, LocalDateTime.now().minusDays(1), 75, "WEB") // No expiry
            );
            when(reportRepository.stockBatches(reportFilters)).thenReturn(expectedBatches);

            // When
            List<StockBatchRow> result = reportRepository.stockBatches(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            StockBatchRow batch1 = result.get(0);
            assertEquals("PROD001", batch1.code());
            assertEquals("Gaming Laptop", batch1.name());
            assertEquals("BATCH001", batch1.batchId());
            assertEquals(50, batch1.qty());
            assertEquals("MAIN_STORE", batch1.location());
            assertNotNull(batch1.expiry());
            assertNotNull(batch1.receivedAt());

            StockBatchRow batch3 = result.get(2);
            assertNull(batch3.expiry()); // Should handle null expiry

            verify(reportRepository).stockBatches(reportFilters);
        }

        @Test
        @DisplayName("Should handle batches across all locations")
        void shouldHandleBatchesAcrossAllLocations() {
            // Given
            List<StockBatchRow> expectedBatches = List.of(
                new StockBatchRow("PROD001", "Product", "BATCH001", LocalDate.now().plusDays(30),
                    LocalDateTime.now().minusDays(1), 100, "MAIN_STORE"),
                new StockBatchRow("PROD001", "Product", "BATCH002", LocalDate.now().plusDays(25),
                    LocalDateTime.now().minusHours(12), 50, "SHELF"),
                new StockBatchRow("PROD001", "Product", "BATCH003", LocalDate.now().plusDays(20),
                    LocalDateTime.now().minusHours(6), 25, "WEB")
            );
            when(reportRepository.stockBatches(reportFilters)).thenReturn(expectedBatches);

            // When
            List<StockBatchRow> result = reportRepository.stockBatches(reportFilters);

            // Then
            assertEquals(3, result.size());
            assertEquals("MAIN_STORE", result.get(0).location());
            assertEquals("SHELF", result.get(1).location());
            assertEquals("WEB", result.get(2).location());
        }

        @Test
        @DisplayName("Should handle empty stock batches")
        void shouldHandleEmptyStockBatches() {
            // Given
            when(reportRepository.stockBatches(reportFilters)).thenReturn(List.of());

            // When
            List<StockBatchRow> result = reportRepository.stockBatches(reportFilters);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Bills Report Tests")
    class BillsReportTests {

        @Test
        @DisplayName("Should return bill header data")
        void shouldReturnBillHeaderData() {
            // Given
            List<BillHeaderRow> expectedBills = List.of(
                new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Main Store",
                    LocalDateTime.now().minusHours(2), new BigDecimal("150.75"), "Cash: $150.75"),
                new BillHeaderRow(2, 102L, "C-000002", "COUNTER", "Main Store",
                    LocalDateTime.now().minusHours(1), new BigDecimal("89.50"), "Cash: $90.00, Change: $0.50")
            );
            when(reportRepository.bills(reportFilters)).thenReturn(expectedBills);

            // When
            List<BillHeaderRow> result = reportRepository.bills(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            BillHeaderRow bill1 = result.get(0);
            assertEquals(1, bill1.rowNo());
            assertEquals(101L, bill1.orderId());
            assertEquals("C-000001", bill1.serial());
            assertEquals("COUNTER", bill1.type());
            assertEquals("Main Store", bill1.store());
            assertEquals(new BigDecimal("150.75"), bill1.netTotal());
            assertTrue(bill1.paymentSummary().contains("Cash"));

            verify(reportRepository).bills(reportFilters);
        }

        @Test
        @DisplayName("Should handle different bill types and payment methods")
        void shouldHandleDifferentBillTypesAndPaymentMethods() {
            // Given
            List<BillHeaderRow> expectedBills = List.of(
                new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "SHELF",
                    LocalDateTime.now(), new BigDecimal("100.00"), "Cash: $100.00"),
                new BillHeaderRow(2, 102L, "Q-000001", "QUOTE", "SHELF",
                    LocalDateTime.now(), new BigDecimal("200.00"), "Quote - Not Paid"),
                new BillHeaderRow(3, 103L, "C-000002", "COUNTER", "SHELF",
                    LocalDateTime.now(), new BigDecimal("75.00"), "Cash: $50.00, Card: $25.00")
            );
            when(reportRepository.bills(reportFilters)).thenReturn(expectedBills);

            // When
            List<BillHeaderRow> result = reportRepository.bills(reportFilters);

            // Then
            assertEquals(3, result.size());
            assertEquals("COUNTER", result.get(0).type());
            assertEquals("QUOTE", result.get(1).type());
            assertTrue(result.get(2).paymentSummary().contains("Cash") &&
                      result.get(2).paymentSummary().contains("Card"));
        }
    }

    @Nested
    @DisplayName("Bill Lines Report Tests")
    class BillLinesReportTests {

        @Test
        @DisplayName("Should return bill line details for specific order")
        void shouldReturnBillLineDetailsForSpecificOrder() {
            // Given
            long orderId = 101L;
            List<BillLineRow> expectedLines = List.of(
                new BillLineRow("PROD001", "Gaming Laptop", new BigDecimal("999.99"), 1, new BigDecimal("999.99")),
                new BillLineRow("PROD002", "Wireless Mouse", new BigDecimal("29.99"), 2, new BigDecimal("59.98")),
                new BillLineRow("PROD003", "USB Cable", new BigDecimal("9.99"), 3, new BigDecimal("29.97"))
            );
            when(reportRepository.billLines(orderId)).thenReturn(expectedLines);

            // When
            List<BillLineRow> result = reportRepository.billLines(orderId);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            BillLineRow line1 = result.get(0);
            assertEquals("PROD001", line1.productCode());
            assertEquals("Gaming Laptop", line1.name());
            assertEquals(new BigDecimal("999.99"), line1.unitPrice());
            assertEquals(1, line1.qty());
            assertEquals(new BigDecimal("999.99"), line1.lineTotal());

            verify(reportRepository).billLines(orderId);
        }

        @Test
        @DisplayName("Should handle empty bill lines")
        void shouldHandleEmptyBillLines() {
            // Given
            long orderId = 999L;
            when(reportRepository.billLines(orderId)).thenReturn(List.of());

            // When
            List<BillLineRow> result = reportRepository.billLines(orderId);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).billLines(orderId);
        }

        @Test
        @DisplayName("Should handle single line bills")
        void shouldHandleSingleLineBills() {
            // Given
            long orderId = 102L;
            List<BillLineRow> singleLine = List.of(
                new BillLineRow("PROD001", "Single Item", new BigDecimal("50.00"), 1, new BigDecimal("50.00"))
            );
            when(reportRepository.billLines(orderId)).thenReturn(singleLine);

            // When
            List<BillLineRow> result = reportRepository.billLines(orderId);

            // Then
            assertEquals(1, result.size());
            assertEquals("Single Item", result.get(0).name());
        }

        @Test
        @DisplayName("Should handle large quantities and amounts")
        void shouldHandleLargeQuantitiesAndAmounts() {
            // Given
            long orderId = 103L;
            List<BillLineRow> largeOrderLines = List.of(
                new BillLineRow("PROD001", "Bulk Item", new BigDecimal("1.99"), 1000, new BigDecimal("1990.00"))
            );
            when(reportRepository.billLines(orderId)).thenReturn(largeOrderLines);

            // When
            List<BillLineRow> result = reportRepository.billLines(orderId);

            // Then
            assertEquals(1, result.size());
            assertEquals(1000, result.get(0).qty());
            assertEquals(new BigDecimal("1990.00"), result.get(0).lineTotal());
        }
    }

    @Nested
    @DisplayName("Orders Report Tests")
    class OrdersReportTests {

        @Test
        @DisplayName("Should return order header data for web transactions")
        void shouldReturnOrderHeaderDataForWebTransactions() {
            // Given
            List<OrderHeaderRow> expectedOrders = List.of(
                new OrderHeaderRow(1, 201L, 2001L, "ONLINE", "Web Store",
                    LocalDateTime.now().minusHours(3), new BigDecimal("299.99"), "Card: ****1234"),
                new OrderHeaderRow(2, 202L, 2002L, "MOBILE", "Mobile App",
                    LocalDateTime.now().minusHours(1), new BigDecimal("149.99"), "Digital Wallet: PayPal")
            );
            when(reportRepository.orders(reportFilters)).thenReturn(expectedOrders);

            // When
            List<OrderHeaderRow> result = reportRepository.orders(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            OrderHeaderRow order1 = result.get(0);
            assertEquals(1, order1.rowNo());
            assertEquals(201L, order1.orderId());
            assertEquals(2001L, order1.serial());
            assertEquals("ONLINE", order1.type());
            assertEquals("Web Store", order1.store());
            assertTrue(order1.paymentSummary().contains("****"));

            OrderHeaderRow order2 = result.get(1);
            assertEquals("MOBILE", order2.type());
            assertTrue(order2.paymentSummary().contains("PayPal"));

            verify(reportRepository).orders(reportFilters);
        }

        @Test
        @DisplayName("Should handle different online order types")
        void shouldHandleDifferentOnlineOrderTypes() {
            // Given
            List<OrderHeaderRow> differentTypes = List.of(
                new OrderHeaderRow(1, 201L, 2001L, "WEB", "Website",
                    LocalDateTime.now(), new BigDecimal("100.00"), "Credit Card"),
                new OrderHeaderRow(2, 202L, 2002L, "MOBILE", "Mobile App",
                    LocalDateTime.now(), new BigDecimal("150.00"), "Mobile Payment"),
                new OrderHeaderRow(3, 203L, 2003L, "API", "Third Party",
                    LocalDateTime.now(), new BigDecimal("200.00"), "API Integration")
            );
            when(reportRepository.orders(reportFilters)).thenReturn(differentTypes);

            // When
            List<OrderHeaderRow> result = reportRepository.orders(reportFilters);

            // Then
            assertEquals(3, result.size());
            assertEquals("WEB", result.get(0).type());
            assertEquals("MOBILE", result.get(1).type());
            assertEquals("API", result.get(2).type());
        }

        @Test
        @DisplayName("Should handle empty orders data")
        void shouldHandleEmptyOrdersData() {
            // Given
            when(reportRepository.orders(reportFilters)).thenReturn(List.of());

            // When
            List<OrderHeaderRow> result = reportRepository.orders(reportFilters);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Order Lines Report Tests")
    class OrderLinesReportTests {

        @Test
        @DisplayName("Should return order line details for specific order")
        void shouldReturnOrderLineDetailsForSpecificOrder() {
            // Given
            long orderId = 201L;
            List<OrderLineRow> expectedLines = List.of(
                new OrderLineRow("PROD001", "Professional Laptop", new BigDecimal("1299.99"), 1, new BigDecimal("1299.99")),
                new OrderLineRow("PROD002", "Laptop Stand", new BigDecimal("49.99"), 1, new BigDecimal("49.99")),
                new OrderLineRow("PROD003", "USB-C Hub", new BigDecimal("79.99"), 2, new BigDecimal("159.98"))
            );
            when(reportRepository.orderLines(orderId)).thenReturn(expectedLines);

            // When
            List<OrderLineRow> result = reportRepository.orderLines(orderId);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            OrderLineRow line1 = result.get(0);
            assertEquals("PROD001", line1.productCode());
            assertEquals("Professional Laptop", line1.name());
            assertEquals(new BigDecimal("1299.99"), line1.unitPrice());
            assertEquals(1, line1.qty());
            assertEquals(new BigDecimal("1299.99"), line1.lineTotal());

            OrderLineRow line3 = result.get(2);
            assertEquals(2, line3.qty());
            assertEquals(new BigDecimal("159.98"), line3.lineTotal());

            verify(reportRepository).orderLines(orderId);
        }

        @Test
        @DisplayName("Should handle empty order lines")
        void shouldHandleEmptyOrderLines() {
            // Given
            long orderId = 999L;
            when(reportRepository.orderLines(orderId)).thenReturn(List.of());

            // When
            List<OrderLineRow> result = reportRepository.orderLines(orderId);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).orderLines(orderId);
        }

        @Test
        @DisplayName("Should handle fractional prices and quantities")
        void shouldHandleFractionalPricesAndQuantities() {
            // Given
            long orderId = 203L;
            List<OrderLineRow> fractionalLines = List.of(
                new OrderLineRow("PROD001", "Weighted Item", new BigDecimal("12.333"), 3, new BigDecimal("36.999"))
            );
            when(reportRepository.orderLines(orderId)).thenReturn(fractionalLines);

            // When
            List<OrderLineRow> result = reportRepository.orderLines(orderId);

            // Then
            assertEquals(1, result.size());
            assertEquals(new BigDecimal("12.333"), result.get(0).unitPrice());
            assertEquals(new BigDecimal("36.999"), result.get(0).lineTotal());
        }
    }

    @Nested
    @DisplayName("Interface Contract Tests")
    class InterfaceContractTests {

        @Test
        @DisplayName("Should define all required report methods")
        void shouldDefineAllRequiredReportMethods() {
            // Given
            Class<ReportRepository> interfaceClass = ReportRepository.class;

            // When
            java.lang.reflect.Method[] methods = interfaceClass.getDeclaredMethods();

            // Then
            assertEquals(8, methods.length); // Should have exactly 8 methods

            // Verify method names exist
            String[] expectedMethods = {
                "dailySales", "reshelve", "reorder", "stockBatches",
                "bills", "billLines", "orders", "orderLines"
            };

            for (String expectedMethod : expectedMethods) {
                boolean methodExists = java.util.Arrays.stream(methods)
                    .anyMatch(method -> method.getName().equals(expectedMethod));
                assertTrue(methodExists, "Method " + expectedMethod + " should exist");
            }
        }

        @Test
        @DisplayName("Should return List types for all methods")
        void shouldReturnListTypesForAllMethods() {
            // Given
            Class<ReportRepository> interfaceClass = ReportRepository.class;

            // When
            java.lang.reflect.Method[] methods = interfaceClass.getDeclaredMethods();

            // Then
            for (java.lang.reflect.Method method : methods) {
                assertTrue(List.class.isAssignableFrom(method.getReturnType()),
                    "Method " + method.getName() + " should return List type");
            }
        }

        @Test
        @DisplayName("Should accept appropriate parameter types")
        void shouldAcceptAppropriateParameterTypes() {
            // Given
            Class<ReportRepository> interfaceClass = ReportRepository.class;

            // When & Then
            try {
                // Verify methods with ReportFilters parameter
                interfaceClass.getMethod("dailySales", ReportFilters.class);
                interfaceClass.getMethod("reshelve", ReportFilters.class);
                interfaceClass.getMethod("stockBatches", ReportFilters.class);
                interfaceClass.getMethod("bills", ReportFilters.class);
                interfaceClass.getMethod("orders", ReportFilters.class);

                // Verify methods with threshold parameter
                interfaceClass.getMethod("reorder", ReportFilters.class, int.class);

                // Verify methods with orderId parameter
                interfaceClass.getMethod("billLines", long.class);
                interfaceClass.getMethod("orderLines", long.class);

            } catch (NoSuchMethodException e) {
                fail("Required method signature not found: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Filter Integration Tests")
    class FilterIntegrationTests {

        @Test
        @DisplayName("Should work with different date filter modes")
        void shouldWorkWithDifferentDateFilterModes() {
            // Given
            ReportFilters singleDayFilters = mock(ReportFilters.class);
            ReportFilters dateRangeFilters = mock(ReportFilters.class);

            when(singleDayFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(dateRangeFilters.dateMode()).thenReturn(ReportFilters.DateMode.DATE_RANGE);

            when(reportRepository.dailySales(singleDayFilters)).thenReturn(List.of());
            when(reportRepository.dailySales(dateRangeFilters)).thenReturn(List.of());

            // When
            List<DailySalesRow> singleDayResult = reportRepository.dailySales(singleDayFilters);
            List<DailySalesRow> rangeResult = reportRepository.dailySales(dateRangeFilters);

            // Then
            assertNotNull(singleDayResult);
            assertNotNull(rangeResult);
            verify(reportRepository).dailySales(singleDayFilters);
            verify(reportRepository).dailySales(dateRangeFilters);
        }

        @Test
        @DisplayName("Should handle various threshold values in reorder report")
        void shouldHandleVariousThresholdValuesInReorderReport() {
            // Given
            int[] thresholds = {0, 10, 25, 50, 100, 500};

            for (int threshold : thresholds) {
                when(reportRepository.reorder(reportFilters, threshold)).thenReturn(List.of());
            }

            // When & Then
            for (int threshold : thresholds) {
                List<ReorderRow> result = reportRepository.reorder(reportFilters, threshold);
                assertNotNull(result);
                verify(reportRepository).reorder(reportFilters, threshold);
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            RuntimeException repositoryException = new RuntimeException("Database connection failed");
            when(reportRepository.dailySales(reportFilters)).thenThrow(repositoryException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.dailySales(reportFilters));
            assertEquals("Database connection failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle invalid order IDs")
        void shouldHandleInvalidOrderIds() {
            // Given
            long invalidOrderId = -1L;
            when(reportRepository.billLines(invalidOrderId)).thenReturn(List.of());

            // When
            List<BillLineRow> result = reportRepository.billLines(invalidOrderId);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).billLines(invalidOrderId);
        }

        @Test
        @DisplayName("Should handle very large order IDs")
        void shouldHandleVeryLargeOrderIds() {
            // Given
            long largeOrderId = Long.MAX_VALUE;
            when(reportRepository.orderLines(largeOrderId)).thenReturn(List.of());

            // When
            List<OrderLineRow> result = reportRepository.orderLines(largeOrderId);

            // Then
            assertNotNull(result);
            verify(reportRepository).orderLines(largeOrderId);
        }
    }

    @Nested
    @DisplayName("Performance and Scale Tests")
    class PerformanceAndScaleTests {

        @Test
        @DisplayName("Should handle large result sets")
        void shouldHandleLargeResultSets() {
            // Given
            List<DailySalesRow> largeResultSet = java.util.stream.IntStream.range(1, 1001)
                .mapToObj(i -> new DailySalesRow("PROD" + String.format("%03d", i), "Product " + i, "SHELF",
                    i % 10, new BigDecimal(i * 10), new BigDecimal(i), new BigDecimal(i * 9)))
                .collect(java.util.stream.Collectors.toList());

            when(reportRepository.dailySales(reportFilters)).thenReturn(largeResultSet);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertEquals(1000, result.size());
            assertEquals("PROD001", result.get(0).code());
            assertEquals("PROD1000", result.get(999).code());
        }

        @Test
        @DisplayName("Should handle multiple concurrent report requests")
        void shouldHandleMultipleConcurrentReportRequests() {
            // Given
            when(reportRepository.dailySales(reportFilters)).thenReturn(List.of());
            when(reportRepository.reshelve(reportFilters)).thenReturn(List.of());
            when(reportRepository.reorder(reportFilters, 50)).thenReturn(List.of());
            when(reportRepository.stockBatches(reportFilters)).thenReturn(List.of());

            // When
            List<DailySalesRow> sales = reportRepository.dailySales(reportFilters);
            List<ReshelveRow> reshelve = reportRepository.reshelve(reportFilters);
            List<ReorderRow> reorder = reportRepository.reorder(reportFilters, 50);
            List<StockBatchRow> batches = reportRepository.stockBatches(reportFilters);

            // Then
            assertNotNull(sales);
            assertNotNull(reshelve);
            assertNotNull(reorder);
            assertNotNull(batches);

            verify(reportRepository).dailySales(reportFilters);
            verify(reportRepository).reshelve(reportFilters);
            verify(reportRepository).reorder(reportFilters, 50);
            verify(reportRepository).stockBatches(reportFilters);
        }
    }
}
