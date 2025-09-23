package application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.BillNumberService;
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
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        billNumberService = new BillNumberService(tx);
    }

    @Test
    @DisplayName("Should create BillNumberService with Tx dependency")
    void shouldCreateBillNumberServiceWithTxDependency() {
        // When
        BillNumberService service = new BillNumberService(tx);

        // Then
        assertNotNull(service);
    }

    @Test
    @DisplayName("Should generate next bill number for COUNTER scope")
    void shouldGenerateNextBillNumberForCounterScope() throws SQLException {
        // Given
        String scope = "COUNTER";
        long currentValue = 5L;
        setupMocksForSuccessfulGeneration(scope, currentValue);

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
        setupMocksForSuccessfulGeneration(scope, currentValue);

        // When
        String result = billNumberService.next(scope);

        // Then
        assertEquals("W-000015", result);
        verifyDatabaseInteractions(scope, currentValue + 1);
    }

    @Test
    @DisplayName("Should handle empty scope with X prefix")
    void shouldHandleEmptyScopeWithXPrefix() throws SQLException {
        // Given
        String scope = "";
        long currentValue = 1L;
        setupMocksForSuccessfulGeneration(scope, currentValue);

        // When
        String result = billNumberService.next(scope);

        // Then
        assertEquals("X-000001", result);
        verifyDatabaseInteractions(scope, currentValue + 1);
    }

    @Test
    @DisplayName("Should handle single character scope")
    void shouldHandleSingleCharacterScope() throws SQLException {
        // Given
        String scope = "Q";
        long currentValue = 99L;
        setupMocksForSuccessfulGeneration(scope, currentValue);

        // When
        String result = billNumberService.next(scope);

        // Then
        assertEquals("Q-000099", result);
        verifyDatabaseInteractions(scope, currentValue + 1);
    }

    @Test
    @DisplayName("Should convert scope prefix to uppercase")
    void shouldConvertScopePrefixToUppercase() throws SQLException {
        // Given
        String scope = "quote";
        long currentValue = 3L;
        setupMocksForSuccessfulGeneration(scope, currentValue);

        // When
        String result = billNumberService.next(scope);

        // Then
        assertEquals("Q-000003", result);
        verifyDatabaseInteractions(scope, currentValue + 1);
    }

    @Test
    @DisplayName("Should format numbers with leading zeros")
    void shouldFormatNumbersWithLeadingZeros() throws SQLException {
        // Given
        String scope = "INVOICE";
        long currentValue = 1L;
        setupMocksForSuccessfulGeneration(scope, currentValue);

        // When
        String result = billNumberService.next(scope);

        // Then
        assertEquals("I-000001", result);
        verifyDatabaseInteractions(scope, currentValue + 1);
    }

    @Test
    @DisplayName("Should handle large numbers")
    void shouldHandleLargeNumbers() throws SQLException {
        // Given
        String scope = "RECEIPT";
        long currentValue = 999999L;
        setupMocksForSuccessfulGeneration(scope, currentValue);

        // When
        String result = billNumberService.next(scope);

        // Then
        assertEquals("R-999999", result);
        verifyDatabaseInteractions(scope, currentValue + 1);
    }

    @Test
    @DisplayName("Should throw exception when scope not found")
    void shouldThrowExceptionWhenScopeNotFound() throws SQLException {
        // Given
        String scope = "UNKNOWN";
        when(tx.inTx(any())).thenAnswer(invocation -> {
            Function<Connection, String> function = invocation.getArgument(0);
            when(connection.prepareStatement(anyString())).thenReturn(selectStatement);
            when(selectStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No scope found

            return function.apply(connection);
        });

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> billNumberService.next(scope));

        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Unknown scope: " + scope, exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should wrap SQL exceptions in RuntimeException")
    void shouldWrapSqlExceptionsInRuntimeException() throws SQLException {
        // Given
        String scope = "ERROR";
        when(tx.inTx(any())).thenAnswer(invocation -> {
            Function<Connection, String> function = invocation.getArgument(0);
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

            return function.apply(connection);
        });

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> billNumberService.next(scope));

        assertTrue(exception.getCause() instanceof SQLException);
        assertEquals("Database error", exception.getCause().getMessage());
    }

    private void setupMocksForSuccessfulGeneration(String scope, long currentValue) throws SQLException {
        when(tx.inTx(any())).thenAnswer(invocation -> {
            Function<Connection, String> function = invocation.getArgument(0);

            // Mock SELECT statement
            when(connection.prepareStatement(contains("SELECT next_val FROM bill_number")))
                .thenReturn(selectStatement);
            when(selectStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(currentValue);

            // Mock UPDATE statement
            when(connection.prepareStatement(contains("UPDATE bill_number")))
                .thenReturn(updateStatement);
            when(updateStatement.executeUpdate()).thenReturn(1);

            return function.apply(connection);
        });
    }

    private void verifyDatabaseInteractions(String scope, long nextValue) throws SQLException {
        verify(tx).inTx(any());
        // Additional verifications would be done through the Tx mock
    }
}
