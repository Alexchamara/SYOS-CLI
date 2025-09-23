package application.reports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.reports.ReportService;
import application.reports.dto.*;
import cli.manager.filters.ReportFilters;
import domain.repository.ReportRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("ReportService Tests")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportFilters reportFilters;

    private ReportService reportService;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        reportService = new ReportService(reportRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Daily Sales Report Tests")
    class DailySalesReportTests {

        @Test
        @DisplayName("Should return daily sales data")
        void shouldReturnDailySalesData() {
            // Given
            List<DailySalesRow> expectedSales = List.of(
                new DailySalesRow("PROD001", "Laptop", "SHELF", 5,
                    new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("450.00")),
                new DailySalesRow("PROD002", "Mouse", "WEB", 10,
                    new BigDecimal("200.00"), new BigDecimal("10.00"), new BigDecimal("190.00"))
            );

            when(reportRepository.dailySales(reportFilters)).thenReturn(expectedSales);

            // When
            List<DailySalesRow> result = reportService.dailySales(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedSales, result);
            verify(reportRepository).dailySales(reportFilters);
        }

        @Test
        @DisplayName("Should handle empty daily sales data")
        void shouldHandleEmptyDailySalesData() {
            // Given
            when(reportRepository.dailySales(reportFilters)).thenReturn(List.of());

            // When
            List<DailySalesRow> result = reportService.dailySales(reportFilters);

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
            List<DailySalesRow> result = reportService.dailySales(null);

            // Then
            assertEquals(1, result.size());
            verify(reportRepository).dailySales(null);
        }
    }

    @Nested
    @DisplayName("Reshelve Report Tests")
    class ReshelveReportTests {

        @Test
        @DisplayName("Should return reshelve data")
        void shouldReturnReshelveData() {
            // Given
            List<ReshelveRow> expectedReshelve = List.of(
                new ReshelveRow(1L, LocalDateTime.now(), "PROD001", "Laptop", "MAIN_STORE", "SHELF", 25, ""),
                new ReshelveRow(2L, LocalDateTime.now().minusHours(1), "PROD002", "Monitor", "MAIN_STORE", "SHELF", 15, "")
            );

            when(reportRepository.reshelve(reportFilters)).thenReturn(expectedReshelve);

            // When
            List<ReshelveRow> result = reportService.reshelve(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedReshelve, result);
            verify(reportRepository).reshelve(reportFilters);
        }

        @Test
        @DisplayName("Should handle empty reshelve data")
        void shouldHandleEmptyReshelveData() {
            // Given
            when(reportRepository.reshelve(reportFilters)).thenReturn(List.of());

            // When
            List<ReshelveRow> result = reportService.reshelve(reportFilters);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(reportRepository).reshelve(reportFilters);
        }
    }

    @Nested
    @DisplayName("Reorder Report Tests")
    class ReorderReportTests {

        @Test
        @DisplayName("Should return reorder data with threshold")
        void shouldReturnReorderDataWithThreshold() {
            // Given
            int threshold = 20;
            List<ReorderRow> expectedReorder = List.of(
                new ReorderRow("PROD001", "Laptop", "BATCH001", "MAIN_STORE", 15, LocalDate.now().plusDays(30), "LOW"),
                new ReorderRow("PROD002", "Mouse", "BATCH002", "SHELF", 5, LocalDate.now().plusDays(45), "CRITICAL")
            );

            when(reportRepository.reorder(reportFilters, threshold)).thenReturn(expectedReorder);

            // When
            List<ReorderRow> result = reportService.reorder(reportFilters, threshold);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedReorder, result);
            verify(reportRepository).reorder(reportFilters, threshold);
        }

        @Test
        @DisplayName("Should handle different threshold values")
        void shouldHandleDifferentThresholdValues() {
            // Given
            int lowThreshold = 10;
            int highThreshold = 100;
            List<ReorderRow> lowThresholdResults = List.of(
                new ReorderRow("PROD001", "Product 1", "BATCH001", "SHELF", 5, LocalDate.now().plusDays(30), "CRITICAL")
            );
            List<ReorderRow> highThresholdResults = List.of(
                new ReorderRow("PROD001", "Product 1", "BATCH001", "SHELF", 5, LocalDate.now().plusDays(30), "CRITICAL"),
                new ReorderRow("PROD002", "Product 2", "BATCH002", "MAIN_STORE", 75, LocalDate.now().plusDays(45), "LOW")
            );

            when(reportRepository.reorder(reportFilters, lowThreshold)).thenReturn(lowThresholdResults);
            when(reportRepository.reorder(reportFilters, highThreshold)).thenReturn(highThresholdResults);

            // When
            List<ReorderRow> lowResult = reportService.reorder(reportFilters, lowThreshold);
            List<ReorderRow> highResult = reportService.reorder(reportFilters, highThreshold);

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
            when(reportRepository.reorder(reportFilters, zeroThreshold)).thenReturn(List.of());

            // When
            List<ReorderRow> result = reportService.reorder(reportFilters, zeroThreshold);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).reorder(reportFilters, zeroThreshold);
        }
    }

    @Nested
    @DisplayName("Stock Batches Report Tests")
    class StockBatchesReportTests {

        @Test
        @DisplayName("Should return stock batch data")
        void shouldReturnStockBatchData() {
            // Given
            List<StockBatchRow> expectedBatches = List.of(
                new StockBatchRow("PROD001", "Laptop", "BATCH001", LocalDate.now().plusDays(30), LocalDateTime.now().minusDays(1), 50, "MAIN_STORE"),
                new StockBatchRow("PROD002", "Mouse", "BATCH002", LocalDate.now().plusDays(60), LocalDateTime.now().minusDays(2), 100, "SHELF")
            );

            when(reportRepository.stockBatches(reportFilters)).thenReturn(expectedBatches);

            // When
            List<StockBatchRow> result = reportService.stockBatches(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedBatches, result);
            verify(reportRepository).stockBatches(reportFilters);
        }

        @Test
        @DisplayName("Should handle empty stock batch data")
        void shouldHandleEmptyStockBatchData() {
            // Given
            when(reportRepository.stockBatches(reportFilters)).thenReturn(List.of());

            // When
            List<StockBatchRow> result = reportService.stockBatches(reportFilters);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).stockBatches(reportFilters);
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
                    LocalDateTime.now(), new BigDecimal("150.00"), "Cash: $150.00"),
                new BillHeaderRow(2, 102L, "C-000002", "COUNTER", "Main Store",
                    LocalDateTime.now().minusHours(1), new BigDecimal("250.00"), "Cash: $250.00")
            );

            when(reportRepository.bills(reportFilters)).thenReturn(expectedBills);

            // When
            List<BillHeaderRow> result = reportService.bills(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedBills, result);
            verify(reportRepository).bills(reportFilters);
        }

        @Test
        @DisplayName("Should return bill line data for specific order")
        void shouldReturnBillLineDataForSpecificOrder() {
            // Given
            long orderId = 101L;
            List<BillLineRow> expectedLines = List.of(
                new BillLineRow("PROD001", "Laptop", new BigDecimal("100.00"), 1, new BigDecimal("100.00")),
                new BillLineRow("PROD002", "Mouse", new BigDecimal("25.00"), 2, new BigDecimal("50.00"))
            );

            when(reportRepository.billLines(orderId)).thenReturn(expectedLines);

            // When
            List<BillLineRow> result = reportService.billLines(orderId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedLines, result);
            verify(reportRepository).billLines(orderId);
        }

        @Test
        @DisplayName("Should handle empty bill lines")
        void shouldHandleEmptyBillLines() {
            // Given
            long orderId = 999L;
            when(reportRepository.billLines(orderId)).thenReturn(List.of());

            // When
            List<BillLineRow> result = reportService.billLines(orderId);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).billLines(orderId);
        }
    }

    @Nested
    @DisplayName("Orders Report Tests")
    class OrdersReportTests {

        @Test
        @DisplayName("Should return order header data")
        void shouldReturnOrderHeaderData() {
            // Given
            List<OrderHeaderRow> expectedOrders = List.of(
                new OrderHeaderRow(1, 201L, 1L, "WEB", "Online Store",
                    LocalDateTime.now(), new BigDecimal("300.00"), "Card: ****1234"),
                new OrderHeaderRow(2, 202L, 2L, "WEB", "Online Store",
                    LocalDateTime.now().minusHours(2), new BigDecimal("150.00"), "Card: ****5678")
            );

            when(reportRepository.orders(reportFilters)).thenReturn(expectedOrders);

            // When
            List<OrderHeaderRow> result = reportService.orders(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedOrders, result);
            verify(reportRepository).orders(reportFilters);
        }

        @Test
        @DisplayName("Should return order line data for specific order")
        void shouldReturnOrderLineDataForSpecificOrder() {
            // Given
            long orderId = 201L;
            List<OrderLineRow> expectedLines = List.of(
                new OrderLineRow("PROD001", "Gaming Laptop", new BigDecimal("999.99"), 1, new BigDecimal("999.99")),
                new OrderLineRow("PROD003", "Keyboard", new BigDecimal("75.00"), 1, new BigDecimal("75.00"))
            );

            when(reportRepository.orderLines(orderId)).thenReturn(expectedLines);

            // When
            List<OrderLineRow> result = reportService.orderLines(orderId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedLines, result);
            verify(reportRepository).orderLines(orderId);
        }

        @Test
        @DisplayName("Should handle empty order lines")
        void shouldHandleEmptyOrderLines() {
            // Given
            long orderId = 999L;
            when(reportRepository.orderLines(orderId)).thenReturn(List.of());

            // When
            List<OrderLineRow> result = reportService.orderLines(orderId);

            // Then
            assertTrue(result.isEmpty());
            verify(reportRepository).orderLines(orderId);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ReportService with required dependencies")
        void shouldCreateReportServiceWithRequiredDependencies() {
            // When
            ReportService service = new ReportService(reportRepository);

            // Then
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions for daily sales")
        void shouldHandleRepositoryExceptionsForDailySales() {
            // Given
            when(reportRepository.dailySales(reportFilters)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.dailySales(reportFilters));
            assertEquals("Database error", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions for reorder report")
        void shouldHandleRepositoryExceptionsForReorderReport() {
            // Given
            int threshold = 10;
            when(reportRepository.reorder(reportFilters, threshold)).thenThrow(new RuntimeException("Query failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.reorder(reportFilters, threshold));
            assertEquals("Query failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions for bill lines")
        void shouldHandleRepositoryExceptionsForBillLines() {
            // Given
            long orderId = 101L;
            when(reportRepository.billLines(orderId)).thenThrow(new RuntimeException("Bill not found"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.billLines(orderId));
            assertEquals("Bill not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions for order lines")
        void shouldHandleRepositoryExceptionsForOrderLines() {
            // Given
            long orderId = 201L;
            when(reportRepository.orderLines(orderId)).thenThrow(new RuntimeException("Order not found"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.orderLines(orderId));
            assertEquals("Order not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle multiple report types in sequence")
        void shouldHandleMultipleReportTypesInSequence() {
            // Given
            when(reportRepository.dailySales(reportFilters)).thenReturn(List.of());
            when(reportRepository.reshelve(reportFilters)).thenReturn(List.of());
            when(reportRepository.reorder(reportFilters, 10)).thenReturn(List.of());
            when(reportRepository.stockBatches(reportFilters)).thenReturn(List.of());

            // When
            List<DailySalesRow> dailySales = reportService.dailySales(reportFilters);
            List<ReshelveRow> reshelve = reportService.reshelve(reportFilters);
            List<ReorderRow> reorder = reportService.reorder(reportFilters, 10);
            List<StockBatchRow> batches = reportService.stockBatches(reportFilters);

            // Then
            assertNotNull(dailySales);
            assertNotNull(reshelve);
            assertNotNull(reorder);
            assertNotNull(batches);

            verify(reportRepository).dailySales(reportFilters);
            verify(reportRepository).reshelve(reportFilters);
            verify(reportRepository).reorder(reportFilters, 10);
            verify(reportRepository).stockBatches(reportFilters);
        }

        @Test
        @DisplayName("Should handle bill and order reports together")
        void shouldHandleBillAndOrderReportsTogether() {
            // Given
            long billId = 101L;
            long orderId = 201L;

            when(reportRepository.bills(reportFilters)).thenReturn(List.of());
            when(reportRepository.billLines(billId)).thenReturn(List.of());
            when(reportRepository.orders(reportFilters)).thenReturn(List.of());
            when(reportRepository.orderLines(orderId)).thenReturn(List.of());

            // When
            List<BillHeaderRow> bills = reportService.bills(reportFilters);
            List<BillLineRow> billLines = reportService.billLines(billId);
            List<OrderHeaderRow> orders = reportService.orders(reportFilters);
            List<OrderLineRow> orderLines = reportService.orderLines(orderId);

            // Then
            assertNotNull(bills);
            assertNotNull(billLines);
            assertNotNull(orders);
            assertNotNull(orderLines);

            verify(reportRepository).bills(reportFilters);
            verify(reportRepository).billLines(billId);
            verify(reportRepository).orders(reportFilters);
            verify(reportRepository).orderLines(orderId);
        }
    }
}
