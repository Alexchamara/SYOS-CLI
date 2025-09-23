package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcReportRepository;
import application.reports.dto.*;
import cli.manager.filters.ReportFilters;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("JdbcReportRepository Tests")
class JdbcReportRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ReportFilters reportFilters;

    private JdbcReportRepository reportRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        reportRepository = new JdbcReportRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Daily Sales Report Tests")
    class DailySalesReportTests {

        @Test
        @DisplayName("Should generate daily sales report for single day")
        void shouldGenerateDailySalesReportForSingleDay() throws SQLException {
            // Given
            LocalDate singleDay = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(singleDay);

            setupMockResultSetForDailySales();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            verify(preparedStatement).setObject(1, singleDay);
            verify(preparedStatement).setObject(2, singleDay);
            verify(preparedStatement).setObject(3, singleDay);
            verify(preparedStatement).setObject(4, singleDay);

            verify(connection).prepareStatement(contains("UNION ALL"));
            verify(connection).prepareStatement(contains("ORDER BY location ASC"));
        }

        @Test
        @DisplayName("Should generate daily sales report for date range")
        void shouldGenerateDailySalesReportForDateRange() throws SQLException {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.DATE_RANGE);
            when(reportFilters.fromDate()).thenReturn(startDate);
            when(reportFilters.toDate()).thenReturn(endDate);

            setupMockResultSetForDailySales();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertEquals(1, result.size());
            verify(preparedStatement).setObject(1, startDate);
            verify(preparedStatement).setObject(2, endDate);
            verify(preparedStatement).setObject(3, startDate);
            verify(preparedStatement).setObject(4, endDate);
        }

        @Test
        @DisplayName("Should handle empty daily sales results")
        void shouldHandleEmptyDailySalesResults() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<DailySalesRow> result = reportRepository.dailySales(reportFilters);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions in daily sales")
        void shouldHandleSQLExceptionsInDailySales() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.dailySales(reportFilters));
            assertEquals("Error fetching daily sales", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Reshelve Report Tests")
    class ReshelveReportTests {

        @Test
        @DisplayName("Should generate reshelve report for single day")
        void shouldGenerateReshelveReportForSingleDay() throws SQLException {
            // Given
            LocalDate singleDay = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(singleDay);

            setupMockResultSetForReshelve();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<ReshelveRow> result = reportRepository.reshelve(reportFilters);

            // Then
            assertEquals(1, result.size());
            ReshelveRow row = result.get(0);
            assertEquals(1L, row.id());
            assertEquals("PROD001", row.productCode());
            assertEquals("MAIN_STORE", row.fromLocation());
            assertEquals("SHELF", row.toLocation());

            verify(preparedStatement).setObject(1, singleDay);
            verify(preparedStatement).setObject(2, singleDay);
            verify(connection).prepareStatement(contains("FROM inventory_movement"));
            verify(connection).prepareStatement(contains("WHERE DATE(im.happened_at) BETWEEN"));
        }

        @Test
        @DisplayName("Should generate reshelve report for date range")
        void shouldGenerateReshelveReportForDateRange() throws SQLException {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.DATE_RANGE);
            when(reportFilters.fromDate()).thenReturn(startDate);
            when(reportFilters.toDate()).thenReturn(endDate);

            setupMockResultSetForReshelve();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<ReshelveRow> result = reportRepository.reshelve(reportFilters);

            // Then
            assertEquals(1, result.size());
            verify(preparedStatement).setObject(1, startDate);
            verify(preparedStatement).setObject(2, endDate);
        }

        @Test
        @DisplayName("Should handle empty reshelve results")
        void shouldHandleEmptyReshelveResults() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<ReshelveRow> result = reportRepository.reshelve(reportFilters);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions in reshelve")
        void shouldHandleSQLExceptionsInReshelve() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());
            SQLException sqlException = new SQLException("Reshelve query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.reshelve(reportFilters));
            assertEquals("Error fetching reshelve data", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Reorder Report Tests")
    class ReorderReportTests {

        @Test
        @DisplayName("Should generate reorder report with threshold")
        void shouldGenerateReorderReportWithThreshold() throws SQLException {
            // Given
            int threshold = 50;
            setupMockResultSetForReorder();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // When
            List<ReorderRow> result = reportRepository.reorder(reportFilters, threshold);

            // Then
            assertEquals(2, result.size());

            ReorderRow row1 = result.get(0);
            assertEquals("PROD001", row1.code());
            assertEquals("CRITICAL", row1.status());

            ReorderRow row2 = result.get(1);
            assertEquals("PROD002", row2.code());
            assertEquals("LOW", row2.status());

            verify(preparedStatement).setInt(1, threshold);
            verify(preparedStatement).setInt(2, threshold);
            verify(connection).prepareStatement(contains("CASE"));
            verify(connection).prepareStatement(contains("ORDER BY b.location ASC"));
        }

        @Test
        @DisplayName("Should handle different threshold values")
        void shouldHandleDifferentThresholdValues() throws SQLException {
            // Given
            int lowThreshold = 10;
            int highThreshold = 100;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // Empty results for simplicity

            // When
            reportRepository.reorder(reportFilters, lowThreshold);
            reportRepository.reorder(reportFilters, highThreshold);

            // Then
            verify(preparedStatement).setInt(1, lowThreshold);
            verify(preparedStatement).setInt(2, lowThreshold);
            verify(preparedStatement).setInt(1, highThreshold);
            verify(preparedStatement).setInt(2, highThreshold);
        }

        @Test
        @DisplayName("Should handle zero threshold")
        void shouldHandleZeroThreshold() throws SQLException {
            // Given
            int zeroThreshold = 0;
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<ReorderRow> result = reportRepository.reorder(reportFilters, zeroThreshold);

            // Then
            assertTrue(result.isEmpty());
            verify(preparedStatement).setInt(1, zeroThreshold);
            verify(preparedStatement).setInt(2, zeroThreshold);
        }

        @Test
        @DisplayName("Should handle SQL exceptions in reorder")
        void shouldHandleSQLExceptionsInReorder() throws SQLException {
            // Given
            int threshold = 25;
            SQLException sqlException = new SQLException("Reorder query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.reorder(reportFilters, threshold));
            assertEquals("Error fetching reorder data", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Stock Batches Report Tests")
    class StockBatchesReportTests {

        @Test
        @DisplayName("Should generate stock batches report")
        void shouldGenerateStockBatchesReport() throws SQLException {
            // Given
            setupMockResultSetForStockBatches();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // When
            List<StockBatchRow> result = reportRepository.stockBatches(reportFilters);

            // Then
            assertEquals(2, result.size());

            StockBatchRow row1 = result.get(0);
            assertEquals("PROD001", row1.code());
            assertEquals("Product 1", row1.name());
            assertEquals("MAIN_STORE", row1.location());

            verify(connection).prepareStatement(contains("WHERE b.quantity > 0"));
            verify(connection).prepareStatement(contains("ORDER BY b.expiry ASC, b.received_at ASC"));
        }

        @Test
        @DisplayName("Should handle empty stock batches results")
        void shouldHandleEmptyStockBatchesResults() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<StockBatchRow> result = reportRepository.stockBatches(reportFilters);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions in stock batches")
        void shouldHandleSQLExceptionsInStockBatches() throws SQLException {
            // Given
            SQLException sqlException = new SQLException("Stock batch query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.stockBatches(reportFilters));
            assertEquals("Error fetching stock batch data", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Bills Report Tests")
    class BillsReportTests {

        @Test
        @DisplayName("Should generate bills report for single day")
        void shouldGenerateBillsReportForSingleDay() throws SQLException {
            // Given
            LocalDate singleDay = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(singleDay);

            setupMockResultSetForBills();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<BillHeaderRow> result = reportRepository.bills(reportFilters);

            // Then
            assertEquals(1, result.size());
            BillHeaderRow row = result.get(0);
            assertEquals(1, row.rowNo());
            assertEquals("C-000001", row.serial());
            assertEquals("COUNTER", row.type());
            assertEquals("SHELF", row.store());

            verify(preparedStatement).setObject(1, singleDay);
            verify(preparedStatement).setObject(2, singleDay);
        }

        @Test
        @DisplayName("Should generate bills report for date range")
        void shouldGenerateBillsReportForDateRange() throws SQLException {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.DATE_RANGE);
            when(reportFilters.fromDate()).thenReturn(startDate);
            when(reportFilters.toDate()).thenReturn(endDate);

            setupMockResultSetForBills();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<BillHeaderRow> result = reportRepository.bills(reportFilters);

            // Then
            assertEquals(1, result.size());
            verify(preparedStatement).setObject(1, startDate);
            verify(preparedStatement).setObject(2, endDate);
        }

        @Test
        @DisplayName("Should handle multiple bills with row numbering")
        void shouldHandleMultipleBillsWithRowNumbering() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, true, false);
            when(resultSet.getLong("id")).thenReturn(101L, 102L, 103L);
            when(resultSet.getString("serial")).thenReturn("C-000001", "C-000002", "C-000003");
            when(resultSet.getString("type")).thenReturn("COUNTER");
            when(resultSet.getString("store")).thenReturn("SHELF");
            when(resultSet.getObject("date_time", LocalDateTime.class)).thenReturn(LocalDateTime.now());
            when(resultSet.getBigDecimal("net_total")).thenReturn(new BigDecimal("100.00"));
            when(resultSet.getString("payment_summary")).thenReturn("Cash: 100.00");

            // When
            List<BillHeaderRow> result = reportRepository.bills(reportFilters);

            // Then
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).rowNo());
            assertEquals(2, result.get(1).rowNo());
            assertEquals(3, result.get(2).rowNo());
        }
    }

    @Nested
    @DisplayName("Bill Lines Report Tests")
    class BillLinesReportTests {

        @Test
        @DisplayName("Should get bill lines for order")
        void shouldGetBillLinesForOrder() throws SQLException {
            // Given
            long orderId = 101L;
            setupMockResultSetForBillLines();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // When
            List<BillLineRow> result = reportRepository.billLines(orderId);

            // Then
            assertEquals(2, result.size());

            BillLineRow row1 = result.get(0);
            assertEquals("PROD001", row1.productCode());
            assertEquals("Product 1", row1.name());
            assertEquals(2, row1.qty());

            verify(preparedStatement).setLong(1, orderId);
            verify(connection).prepareStatement(contains("WHERE bl.bill_id = ?"));
        }

        @Test
        @DisplayName("Should handle empty bill lines")
        void shouldHandleEmptyBillLines() throws SQLException {
            // Given
            long orderId = 999L;
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<BillLineRow> result = reportRepository.billLines(orderId);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions in bill lines")
        void shouldHandleSQLExceptionsInBillLines() throws SQLException {
            // Given
            long orderId = 101L;
            SQLException sqlException = new SQLException("Bill lines query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.billLines(orderId));
            assertEquals("Error fetching bill lines", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Orders Report Tests")
    class OrdersReportTests {

        @Test
        @DisplayName("Should generate orders report for single day")
        void shouldGenerateOrdersReportForSingleDay() throws SQLException {
            // Given
            LocalDate singleDay = LocalDate.now();
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(singleDay);

            setupMockResultSetForOrders();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<OrderHeaderRow> result = reportRepository.orders(reportFilters);

            // Then
            assertEquals(1, result.size());
            OrderHeaderRow row = result.get(0);
            assertEquals(1, row.rowNo());
            assertEquals(201L, row.orderId());
            assertEquals(2001L, row.serial());
            assertEquals("ONLINE", row.type());
            assertEquals("WEB", row.store());

            verify(preparedStatement).setObject(1, singleDay);
            verify(preparedStatement).setObject(2, singleDay);
        }

        @Test
        @DisplayName("Should handle multiple orders with row numbering")
        void shouldHandleMultipleOrdersWithRowNumbering() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            when(resultSet.getLong("id")).thenReturn(201L, 202L);
            when(resultSet.getLong("bill_serial")).thenReturn(2001L, 2002L);
            when(resultSet.getString("type")).thenReturn("ONLINE");
            when(resultSet.getString("store")).thenReturn("WEB");
            when(resultSet.getObject("created_at", LocalDateTime.class)).thenReturn(LocalDateTime.now());
            when(resultSet.getBigDecimal("net_total")).thenReturn(new BigDecimal("200.00"));
            when(resultSet.getString("payment_summary")).thenReturn("Online Payment: 200.00");

            // When
            List<OrderHeaderRow> result = reportRepository.orders(reportFilters);

            // Then
            assertEquals(2, result.size());
            assertEquals(1, result.get(0).rowNo());
            assertEquals(2, result.get(1).rowNo());
        }
    }

    @Nested
    @DisplayName("Order Lines Report Tests")
    class OrderLinesReportTests {

        @Test
        @DisplayName("Should get order lines for order")
        void shouldGetOrderLinesForOrder() throws SQLException {
            // Given
            long orderId = 201L;
            setupMockResultSetForOrderLines();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<OrderLineRow> result = reportRepository.orderLines(orderId);

            // Then
            assertEquals(1, result.size());
            OrderLineRow row = result.get(0);
            assertEquals("PROD001", row.productCode());
            assertEquals("Online Product", row.name());
            assertEquals(1, row.qty());

            verify(preparedStatement).setLong(1, orderId);
        }

        @Test
        @DisplayName("Should handle empty order lines")
        void shouldHandleEmptyOrderLines() throws SQLException {
            // Given
            long orderId = 999L;
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<OrderLineRow> result = reportRepository.orderLines(orderId);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions in order lines")
        void shouldHandleSQLExceptionsInOrderLines() throws SQLException {
            // Given
            long orderId = 201L;
            SQLException sqlException = new SQLException("Order lines query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportRepository.orderLines(orderId));
            assertEquals("Error fetching order lines", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcReportRepository with DataSource")
        void shouldCreateJdbcReportRepositoryWithDataSource() {
            // When
            JdbcReportRepository repository = new JdbcReportRepository(dataSource);

            // Then
            assertNotNull(repository);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly on success")
        void shouldCloseResourcesProperlyOnSuccess() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            reportRepository.dailySales(reportFilters);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            when(reportFilters.dateMode()).thenReturn(ReportFilters.DateMode.SINGLE_DAY);
            when(reportFilters.day()).thenReturn(LocalDate.now());
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> reportRepository.dailySales(reportFilters));
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }

    // Helper methods to setup mock ResultSets
    private void setupMockResultSetForDailySales() throws SQLException {
        when(resultSet.getString("product_code")).thenReturn("PROD001", "PROD002");
        when(resultSet.getString("name")).thenReturn("Product 1", "Product 2");
        when(resultSet.getString("location")).thenReturn("SHELF", "WEB");
        when(resultSet.getInt("total_qty")).thenReturn(5, 3);
        when(resultSet.getBigDecimal("gross")).thenReturn(new BigDecimal("500.00"), new BigDecimal("150.00"));
        when(resultSet.getBigDecimal("discount")).thenReturn(new BigDecimal("50.00"), new BigDecimal("15.00"));
        when(resultSet.getBigDecimal("net")).thenReturn(new BigDecimal("450.00"), new BigDecimal("135.00"));
    }

    private void setupMockResultSetForReshelve() throws SQLException {
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("happened_at", LocalDateTime.class)).thenReturn(LocalDateTime.now());
        when(resultSet.getString("product_code")).thenReturn("PROD001");
        when(resultSet.getString("product_name")).thenReturn("Product 1");
        when(resultSet.getString("from_location")).thenReturn("MAIN_STORE");
        when(resultSet.getString("to_location")).thenReturn("SHELF");
        when(resultSet.getInt("quantity")).thenReturn(25);
        when(resultSet.getString("note")).thenReturn("Manual transfer");
    }

    private void setupMockResultSetForReorder() throws SQLException {
        when(resultSet.getString("product_code")).thenReturn("PROD001", "PROD002");
        when(resultSet.getString("name")).thenReturn("Product 1", "Product 2");
        when(resultSet.getLong("batch_id")).thenReturn(1L, 2L);
        when(resultSet.getString("location")).thenReturn("MAIN_STORE", "SHELF");
        when(resultSet.getInt("quantity")).thenReturn(15, 35);
        when(resultSet.getObject("expiry", LocalDate.class)).thenReturn(LocalDate.now().plusDays(30));
        when(resultSet.getString("status")).thenReturn("CRITICAL", "LOW");
    }

    private void setupMockResultSetForStockBatches() throws SQLException {
        when(resultSet.getString("product_code")).thenReturn("PROD001", "PROD002");
        when(resultSet.getString("name")).thenReturn("Product 1", "Product 2");
        when(resultSet.getLong("batch_id")).thenReturn(1L, 2L);
        when(resultSet.getObject("expiry", LocalDate.class)).thenReturn(LocalDate.now().plusDays(30));
        when(resultSet.getObject("received_at", LocalDateTime.class)).thenReturn(LocalDateTime.now().minusDays(5));
        when(resultSet.getInt("quantity")).thenReturn(100, 75);
        when(resultSet.getString("location")).thenReturn("MAIN_STORE", "SHELF");
    }

    private void setupMockResultSetForBills() throws SQLException {
        when(resultSet.getLong("id")).thenReturn(101L);
        when(resultSet.getString("serial")).thenReturn("C-000001");
        when(resultSet.getString("type")).thenReturn("COUNTER");
        when(resultSet.getString("store")).thenReturn("SHELF");
        when(resultSet.getObject("date_time", LocalDateTime.class)).thenReturn(LocalDateTime.now());
        when(resultSet.getBigDecimal("net_total")).thenReturn(new BigDecimal("150.00"));
        when(resultSet.getString("payment_summary")).thenReturn("Cash: 150.00");
    }

    private void setupMockResultSetForOrders() throws SQLException {
        when(resultSet.getLong("id")).thenReturn(201L);
        when(resultSet.getLong("bill_serial")).thenReturn(2001L);
        when(resultSet.getString("type")).thenReturn("ONLINE");
        when(resultSet.getString("store")).thenReturn("WEB");
        when(resultSet.getObject("created_at", LocalDateTime.class)).thenReturn(LocalDateTime.now());
        when(resultSet.getBigDecimal("net_total")).thenReturn(new BigDecimal("300.00"));
        when(resultSet.getString("payment_summary")).thenReturn("Online Payment: 300.00");
    }

    private void setupMockResultSetForBillLines() throws SQLException {
        when(resultSet.getString("product_code")).thenReturn("PROD001", "PROD002");
        when(resultSet.getString("name")).thenReturn("Product 1", "Product 2");
        when(resultSet.getInt("qty")).thenReturn(2, 1);
        when(resultSet.getBigDecimal("unit_price")).thenReturn(new BigDecimal("50.00"), new BigDecimal("25.00"));
        when(resultSet.getBigDecimal("line_total")).thenReturn(new BigDecimal("100.00"), new BigDecimal("25.00"));
    }

    private void setupMockResultSetForOrderLines() throws SQLException {
        when(resultSet.getString("product_code")).thenReturn("PROD001");
        when(resultSet.getString("name")).thenReturn("Online Product");
        when(resultSet.getInt("qty")).thenReturn(1);
        when(resultSet.getBigDecimal("unit_price")).thenReturn(new BigDecimal("199.99"));
        when(resultSet.getBigDecimal("line_total")).thenReturn(new BigDecimal("199.99"));
    }
}
