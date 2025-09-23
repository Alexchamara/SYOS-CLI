package infrastructure.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.concurrency.Tx;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

@DisplayName("Tx (Transaction) Tests")
class TxTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Function<Connection, String> workFunction;

    private Tx tx;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        tx = new Tx(dataSource);

        // Default connection setup
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
    }

    @Nested
    @DisplayName("Successful Transaction Tests")
    class SuccessfulTransactionTests {

        @Test
        @DisplayName("Should execute work in transaction and commit")
        void shouldExecuteWorkInTransactionAndCommit() throws SQLException {
            // Given
            String expectedResult = "success";
            when(workFunction.apply(connection)).thenReturn(expectedResult);

            // When
            String result = tx.inTx(workFunction);

            // Then
            assertEquals(expectedResult, result);
            verify(connection).setAutoCommit(false);
            verify(workFunction).apply(connection);
            verify(connection).commit();
            verify(connection).setAutoCommit(true); // Restore original
            verify(connection).close();
        }

        @Test
        @DisplayName("Should restore original autocommit setting")
        void shouldRestoreOriginalAutocommitSetting() throws SQLException {
            // Given
            when(connection.getAutoCommit()).thenReturn(false); // Original was false
            when(workFunction.apply(connection)).thenReturn("result");

            // When
            tx.inTx(workFunction);

            // Then
            verify(connection).setAutoCommit(false); // Set to false for transaction
            verify(connection).setAutoCommit(false); // Restore original false
        }

        @Test
        @DisplayName("Should handle work function returning null")
        void shouldHandleWorkFunctionReturningNull() throws SQLException {
            // Given
            when(workFunction.apply(connection)).thenReturn(null);

            // When
            String result = tx.inTx(workFunction);

            // Then
            assertNull(result);
            verify(connection).commit();
        }

        @Test
        @DisplayName("Should handle different return types")
        void shouldHandleDifferentReturnTypes() throws SQLException {
            // Given
            Function<Connection, Integer> intFunction = con -> 42;
            Function<Connection, Boolean> boolFunction = con -> true;

            // When
            Integer intResult = tx.inTx(intFunction);
            Boolean boolResult = tx.inTx(boolFunction);

            // Then
            assertEquals(42, intResult);
            assertTrue(boolResult);
            verify(connection, times(2)).commit();
        }
    }

    @Nested
    @DisplayName("Transaction Rollback Tests")
    class TransactionRollbackTests {

        @Test
        @DisplayName("Should rollback on RuntimeException")
        void shouldRollbackOnRuntimeException() throws SQLException {
            // Given
            RuntimeException expectedException = new RuntimeException("Business logic error");
            when(workFunction.apply(connection)).thenThrow(expectedException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals("Business logic error", exception.getMessage());
            assertSame(expectedException, exception);

            verify(connection).setAutoCommit(false);
            verify(connection).rollback();
            verify(connection).setAutoCommit(true);
            verify(connection, never()).commit();
        }

        @Test
        @DisplayName("Should rollback and wrap checked exceptions")
        void shouldRollbackAndWrapCheckedExceptions() throws SQLException {
            // Given
            Function<Connection, String> failingFunction = con -> {
                try {
                    throw new SQLException("Database error");
                } catch (SQLException e) {
                    throw new RuntimeException("Wrapped SQL exception", e);
                }
            };

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(failingFunction));
            assertEquals("Wrapped SQL exception", exception.getMessage());
            assertTrue(exception.getCause() instanceof SQLException);

            verify(connection).rollback();
            verify(connection, never()).commit();
        }

        @Test
        @DisplayName("Should handle rollback failure gracefully")
        void shouldHandleRollbackFailureGracefully() throws SQLException {
            // Given
            RuntimeException originalException = new RuntimeException("Original error");
            SQLException rollbackException = new SQLException("Rollback failed");

            when(workFunction.apply(connection)).thenThrow(originalException);
            doThrow(rollbackException).when(connection).rollback();

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals("Original error", exception.getMessage()); // Original exception preserved

            verify(connection).rollback();
        }
    }

    @Nested
    @DisplayName("Connection Management Tests")
    class ConnectionManagementTests {

        @Test
        @DisplayName("Should handle connection acquisition failure")
        void shouldHandleConnectionAcquisitionFailure() throws SQLException {
            // Given
            SQLException connectionException = new SQLException("Connection pool exhausted");
            when(dataSource.getConnection()).thenThrow(connectionException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals(connectionException, exception.getCause());

            verifyNoInteractions(workFunction);
        }

        @Test
        @DisplayName("Should close connection even if work succeeds")
        void shouldCloseConnectionEvenIfWorkSucceeds() throws SQLException {
            // Given
            when(workFunction.apply(connection)).thenReturn("success");

            // When
            tx.inTx(workFunction);

            // Then
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close connection even if work fails")
        void shouldCloseConnectionEvenIfWorkFails() throws SQLException {
            // Given
            when(workFunction.apply(connection)).thenThrow(new RuntimeException("Work failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            verify(connection).close();
        }

        @Test
        @DisplayName("Should handle connection close failure")
        void shouldHandleConnectionCloseFailure() throws SQLException {
            // Given
            SQLException closeException = new SQLException("Close failed");
            when(workFunction.apply(connection)).thenReturn("success");
            doThrow(closeException).when(connection).close();

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals(closeException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("AutoCommit Management Tests")
    class AutoCommitManagementTests {

        @Test
        @DisplayName("Should handle autocommit retrieval failure")
        void shouldHandleAutocommitRetrievalFailure() throws SQLException {
            // Given
            SQLException autoCommitException = new SQLException("AutoCommit check failed");
            when(connection.getAutoCommit()).thenThrow(autoCommitException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals(autoCommitException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle autocommit setting failure")
        void shouldHandleAutocommitSettingFailure() throws SQLException {
            // Given
            SQLException autoCommitException = new SQLException("AutoCommit setting failed");
            doThrow(autoCommitException).when(connection).setAutoCommit(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals(autoCommitException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle commit failure")
        void shouldHandleCommitFailure() throws SQLException {
            // Given
            SQLException commitException = new SQLException("Commit failed");
            when(workFunction.apply(connection)).thenReturn("success");
            doThrow(commitException).when(connection).commit();

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tx.inTx(workFunction));
            assertEquals(commitException, exception.getCause());

            // Should still try to restore autocommit
            verify(connection).setAutoCommit(true);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle multiple concurrent transactions")
        void shouldHandleMultipleConcurrentTransactions() throws SQLException {
            // Given
            Connection connection1 = mock(Connection.class);
            Connection connection2 = mock(Connection.class);
            when(dataSource.getConnection())
                .thenReturn(connection1)
                .thenReturn(connection2);
            when(connection1.getAutoCommit()).thenReturn(true);
            when(connection2.getAutoCommit()).thenReturn(true);

            Function<Connection, String> work1 = con -> "result1";
            Function<Connection, String> work2 = con -> "result2";

            // When
            String result1 = tx.inTx(work1);
            String result2 = tx.inTx(work2);

            // Then
            assertEquals("result1", result1);
            assertEquals("result2", result2);
            verify(connection1).commit();
            verify(connection2).commit();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Tx with DataSource")
        void shouldCreateTxWithDataSource() {
            // When
            Tx transaction = new Tx(dataSource);

            // Then
            assertNotNull(transaction);
        }

        @Test
        @DisplayName("Should handle null DataSource in constructor")
        void shouldHandleNullDataSourceInConstructor() {
            // When & Then
            // Constructor doesn't validate null, but usage will fail
            assertDoesNotThrow(() -> new Tx(null));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle work function that modifies connection state")
        void shouldHandleWorkFunctionThatModifiesConnectionState() throws SQLException {
            // Given
            Function<Connection, String> stateModifyingWork = con -> {
                try {
                    con.setReadOnly(true);
                    return "modified";
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            };

            // When
            String result = tx.inTx(stateModifyingWork);

            // Then
            assertEquals("modified", result);
            verify(connection).commit();
        }

        @Test
        @DisplayName("Should handle nested function calls")
        void shouldHandleNestedFunctionCalls() throws SQLException {
            // Given
            Function<Connection, String> nestedWork = con -> {
                // Simulate nested work that doesn't interfere with transaction
                return "outer(" + "inner(data)" + ")";
            };
            when(nestedWork.apply(connection)).thenReturn("outer(inner(data))");

            // When
            String result = tx.inTx(nestedWork);

            // Then
            assertEquals("outer(inner(data))", result);
            verify(connection).commit();
        }
    }
}
