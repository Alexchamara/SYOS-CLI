package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcCartRepository;
import domain.repository.CartRepository.CartItem;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

@DisplayName("JdbcCartRepository Tests")
class JdbcCartRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcCartRepository cartRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        cartRepository = new JdbcCartRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Get Or Create Cart Tests")
    class GetOrCreateCartTests {

        @Test
        @DisplayName("Should return existing cart ID when cart exists")
        void shouldReturnExistingCartIdWhenCartExists() throws SQLException {
            // Given
            long userId = 123L;
            long existingCartId = 456L;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong("id")).thenReturn(existingCartId);

            // When
            long result = cartRepository.getOrCreateCart(userId);

            // Then
            assertEquals(existingCartId, result);
            verify(preparedStatement).setLong(1, userId);
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should create new cart when none exists")
        void shouldCreateNewCartWhenNoneExists() throws SQLException {
            // Given
            long userId = 123L;
            long newCartId = 789L;

            // First query returns no existing cart
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Second statement for cart creation
            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(newCartId);

            // When
            long result = cartRepository.getOrCreateCart(userId);

            // Then
            assertEquals(newCartId, result);
            verify(connection).prepareStatement(contains("SELECT id FROM carts"));
            verify(connection).prepareStatement(contains("INSERT INTO carts"), eq(Statement.RETURN_GENERATED_KEYS));
        }

        @Test
        @DisplayName("Should throw exception when generated keys not available")
        void shouldThrowExceptionWhenGeneratedKeysNotAvailable() throws SQLException {
            // Given
            long userId = 123L;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No existing cart

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No generated keys

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartRepository.getOrCreateCart(userId));
            assertEquals("Failed to get generated cart ID", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during cart creation")
        void shouldHandleSQLExceptionsDuringCartCreation() throws SQLException {
            // Given
            long userId = 123L;
            SQLException sqlException = new SQLException("Database error");
            when(dataSource.getConnection()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartRepository.getOrCreateCart(userId));
            assertTrue(exception.getMessage().contains("Failed to get or create cart"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Upsert Item Tests")
    class UpsertItemTests {

        @Test
        @DisplayName("Should upsert cart item with positive quantity")
        void shouldUpsertCartItemWithPositiveQuantity() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";
            int qty = 5;

            // When
            cartRepository.upsertItem(cartId, productCode, qty);

            // Then
            verify(connection).prepareStatement(contains("INSERT INTO cart_items"));
            verify(preparedStatement).setLong(1, cartId);
            verify(preparedStatement).setString(2, productCode);
            verify(preparedStatement).setInt(3, qty);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should remove item when quantity is zero")
        void shouldRemoveItemWhenQuantityIsZero() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";
            int qty = 0;

            // When
            cartRepository.upsertItem(cartId, productCode, qty);

            // Then
            verify(connection).prepareStatement(contains("DELETE FROM cart_items"));
            verify(preparedStatement).setLong(1, cartId);
            verify(preparedStatement).setString(2, productCode);
        }

        @Test
        @DisplayName("Should remove item when quantity is negative")
        void shouldRemoveItemWhenQuantityIsNegative() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";
            int qty = -5;

            // When
            cartRepository.upsertItem(cartId, productCode, qty);

            // Then
            verify(connection).prepareStatement(contains("DELETE FROM cart_items"));
        }

        @Test
        @DisplayName("Should handle SQL exceptions during upsert")
        void shouldHandleSQLExceptionsDuringUpsert() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";
            int qty = 3;
            SQLException sqlException = new SQLException("Constraint violation");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartRepository.upsertItem(cartId, productCode, qty));
            assertEquals("Failed to upsert cart item", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Remove Item Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should remove item from cart successfully")
        void shouldRemoveItemFromCartSuccessfully() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";

            // When
            cartRepository.removeItem(cartId, productCode);

            // Then
            verify(connection).prepareStatement(contains("DELETE FROM cart_items"));
            verify(preparedStatement).setLong(1, cartId);
            verify(preparedStatement).setString(2, productCode);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle SQL exceptions during remove")
        void shouldHandleSQLExceptionsDuringRemove() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";
            SQLException sqlException = new SQLException("Delete failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartRepository.removeItem(cartId, productCode));
            assertEquals("Failed to remove cart item", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Get Items Tests")
    class GetItemsTests {

        @Test
        @DisplayName("Should get cart items successfully")
        void shouldGetCartItemsSuccessfully() throws SQLException {
            // Given
            long cartId = 456L;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            when(resultSet.getString("product_code")).thenReturn("PROD001", "PROD002");
            when(resultSet.getInt("quantity")).thenReturn(3, 1);

            // When
            List<CartItem> result = cartRepository.items(cartId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            CartItem item1 = result.get(0);
            assertEquals("PROD001", item1.productCode);
            assertEquals(3, item1.qty);

            CartItem item2 = result.get(1);
            assertEquals("PROD002", item2.productCode);
            assertEquals(1, item2.qty);

            verify(preparedStatement).setLong(1, cartId);
        }

        @Test
        @DisplayName("Should return empty list when no items in cart")
        void shouldReturnEmptyListWhenNoItemsInCart() throws SQLException {
            // Given
            long cartId = 456L;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<CartItem> result = cartRepository.items(cartId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during get items")
        void shouldHandleSQLExceptionsDuringGetItems() throws SQLException {
            // Given
            long cartId = 456L;
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartRepository.items(cartId));
            assertEquals("Failed to get cart items", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Clear Cart Tests")
    class ClearCartTests {

        @Test
        @DisplayName("Should clear cart successfully")
        void shouldClearCartSuccessfully() throws SQLException {
            // Given
            long cartId = 456L;

            // When
            cartRepository.clearCart(cartId);

            // Then
            verify(connection).prepareStatement(contains("DELETE FROM cart_items WHERE cart_id"));
            verify(preparedStatement).setLong(1, cartId);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle SQL exceptions during clear")
        void shouldHandleSQLExceptionsDuringClear() throws SQLException {
            // Given
            long cartId = 456L;
            SQLException sqlException = new SQLException("Clear operation failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartRepository.clearCart(cartId));
            assertEquals("Failed to clear cart", exception.getMessage());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcCartRepository with DataSource")
        void shouldCreateJdbcCartRepositoryWithDataSource() {
            // When
            JdbcCartRepository repository = new JdbcCartRepository(dataSource);

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
            long cartId = 456L;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            cartRepository.items(cartId);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            long cartId = 456L;
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> cartRepository.items(cartId));
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large cart IDs")
        void shouldHandleVeryLargeCartIds() throws SQLException {
            // Given
            long largeCartId = Long.MAX_VALUE;
            String productCode = "PROD001";
            int qty = 1;

            // When
            cartRepository.upsertItem(largeCartId, productCode, qty);

            // Then
            verify(preparedStatement).setLong(1, largeCartId);
        }

        @Test
        @DisplayName("Should handle very large quantities")
        void shouldHandleVeryLargeQuantities() throws SQLException {
            // Given
            long cartId = 456L;
            String productCode = "PROD001";
            int largeQty = Integer.MAX_VALUE;

            // When
            cartRepository.upsertItem(cartId, productCode, largeQty);

            // Then
            verify(preparedStatement).setInt(3, largeQty);
        }

        @Test
        @DisplayName("Should handle empty product codes")
        void shouldHandleEmptyProductCodes() throws SQLException {
            // Given
            long cartId = 456L;
            String emptyProductCode = "";
            int qty = 1;

            // When
            cartRepository.upsertItem(cartId, emptyProductCode, qty);

            // Then
            verify(preparedStatement).setString(2, emptyProductCode);
        }
    }
}
