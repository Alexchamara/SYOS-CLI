package application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.concurrency.Tx;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

@DisplayName("BillNumberService Tests")
class BillNumberServiceTest {

    @Mock
    private Tx tx;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement selectStatement;
    @Mock
    private PreparedStatement updateStatement;
    @Mock
    private ResultSet resultSet;

    private BillNumberService billNumberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        billNumberService = new BillNumberService(tx);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create BillNumberService with Tx dependency")
        void shouldCreateBillNumberServiceWithTxDependency() {
            // When
            BillNumberService service = new BillNumberService(tx);

            // Then
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Next Method Tests")
    class NextMethodTests {

        @Test
        @DisplayName("Should generate next bill number for COUNTER scope")
        void shouldGenerateNextBillNumberForCounterScope() throws SQLException {
            // Given
            String scope = "COUNTER";
            long currentValue = 5L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("C-000005", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should generate next bill number for WEB scope")
        void shouldGenerateNextBillNumberForWebScope() throws SQLException {
            // Given
            String scope = "WEB";
            long currentValue = 15L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("W-000015", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should handle single character scope")
        void shouldHandleSingleCharacterScope() throws SQLException {
            // Given
            String scope = "X";
            long currentValue = 1L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("X-000001", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should handle lowercase scope and convert to uppercase")
        void shouldHandleLowercaseScopeAndConvertToUppercase() throws SQLException {
            // Given
            String scope = "counter";
            long currentValue = 3L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("C-000003", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should handle empty scope with X prefix")
        void shouldHandleEmptyScopeWithXPrefix() throws SQLException {
            // Given
            String scope = "";
            long currentValue = 1L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("X-000001", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should handle whitespace-only scope with X prefix")
        void shouldHandleWhitespaceOnlyScopeWithXPrefix() throws SQLException {
            // Given
            String scope = "   ";
            long currentValue = 7L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("X-000007", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should format numbers with leading zeros")
        void shouldFormatNumbersWithLeadingZeros() throws SQLException {
            // Given
            String scope = "TEST";
            long currentValue = 999999L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("T-999999", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should handle large numbers")
        void shouldHandleLargeNumbers() throws SQLException {
            // Given
            String scope = "LARGE";
            long currentValue = 1000000L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("L-1000000", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }

        @Test
        @DisplayName("Should handle zero as current value")
        void shouldHandleZeroAsCurrentValue() throws SQLException {
            // Given
            String scope = "ZERO";
            long currentValue = 0L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            String result = billNumberService.next(scope);

            // Then
            assertEquals("Z-000000", result);
            verifyDatabaseInteractions(scope, currentValue + 1);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw IllegalStateException when scope not found in database")
        void shouldThrowIllegalStateExceptionWhenScopeNotFound() throws SQLException {
            // Given
            String scope = "UNKNOWN";
            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, String> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(connection.prepareStatement(anyString())).thenReturn(selectStatement);
            when(selectStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No result found

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billNumberService.next(scope));

            assertTrue(exception.getCause() instanceof IllegalStateException);
            assertTrue(exception.getCause().getMessage().contains("Unknown scope: " + scope));
        }

        @Test
        @DisplayName("Should wrap SQLException in RuntimeException")
        void shouldWrapSqlExceptionInRuntimeException() throws SQLException {
            // Given
            String scope = "ERROR";
            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, String> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billNumberService.next(scope));

            assertTrue(exception.getCause() instanceof SQLException);
            assertEquals("Database error", exception.getCause().getMessage());
        }

        @Test
        @DisplayName("Should handle ResultSet access SQLException")
        void shouldHandleResultSetAccessSqlException() throws SQLException {
            // Given
            String scope = "RESULT_ERROR";
            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, String> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(connection.prepareStatement(anyString())).thenReturn(selectStatement);
            when(selectStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenThrow(new SQLException("ResultSet error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billNumberService.next(scope));

            assertTrue(exception.getCause() instanceof SQLException);
            assertEquals("ResultSet error", exception.getCause().getMessage());
        }

        @Test
        @DisplayName("Should handle update statement SQLException")
        void shouldHandleUpdateStatementSqlException() throws SQLException {
            // Given
            String scope = "UPDATE_ERROR";
            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, String> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(connection.prepareStatement(anyString())).thenReturn(selectStatement, updateStatement);
            when(selectStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(5L);
            when(updateStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billNumberService.next(scope));

            assertTrue(exception.getCause() instanceof SQLException);
            assertEquals("Update failed", exception.getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("Transaction Tests")
    class TransactionTests {

        @Test
        @DisplayName("Should use transaction for database operations")
        void shouldUseTransactionForDatabaseOperations() throws SQLException {
            // Given
            String scope = "TRANSACTION";
            long currentValue = 1L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            billNumberService.next(scope);

            // Then
            verify(tx).inTx(any());
        }

        @Test
        @DisplayName("Should use FOR UPDATE lock in select statement")
        void shouldUseForUpdateLockInSelectStatement() throws SQLException {
            // Given
            String scope = "LOCK_TEST";
            long currentValue = 1L;
            setupSuccessfulMocks(scope, currentValue);

            // When
            billNumberService.next(scope);

            // Then
            verify(connection).prepareStatement(contains("FOR UPDATE"));
        }
    }

    // Helper methods
    private void setupSuccessfulMocks(String scope, long currentValue) throws SQLException {
        when(tx.inTx(any())).thenAnswer(invocation -> {
            Function<Connection, String> function = invocation.getArgument(0);
            return function.apply(connection);
        });

        when(connection.prepareStatement(anyString())).thenReturn(selectStatement, updateStatement);
        when(selectStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(currentValue);
        when(updateStatement.executeUpdate()).thenReturn(1);
    }

    private void verifyDatabaseInteractions(String scope, long expectedNewValue) throws SQLException {
        verify(tx).inTx(any());
        verify(connection, times(2)).prepareStatement(anyString());
        verify(selectStatement).setString(1, scope);
        verify(selectStatement).executeQuery();
        verify(updateStatement).setLong(1, expectedNewValue);
        verify(updateStatement).setString(2, scope);
        verify(updateStatement).executeUpdate();
    }
}
