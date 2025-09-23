package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcPaymentRepository;

import javax.sql.DataSource;
import java.sql.*;

@DisplayName("JdbcPaymentRepository Tests")
class JdbcPaymentRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    private JdbcPaymentRepository paymentRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        paymentRepository = new JdbcPaymentRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Save Card Payment Tests")
    class SaveCardPaymentTests {

        @Test
        @DisplayName("Should save card payment successfully")
        void shouldSaveCardPaymentSuccessfully() throws SQLException {
            // Given
            long orderId = 123L;
            String last4 = "1234";
            String authRef = "AUTH123456789";

            // When
            paymentRepository.saveCard(orderId, last4, authRef);

            // Then
            verify(connection).prepareStatement(contains("INSERT INTO payments"));
            verify(preparedStatement).setLong(1, orderId);
            verify(preparedStatement).setString(2, last4);
            verify(preparedStatement).setString(3, authRef);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle different last4 formats")
        void shouldHandleDifferentLast4Formats() throws SQLException {
            // Given
            long orderId = 124L;
            String[] last4Values = {"0000", "9999", "5678", "****"};

            // When & Then
            for (String last4 : last4Values) {
                paymentRepository.saveCard(orderId, last4, "AUTH_REF");
                verify(preparedStatement).setString(2, last4);
            }
        }

        @Test
        @DisplayName("Should handle different authorization references")
        void shouldHandleDifferentAuthorizationReferences() throws SQLException {
            // Given
            long orderId = 125L;
            String last4 = "1234";
            String[] authRefs = {"AUTH123", "TXN_987654321", "REF-ABC-123", ""};

            // When & Then
            for (String authRef : authRefs) {
                paymentRepository.saveCard(orderId, last4, authRef);
                verify(preparedStatement).setString(3, authRef);
            }
        }

        @Test
        @DisplayName("Should handle null last4")
        void shouldHandleNullLast4() throws SQLException {
            // Given
            long orderId = 126L;
            String last4 = null;
            String authRef = "AUTH789";

            // When
            paymentRepository.saveCard(orderId, last4, authRef);

            // Then
            verify(preparedStatement).setString(2, null);
            verify(preparedStatement).setString(3, authRef);
        }

        @Test
        @DisplayName("Should handle null authorization reference")
        void shouldHandleNullAuthorizationReference() throws SQLException {
            // Given
            long orderId = 127L;
            String last4 = "5678";
            String authRef = null;

            // When
            paymentRepository.saveCard(orderId, last4, authRef);

            // Then
            verify(preparedStatement).setString(2, last4);
            verify(preparedStatement).setString(3, null);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during save card")
        void shouldHandleSQLExceptionsDuringSaveCard() throws SQLException {
            // Given
            long orderId = 128L;
            String last4 = "9876";
            String authRef = "AUTH456";
            SQLException sqlException = new SQLException("Payment insert failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentRepository.saveCard(orderId, last4, authRef));
            assertEquals("Failed to save card payment", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle very large order IDs")
        void shouldHandleVeryLargeOrderIds() throws SQLException {
            // Given
            long largeOrderId = Long.MAX_VALUE;
            String last4 = "1111";
            String authRef = "AUTH_LARGE";

            // When
            paymentRepository.saveCard(largeOrderId, last4, authRef);

            // Then
            verify(preparedStatement).setLong(1, largeOrderId);
        }

        @Test
        @DisplayName("Should handle negative order IDs")
        void shouldHandleNegativeOrderIds() throws SQLException {
            // Given
            long negativeOrderId = -1L;
            String last4 = "2222";
            String authRef = "AUTH_NEG";

            // When
            paymentRepository.saveCard(negativeOrderId, last4, authRef);

            // Then
            verify(preparedStatement).setLong(1, negativeOrderId);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcPaymentRepository with DataSource")
        void shouldCreateJdbcPaymentRepositoryWithDataSource() {
            // When
            JdbcPaymentRepository repository = new JdbcPaymentRepository(dataSource);

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
            long orderId = 129L;
            String last4 = "3333";
            String authRef = "AUTH999";

            // When
            paymentRepository.saveCard(orderId, last4, authRef);

            // Then
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            long orderId = 130L;
            String last4 = "4444";
            String authRef = "AUTH000";
            when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> paymentRepository.saveCard(orderId, last4, authRef));
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty string values")
        void shouldHandleEmptyStringValues() throws SQLException {
            // Given
            long orderId = 131L;
            String last4 = "";
            String authRef = "";

            // When
            paymentRepository.saveCard(orderId, last4, authRef);

            // Then
            verify(preparedStatement).setString(2, "");
            verify(preparedStatement).setString(3, "");
        }

        @Test
        @DisplayName("Should handle very long authorization references")
        void shouldHandleVeryLongAuthorizationReferences() throws SQLException {
            // Given
            long orderId = 132L;
            String last4 = "5555";
            String longAuthRef = "AUTHORIZATION-REFERENCE-" + "X".repeat(200);

            // When
            paymentRepository.saveCard(orderId, last4, longAuthRef);

            // Then
            verify(preparedStatement).setString(3, longAuthRef);
        }

        @Test
        @DisplayName("Should handle special characters in authorization reference")
        void shouldHandleSpecialCharactersInAuthorizationReference() throws SQLException {
            // Given
            long orderId = 133L;
            String last4 = "6666";
            String specialAuthRef = "AUTH-123@#$%^&*()_+{}|:<>?";

            // When
            paymentRepository.saveCard(orderId, last4, specialAuthRef);

            // Then
            verify(preparedStatement).setString(3, specialAuthRef);
        }
    }
}
