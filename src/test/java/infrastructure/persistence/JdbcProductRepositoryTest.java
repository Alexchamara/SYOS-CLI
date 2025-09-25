package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcProductRepository;
import domain.product.Product;
import domain.shared.Code;
import domain.shared.Money;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@DisplayName("JdbcProductRepository Tests")
class JdbcProductRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcProductRepository productRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        productRepository = new JdbcProductRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Upsert Product Tests")
    class UpsertProductTests {

        @Test
        @DisplayName("Should upsert product successfully")
        void shouldUpsertProductSuccessfully() throws SQLException {
            Product product = new Product(new Code("PROD001"), "Test Product", Money.of(new BigDecimal("99.99")));

            productRepository.upsert(product);

            verify(connection).prepareStatement(contains("INSERT INTO product"));
            verify(preparedStatement).setString(1, "PROD001");
            verify(preparedStatement).setString(2, "Test Product");
            verify(preparedStatement).setLong(3, 9999L);
            verify(preparedStatement).setString(4, null);
            verify(preparedStatement).executeUpdate();
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should upsert product with category code")
        void shouldUpsertProductWithCategoryCode() throws SQLException {
            // Given
            Product product = new Product(new Code("PROD001"), "Test Product", Money.of(new BigDecimal("50.00")), "ELEC");

            // When
            productRepository.upsert(product);

            // Then
            verify(preparedStatement).setString(1, "PROD001");
            verify(preparedStatement).setString(2, "Test Product");
            verify(preparedStatement).setLong(3, 5000L); // 50.00 * 100 = 5000 cents
            verify(preparedStatement).setString(4, "ELEC");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle SQL exceptions during upsert")
        void shouldHandleSQLExceptionsDuringUpsert() throws SQLException {
            // Given
            Product product = new Product(new Code("PROD001"), "Test Product", Money.of(new BigDecimal("25.00")));
            SQLException sqlException = new SQLException("Database constraint violation");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.upsert(product));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle connection failure during upsert")
        void shouldHandleConnectionFailureDuringUpsert() throws SQLException {
            // Given
            Product product = new Product(new Code("PROD001"), "Test Product", Money.of(new BigDecimal("25.00")));
            SQLException connectionException = new SQLException("Connection failed");
            when(dataSource.getConnection()).thenThrow(connectionException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.upsert(product));
            assertEquals(connectionException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Find By Code Tests")
    class FindByCodeTests {

        @Test
        @DisplayName("Should find product by code successfully")
        void shouldFindProductByCodeSuccessfully() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("code")).thenReturn("PROD001");
            when(resultSet.getString("name")).thenReturn("Test Product");
            when(resultSet.getLong("price_cents")).thenReturn(9999L);
            when(resultSet.getString("category_code")).thenReturn("ELEC");

            // When
            Optional<Product> result = productRepository.findByCode(productCode);

            // Then
            assertTrue(result.isPresent());
            Product product = result.get();
            assertEquals("PROD001", product.code().value());
            assertEquals("Test Product", product.name());
            assertEquals(Money.of(9999L), product.price());
            assertEquals("ELEC", product.categoryCode());

            verify(preparedStatement).setString(1, "PROD001");
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should return empty when product not found")
        void shouldReturnEmptyWhenProductNotFound() throws SQLException {
            // Given
            Code productCode = new Code("NONEXISTENT");
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            Optional<Product> result = productRepository.findByCode(productCode);

            // Then
            assertFalse(result.isPresent());
            verify(preparedStatement).setString(1, "NONEXISTENT");
        }

        @Test
        @DisplayName("Should handle product without category code")
        void shouldHandleProductWithoutCategoryCode() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("code")).thenReturn("PROD001");
            when(resultSet.getString("name")).thenReturn("Product Without Category");
            when(resultSet.getLong("price_cents")).thenReturn(2500L);
            when(resultSet.getString("category_code")).thenReturn(null);

            // When
            Optional<Product> result = productRepository.findByCode(productCode);

            // Then
            assertTrue(result.isPresent());
            Product product = result.get();
            assertNull(product.categoryCode());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find")
        void shouldHandleSQLExceptionsDuringFind() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            SQLException sqlException = new SQLException("Query execution failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.findByCode(productCode));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Find All Products Tests")
    class FindAllProductsTests {

        @Test
        @DisplayName("Should find all products successfully")
        void shouldFindAllProductsSuccessfully() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false); // Two products
            when(resultSet.getString("code")).thenReturn("PROD001", "PROD002");
            when(resultSet.getString("name")).thenReturn("Product 1", "Product 2");
            when(resultSet.getLong("price_cents")).thenReturn(1000L, 2000L);
            when(resultSet.getString("category_code")).thenReturn("ELEC", "FOOD");

            // When
            List<Product> result = productRepository.findAll();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            Product product1 = result.get(0);
            assertEquals("PROD001", product1.code().value());
            assertEquals("Product 1", product1.name());
            assertEquals(Money.of(1000L), product1.price());
            assertEquals("ELEC", product1.categoryCode());

            Product product2 = result.get(1);
            assertEquals("PROD002", product2.code().value());
            assertEquals("Product 2", product2.name());
            assertEquals(Money.of(2000L), product2.price());
            assertEquals("FOOD", product2.categoryCode());

            verify(connection).prepareStatement(contains("ORDER BY code"));
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void shouldReturnEmptyListWhenNoProductsExist() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<Product> result = productRepository.findAll();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find all")
        void shouldHandleSQLExceptionsDuringFindAll() throws SQLException {
            // Given
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.findAll());
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcProductRepository with DataSource")
        void shouldCreateJdbcProductRepositoryWithDataSource() {
            // When
            JdbcProductRepository repository = new JdbcProductRepository(dataSource);

            // Then
            assertNotNull(repository);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources even when work succeeds")
        void shouldCloseResourcesEvenWhenWorkSucceeds() throws SQLException {
            // Given
            Product product = new Product(new Code("PROD001"), "Product", Money.of(new BigDecimal("10.00")));

            // When
            productRepository.upsert(product);

            // Then
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources even when exception occurs")
        void shouldCloseResourcesEvenWhenExceptionOccurs() throws SQLException {
            // Given
            Product product = new Product(new Code("PROD001"), "Product", Money.of(new BigDecimal("10.00")));
            when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> productRepository.upsert(product));
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }
}
