package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.QuoteUseCase;
import domain.billing.BillLine;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

@DisplayName("JdbcOrderRepository Tests")
class JdbcOrderRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private PreparedStatement updateStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcOrderRepository orderRepository;
    private AutoCloseable mockCloseable;

    @BeforeEach
    void setUp() throws SQLException {
        mockCloseable = MockitoAnnotations.openMocks(this);
        orderRepository = new JdbcOrderRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
    }

    @Nested
    @DisplayName("Next Bill Serial Tests")
    class NextBillSerialTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(connection.prepareStatement(contains("SELECT next_val FROM bill_number"))).thenReturn(preparedStatement);
            when(connection.prepareStatement(contains("UPDATE bill_number SET next_val"))).thenReturn(updateStatement);
        }

        @Test
        @DisplayName("Should get next bill serial successfully")
        void shouldGetNextBillSerialSuccessfully() throws SQLException {
            // Given
            String type = "WEB";
            long currentSerial = 1000L;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(currentSerial);

            // When
            long result = orderRepository.nextBillSerial(type);

            // Then
            assertEquals(currentSerial, result);
            verify(preparedStatement).setString(1, type);
            verify(preparedStatement).executeQuery();
            verify(updateStatement).setLong(1, currentSerial + 1);
            verify(updateStatement).setString(2, type);
            verify(updateStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should throw RuntimeException when bill type not found")
        void shouldThrowRuntimeExceptionWhenBillTypeNotFound() throws SQLException {
            // Given
            String type = "UNKNOWN";

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No record found

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.nextBillSerial(type));
            assertEquals("Failed to get next bill serial", exception.getMessage());
            assertInstanceOf(IllegalStateException.class, exception.getCause());
            assertEquals("Unknown bill type scope: UNKNOWN", exception.getCause().getMessage());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during serial retrieval")
        void shouldHandleSQLExceptionsDuringSerialRetrieval() throws SQLException {
            // Given
            String type = "WEB";
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.nextBillSerial(type));
            assertEquals("Failed to get next bill serial", exception.getMessage());
            assertSame(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle different bill types")
        void shouldHandleDifferentBillTypes() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, true);
            when(resultSet.getLong(1)).thenReturn(2000L, 3000L, 4000L);

            // When
            long webSerial = orderRepository.nextBillSerial("WEB");
            long counterSerial = orderRepository.nextBillSerial("COUNTER");
            long quoteSerial = orderRepository.nextBillSerial("QUOTE");

            // Then
            assertEquals(2000L, webSerial);
            assertEquals(3000L, counterSerial);
            assertEquals(4000L, quoteSerial);
        }

        @Test
        @DisplayName("Should handle update statement failure")
        void shouldHandleUpdateStatementFailure() throws SQLException {
            // Given
            String type = "WEB";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(1000L);
            when(updateStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.nextBillSerial(type));
            assertEquals("Failed to get next bill serial", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Save Preview Tests")
    class SavePreviewTests {

        private PreparedStatement serialStatement;

        @BeforeEach
        void setUp() throws SQLException {
            // Create mock for nextBillSerial call
            serialStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(contains("SELECT next_val FROM bill_number"))).thenReturn(serialStatement);
            when(connection.prepareStatement(contains("UPDATE bill_number SET next_val"))).thenReturn(updateStatement);
            // Mock for main insert
            when(connection.prepareStatement(contains("INSERT INTO orders"), anyInt())).thenReturn(preparedStatement);
        }

        @Test
        @DisplayName("Should save preview order successfully")
        void shouldSavePreviewOrderSuccessfully() throws SQLException {
            // Given
            String type = "WEB";
            String location = "Online Store";
            long userId = 123L;
            QuoteUseCase.Quote quote = createTestQuote();
            long expectedOrderId = 789L;
            long billSerial = 5000L;

            // Mock nextBillSerial call
            when(serialStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(billSerial);

            // Mock main insert
            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true); // For serial query and generated keys
            when(resultSet.getLong(1)).thenReturn(billSerial, expectedOrderId);

            // When
            long result = orderRepository.savePreview(type, location, userId, quote);

            // Then
            assertEquals(expectedOrderId, result);
            verify(preparedStatement).setLong(1, billSerial);
            verify(preparedStatement).setString(2, type);
            verify(preparedStatement).setString(3, location);
            verify(preparedStatement).setLong(4, userId);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should save preview order with null user ID")
        void shouldSavePreviewOrderWithNullUserId() throws SQLException {
            // Given
            String type = "COUNTER";
            String location = "Main Store";
            Long userId = null;
            QuoteUseCase.Quote quote = createTestQuote();

            // Mock nextBillSerial call
            when(serialStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(6000L);

            // Mock main insert
            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true);
            when(resultSet.getLong(1)).thenReturn(6000L, 790L);

            // When
            orderRepository.savePreview(type, location, userId, quote);

            // Then
            verify(preparedStatement).setNull(4, java.sql.Types.BIGINT);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during preview save")
        void shouldHandleSQLExceptionsDuringPreviewSave() throws SQLException {
            // Given
            String type = "WEB";
            String location = "Online Store";
            long userId = 123L;
            QuoteUseCase.Quote quote = createTestQuote();
            SQLException sqlException = new SQLException("Insert failed");

            // Mock nextBillSerial to succeed
            when(serialStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(5000L);

            // Make the main insert fail
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.savePreview(type, location, userId, quote));
            assertTrue(exception.getMessage().contains("Failed to save preview order"));
            assertSame(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle failure to get generated keys")
        void shouldHandleFailureToGetGeneratedKeys() throws SQLException {
            // Given
            String type = "WEB";
            String location = "Online Store";
            long userId = 123L;
            QuoteUseCase.Quote quote = createTestQuote();

            // Mock nextBillSerial to succeed
            when(serialStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false); // Serial succeeds, generated keys fail
            when(resultSet.getLong(1)).thenReturn(5000L);

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.savePreview(type, location, userId, quote));
            assertTrue(exception.getMessage().contains("Failed to save preview order"));
        }

        @Test
        @DisplayName("Should handle nextBillSerial failure during preview save")
        void shouldHandleNextBillSerialFailureDuringPreviewSave() throws SQLException {
            // Given
            String type = "WEB";
            String location = "Online Store";
            long userId = 123L;
            QuoteUseCase.Quote quote = createTestQuote();

            // Make nextBillSerial fail
            when(serialStatement.executeQuery()).thenThrow(new SQLException("Serial query failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.savePreview(type, location, userId, quote));
            assertEquals("Failed to save preview order: Failed to get next bill serial", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Save Lines Tests")
    class SaveLinesTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(connection.prepareStatement(contains("INSERT INTO order_lines"))).thenReturn(preparedStatement);
        }

        @Test
        @DisplayName("Should save order lines successfully")
        void shouldSaveOrderLinesSuccessfully() throws SQLException {
            // Given
            long orderId = 789L;
            List<BillLine> lines = List.of(
                new BillLine(new Code("PROD001"), "Product 1", new Quantity(2), Money.of(new BigDecimal("25.00"))),
                new BillLine(new Code("PROD002"), "Product 2", new Quantity(1), Money.of(new BigDecimal("50.00")))
            );

            // When
            orderRepository.saveLines(orderId, lines);

            // Then
            verify(preparedStatement, times(2)).setLong(1, orderId);
            verify(preparedStatement).setString(2, "PROD001");
            verify(preparedStatement).setString(3, "Product 1");
            verify(preparedStatement).setInt(5, 2);
            verify(preparedStatement).setString(2, "PROD002");
            verify(preparedStatement).setString(3, "Product 2");
            verify(preparedStatement).setInt(5, 1);
            verify(preparedStatement, times(2)).addBatch();
            verify(preparedStatement).executeBatch();
        }

        @Test
        @DisplayName("Should handle empty lines list")
        void shouldHandleEmptyLinesList() throws SQLException {
            // Given
            long orderId = 789L;
            List<BillLine> lines = List.of();

            // When
            orderRepository.saveLines(orderId, lines);

            // Then
            verify(preparedStatement, never()).addBatch();
            verify(preparedStatement).executeBatch(); // Should still call executeBatch
        }

        @Test
        @DisplayName("Should handle SQL exceptions during lines save")
        void shouldHandleSQLExceptionsDuringLinesSave() throws SQLException {
            // Given
            long orderId = 789L;
            List<BillLine> lines = List.of(
                new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("10.00")))
            );
            SQLException sqlException = new SQLException("Batch execution failed");
            when(preparedStatement.executeBatch()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.saveLines(orderId, lines));
            assertEquals("Failed to save order lines", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle single line correctly")
        void shouldHandleSingleLineCorrectly() throws SQLException {
            // Given
            long orderId = 123L;
            List<BillLine> lines = List.of(
                new BillLine(new Code("SINGLE"), "Single Product", new Quantity(5), Money.of(new BigDecimal("15.99")))
            );

            // When
            orderRepository.saveLines(orderId, lines);

            // Then
            verify(preparedStatement).setLong(1, orderId);
            verify(preparedStatement).setString(2, "SINGLE");
            verify(preparedStatement).setString(3, "Single Product");
            verify(preparedStatement).setBigDecimal(4, new BigDecimal("15.99"));
            verify(preparedStatement).setInt(5, 5);
            verify(preparedStatement).addBatch();
            verify(preparedStatement).executeBatch();
        }

        @Test
        @DisplayName("Should handle lines with different quantities and prices")
        void shouldHandleLinesWithDifferentQuantitiesAndPrices() throws SQLException {
            // Given
            long orderId = 456L;
            List<BillLine> lines = List.of(
                new BillLine(new Code("A"), "Product A", new Quantity(1), Money.of(new BigDecimal("10.00"))),
                new BillLine(new Code("B"), "Product B", new Quantity(3), Money.of(new BigDecimal("5.50"))),
                new BillLine(new Code("C"), "Product C", new Quantity(10), Money.of(new BigDecimal("1.25")))
            );

            // When
            orderRepository.saveLines(orderId, lines);

            // Then
            verify(preparedStatement, times(3)).setLong(1, orderId);
            verify(preparedStatement).setString(2, "A");
            verify(preparedStatement).setString(2, "B");
            verify(preparedStatement).setString(2, "C");
            verify(preparedStatement, times(3)).addBatch();
            verify(preparedStatement).executeBatch();
        }
    }

    @Nested
    @DisplayName("Save Final Tests")
    class SaveFinalTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(connection.prepareStatement(contains("UPDATE orders SET status = 'FINAL'"))).thenReturn(preparedStatement);
        }

        @Test
        @DisplayName("Should finalize order successfully")
        void shouldFinalizeOrderSuccessfully() throws SQLException {
            // Given
            long orderId = 789L;
            QuoteUseCase.Quote quote = createTestQuote();

            // When
            orderRepository.saveFinal(orderId, quote);

            // Then
            verify(preparedStatement).setBigDecimal(1, quote.subtotal().amount());
            verify(preparedStatement).setBigDecimal(2, quote.discount().amount());
            verify(preparedStatement).setBigDecimal(3, quote.total().amount());
            verify(preparedStatement).setLong(4, orderId);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle SQL exceptions during final save")
        void shouldHandleSQLExceptionsDuringFinalSave() throws SQLException {
            // Given
            long orderId = 789L;
            QuoteUseCase.Quote quote = createTestQuote();
            SQLException sqlException = new SQLException("Update failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.saveFinal(orderId, quote));
            assertEquals("Failed to finalize order", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle quote with zero values")
        void shouldHandleQuoteWithZeroValues() throws SQLException {
            // Given
            long orderId = 999L;
            QuoteUseCase.Quote zeroQuote = new QuoteUseCase.Quote(
                List.of(),
                Money.of(BigDecimal.ZERO),
                Money.of(BigDecimal.ZERO),
                Money.of(BigDecimal.ZERO)
            );

            // When
            orderRepository.saveFinal(orderId, zeroQuote);

            // Then
            verify(preparedStatement).setBigDecimal(1, BigDecimal.ZERO);
            verify(preparedStatement).setBigDecimal(2, BigDecimal.ZERO);
            verify(preparedStatement).setBigDecimal(3, BigDecimal.ZERO);
            verify(preparedStatement).setLong(4, orderId);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle quote with large values")
        void shouldHandleQuoteWithLargeValues() throws SQLException {
            // Given
            long orderId = 888L;
            QuoteUseCase.Quote largeQuote = new QuoteUseCase.Quote(
                List.of(),
                Money.of(new BigDecimal("99999.99")),
                Money.of(new BigDecimal("9999.99")),
                Money.of(new BigDecimal("89999.99"))
            );

            // When
            orderRepository.saveFinal(orderId, largeQuote);

            // Then
            verify(preparedStatement).setBigDecimal(1, new BigDecimal("99999.99"));
            verify(preparedStatement).setBigDecimal(2, new BigDecimal("9999.99"));
            verify(preparedStatement).setBigDecimal(3, new BigDecimal("89999.99"));
            verify(preparedStatement).setLong(4, orderId);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcOrderRepository with DataSource")
        void shouldCreateJdbcOrderRepositoryWithDataSource() {
            // When
            JdbcOrderRepository repository = new JdbcOrderRepository(dataSource);

            // Then
            assertNotNull(repository);
        }

        @Test
        @DisplayName("Should accept null DataSource in constructor")
        void shouldAcceptNullDataSourceInConstructor() {
            // When & Then
            assertDoesNotThrow(() -> new JdbcOrderRepository(null));
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        }

        @Test
        @DisplayName("Should close resources properly on success")
        void shouldCloseResourcesProperlyOnSuccess() throws SQLException {
            // Given
            long orderId = 789L;
            QuoteUseCase.Quote quote = createTestQuote();

            // When
            orderRepository.saveFinal(orderId, quote);

            // Then
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            String type = "WEB";
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> orderRepository.nextBillSerial(type));
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should handle resource closure exceptions gracefully")
        void shouldHandleResourceClosureExceptionsGracefully() throws SQLException {
            // Given
            String type = "WEB";
            doThrow(new SQLException("Close failed")).when(preparedStatement).close();

            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> orderRepository.nextBillSerial(type));
            // Should not throw additional exceptions due to close failure
        }
    }

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should handle complete order flow")
        void shouldHandleCompleteOrderFlow() throws SQLException {
            // Given - Setup for nextBillSerial in savePreview
            PreparedStatement serialStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(contains("SELECT next_val FROM bill_number"))).thenReturn(serialStatement);
            when(connection.prepareStatement(contains("UPDATE bill_number SET next_val"))).thenReturn(updateStatement);
            when(connection.prepareStatement(contains("INSERT INTO orders"), anyInt())).thenReturn(preparedStatement);

            PreparedStatement linesStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(contains("INSERT INTO order_lines"))).thenReturn(linesStatement);

            PreparedStatement finalStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(contains("UPDATE orders SET status = 'FINAL'"))).thenReturn(finalStatement);

            when(serialStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true);
            when(resultSet.getLong(1)).thenReturn(1000L, 123L);
            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

            QuoteUseCase.Quote quote = createTestQuote();
            List<BillLine> lines = quote.lines();

            // When - Complete flow
            long orderId = orderRepository.savePreview("WEB", "Online", 1L, quote);
            orderRepository.saveLines(orderId, lines);
            orderRepository.saveFinal(orderId, quote);

            // Then
            assertEquals(123L, orderId);
            verify(linesStatement).executeBatch();
            verify(finalStatement).executeUpdate();
        }
    }

    // Helper method to create test quote
    private QuoteUseCase.Quote createTestQuote() {
        BillLine line = new BillLine(new Code("PROD001"), "Test Product", new Quantity(1), Money.of(new BigDecimal("100.00")));
        return new QuoteUseCase.Quote(
            List.of(line),
            Money.of(new BigDecimal("100.00")), // subtotal
            Money.of(new BigDecimal("10.00")),  // discount
            Money.of(new BigDecimal("90.00"))   // total
        );
    }
}
