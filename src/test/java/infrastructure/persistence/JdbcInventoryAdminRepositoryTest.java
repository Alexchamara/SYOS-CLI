package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcInventoryAdminRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@DisplayName("JdbcInventoryAdminRepository Tests")
class JdbcInventoryAdminRepositoryTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcInventoryAdminRepository inventoryAdminRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        inventoryAdminRepository = new JdbcInventoryAdminRepository();

        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
    }

    @Nested
    @DisplayName("Insert Batch Tests")
    class InsertBatchTests {

        @Test
        @DisplayName("Should insert batch successfully with expiry date")
        void shouldInsertBatchSuccessfullyWithExpiryDate() throws SQLException {
            // Given
            String productCode = "prod001";
            String location = "MAIN_STORE";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 100;
            long expectedBatchId = 123L;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(expectedBatchId);

            // When
            long result = inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

            // Then
            assertEquals(expectedBatchId, result);
            verify(connection).prepareStatement(contains("INSERT INTO batch"), eq(Statement.RETURN_GENERATED_KEYS));
            verify(preparedStatement).setString(1, "PROD001"); // Should be uppercase
            verify(preparedStatement).setString(2, location);
            verify(preparedStatement).setTimestamp(3, Timestamp.valueOf(receivedAt));
            verify(preparedStatement).setDate(4, Date.valueOf(expiry));
            verify(preparedStatement).setInt(5, quantity);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should insert batch successfully with null expiry date")
        void shouldInsertBatchSuccessfullyWithNullExpiryDate() throws SQLException {
            // Given
            String productCode = "PROD002";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = null; // No expiry
            int quantity = 50;
            long expectedBatchId = 124L;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(expectedBatchId);

            // When
            long result = inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

            // Then
            assertEquals(expectedBatchId, result);
            verify(preparedStatement).setString(1, "PROD002");
            verify(preparedStatement).setNull(4, Types.DATE);
            verify(preparedStatement).setInt(5, quantity);
        }

        @Test
        @DisplayName("Should convert product code to uppercase")
        void shouldConvertProductCodeToUppercase() throws SQLException {
            // Given
            String lowercaseCode = "prod001";
            String location = "WEB";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(45);
            int quantity = 75;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(125L);

            // When
            inventoryAdminRepository.insertBatch(connection, lowercaseCode, location, receivedAt, expiry, quantity);

            // Then
            verify(preparedStatement).setString(1, "PROD001"); // Should be converted to uppercase
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() throws SQLException {
            // Given
            String productCode = "PROD003";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(60);
            int quantity = 25;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(126L, 127L, 128L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, "MAIN_STORE", receivedAt, expiry, quantity);
            inventoryAdminRepository.insertBatch(connection, productCode, "SHELF", receivedAt, expiry, quantity);
            inventoryAdminRepository.insertBatch(connection, productCode, "WEB", receivedAt, expiry, quantity);

            // Then
            verify(preparedStatement).setString(2, "MAIN_STORE");
            verify(preparedStatement).setString(2, "SHELF");
            verify(preparedStatement).setString(2, "WEB");
        }

        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() throws SQLException {
            // Given
            String productCode = "PROD004";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 0;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(129L);

            // When
            long result = inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

            // Then
            assertEquals(129L, result);
            verify(preparedStatement).setInt(5, 0);
        }

        @Test
        @DisplayName("Should handle very large quantities")
        void shouldHandleVeryLargeQuantities() throws SQLException {
            // Given
            String productCode = "PROD005";
            String location = "MAIN_STORE";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(90);
            int quantity = Integer.MAX_VALUE;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(130L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

            // Then
            verify(preparedStatement).setInt(5, Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during insert")
        void shouldHandleSQLExceptionsDuringInsert() throws SQLException {
            // Given
            String productCode = "PROD006";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 50;
            SQLException sqlException = new SQLException("Insert failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle failure to get generated keys")
        void shouldHandleFailureToGetGeneratedKeys() throws SQLException {
            // Given
            String productCode = "PROD007";
            String location = "WEB";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 25;

            when(resultSet.next()).thenReturn(false); // No generated keys

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity));
            // Should fail when trying to get generated key
        }
    }

    @Nested
    @DisplayName("DateTime Handling Tests")
    class DateTimeHandlingTests {

        @Test
        @DisplayName("Should handle past received dates")
        void shouldHandlePastReceivedDates() throws SQLException {
            // Given
            String productCode = "PROD008";
            String location = "MAIN_STORE";
            LocalDateTime pastReceived = LocalDateTime.now().minusDays(10);
            LocalDate expiry = LocalDate.now().plusDays(20);
            int quantity = 40;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(131L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, location, pastReceived, expiry, quantity);

            // Then
            verify(preparedStatement).setTimestamp(3, Timestamp.valueOf(pastReceived));
        }

        @Test
        @DisplayName("Should handle future expiry dates")
        void shouldHandleFutureExpiryDates() throws SQLException {
            // Given
            String productCode = "PROD009";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate futureExpiry = LocalDate.now().plusYears(2);
            int quantity = 60;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(132L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, futureExpiry, quantity);

            // Then
            verify(preparedStatement).setDate(4, Date.valueOf(futureExpiry));
        }

        @Test
        @DisplayName("Should handle past expiry dates")
        void shouldHandlePastExpiryDates() throws SQLException {
            // Given
            String productCode = "PROD010";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate pastExpiry = LocalDate.now().minusDays(5); // Already expired
            int quantity = 30;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(133L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, pastExpiry, quantity);

            // Then
            verify(preparedStatement).setDate(4, Date.valueOf(pastExpiry));
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly on success")
        void shouldCloseResourcesProperlyOnSuccess() throws SQLException {
            // Given
            String productCode = "PROD011";
            String location = "MAIN_STORE";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 80;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(134L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            String productCode = "PROD012";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 45;

            when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));

            // When & Then
            assertThrows(RuntimeException.class,
                () -> inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity));
            verify(preparedStatement).close();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty product codes")
        void shouldHandleEmptyProductCodes() throws SQLException {
            // Given
            String emptyProductCode = "";
            String location = "MAIN_STORE";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 20;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(135L);

            // When
            inventoryAdminRepository.insertBatch(connection, emptyProductCode, location, receivedAt, expiry, quantity);

            // Then
            verify(preparedStatement).setString(1, ""); // Empty string converted to uppercase is still empty
        }

        @Test
        @DisplayName("Should handle very long product codes")
        void shouldHandleVeryLongProductCodes() throws SQLException {
            // Given
            String longProductCode = "very-long-product-code-" + "x".repeat(100);
            String location = "WEB";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 15;

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(136L);

            // When
            inventoryAdminRepository.insertBatch(connection, longProductCode, location, receivedAt, expiry, quantity);

            // Then
            verify(preparedStatement).setString(1, longProductCode.toUpperCase());
        }

        @Test
        @DisplayName("Should handle negative quantities")
        void shouldHandleNegativeQuantities() throws SQLException {
            // Given
            String productCode = "PROD013";
            String location = "SHELF";
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = -10; // Negative quantity (correction/adjustment)

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(137L);

            // When
            inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

            // Then
            verify(preparedStatement).setInt(5, -10);
        }
    }
}
