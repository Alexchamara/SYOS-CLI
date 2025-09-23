package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcOrderRepository;
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
    private ResultSet resultSet;

    private JdbcOrderRepository orderRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        orderRepository = new JdbcOrderRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Next Bill Serial Tests")
    class NextBillSerialTests {

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
            verify(connection).prepareStatement(contains("SELECT next_val FROM bill_number"));
            verify(preparedStatement).setString(1, type);
            verify(preparedStatement).executeQuery();

            // Verify update statement
            verify(connection).prepareStatement(contains("UPDATE bill_number SET next_val"));
        }

        @Test
        @DisplayName("Should throw exception when bill type not found")
        void shouldThrowExceptionWhenBillTypeNotFound() throws SQLException {
            // Given
            String type = "UNKNOWN";

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No record found

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderRepository.nextBillSerial(type));
            assertEquals("Unknown bill type scope: UNKNOWN", exception.getMessage());
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
            assertTrue(exception.getMessage().contains("Failed to get next bill serial"));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle different bill types")
        void shouldHandleDifferentBillTypes() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
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
    }

    @Nested
    @DisplayName("Save Preview Tests")
    class SavePreviewTests {

        @Test
        @DisplayName("Should save preview order successfully")
        void shouldSavePreviewOrderSuccessfully() throws SQLException {
            // Given
            String type = "WEB";
            String location = "Online Store";
            Long userId = 123L;
            QuoteUseCase.Quote quote = createTestQuote();
            long expectedOrderId = 789L;

            // Mock nextBillSerial call
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true); // For serial query and generated keys
            when(resultSet.getLong(1)).thenReturn(5000L, expectedOrderId); // Serial, then order ID

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

            // When
            long result = orderRepository.savePreview(type, location, userId, quote);

            // Then
            assertEquals(expectedOrderId, result);
            verify(connection, times(2)).prepareStatement(anyString(), anyInt());
            verify(preparedStatement).setString(2, type);
            verify(preparedStatement).setString(3, location);
            verify(preparedStatement).setLong(4, userId);
            verify(preparedStatement, times(2)).executeUpdate();
        }

        @Test
        @DisplayName("Should save preview order with null user ID")
        void shouldSavePreviewOrderWithNullUserId() throws SQLException {
            // Given
            String type = "COUNTER";
            String location = "Main Store";
            Long userId = null; // No user for counter orders
            QuoteUseCase.Quote quote = createTestQuote();

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true);
            when(resultSet.getLong(1)).thenReturn(6000L, 790L);
            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

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
            Long userId = 123L;
            QuoteUseCase.Quote quote = createTestQuote();
            SQLException sqlException = new SQLException("Insert failed");

            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderRepository.savePreview(type, location, userId, quote));
            assertTrue(exception.getMessage().contains("Failed to save preview order"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Save Lines Tests")
    class SaveLinesTests {

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
            verify(connection).prepareStatement(contains("INSERT INTO order_lines"));
            verify(preparedStatement, times(2)).addBatch();
            verify(preparedStatement).executeBatch();

            // Verify first line parameters
            verify(preparedStatement).setLong(1, orderId);
            verify(preparedStatement).setString(2, "PROD001");
            verify(preparedStatement).setString(3, "Product 1");
            verify(preparedStatement).setInt(5, 2);
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
    }

    @Nested
    @DisplayName("Save Final Tests")
    class SaveFinalTests {

        @Test
        @DisplayName("Should finalize order successfully")
        void shouldFinalizeOrderSuccessfully() throws SQLException {
            // Given
            long orderId = 789L;
            QuoteUseCase.Quote quote = createTestQuote();

            // When
            orderRepository.saveFinal(orderId, quote);

            // Then
            verify(connection).prepareStatement(contains("UPDATE orders SET status = 'FINAL'"));
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
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

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
