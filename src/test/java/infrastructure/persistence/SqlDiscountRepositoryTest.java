package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.SqlDiscountRepository;
import domain.pricing.Discount;
import domain.pricing.Discount.DiscountType;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@DisplayName("SqlDiscountRepository Tests")
class SqlDiscountRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private SqlDiscountRepository discountRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        discountRepository = new SqlDiscountRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Save Discount Tests")
    class SaveDiscountTests {

        @Test
        @DisplayName("Should save new discount successfully")
        void shouldSaveNewDiscountSuccessfully() throws SQLException {
            // Given
            Discount discount = new Discount(0L, 100L, DiscountType.PERCENTAGE, new BigDecimal("15.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "15% off promotion", 1L);
            long generatedId = 123L;

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(generatedId);

            // When
            Discount result = discountRepository.save(discount);

            // Then
            assertNotNull(result);
            assertEquals(generatedId, result.getId());
            assertEquals(discount.getBatchId(), result.getBatchId());
            assertEquals(discount.getType(), result.getType());
            assertEquals(discount.getValue(), result.getValue());

            verify(connection).prepareStatement(contains("INSERT INTO discounts"), eq(Statement.RETURN_GENERATED_KEYS));
            verify(preparedStatement).setLong(1, 100L);
            verify(preparedStatement).setString(2, "PERCENTAGE");
            verify(preparedStatement).setBigDecimal(3, new BigDecimal("15.00"));
            verify(preparedStatement).setDate(4, Date.valueOf(discount.getStartDate()));
            verify(preparedStatement).setDate(5, Date.valueOf(discount.getEndDate()));
            verify(preparedStatement).setBoolean(6, true);
            verify(preparedStatement).setString(7, "15% off promotion");
            verify(preparedStatement).setLong(8, 1L);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should save fixed amount discount")
        void shouldSaveFixedAmountDiscount() throws SQLException {
            // Given
            Discount discount = new Discount(0L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("25.00"),
                LocalDate.now(), LocalDate.now().plusDays(15), true, "$25 off", 2L);

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(124L);

            // When
            Discount result = discountRepository.save(discount);

            // Then
            assertEquals(DiscountType.FIXED_AMOUNT, result.getType());
            verify(preparedStatement).setString(2, "FIXED_AMOUNT");
            verify(preparedStatement).setBigDecimal(3, new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("Should handle null description")
        void shouldHandleNullDescription() throws SQLException {
            // Given
            Discount discount = new Discount(0L, 102L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, null, 1L);

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(125L);

            // When
            discountRepository.save(discount);

            // Then
            verify(preparedStatement).setString(7, null);
        }

        @Test
        @DisplayName("Should throw exception when no generated keys returned")
        void shouldThrowExceptionWhenNoGeneratedKeysReturned() throws SQLException {
            // Given
            Discount discount = createTestDiscount();
            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discountRepository.save(discount));
            assertEquals("Failed to get generated discount ID", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during save")
        void shouldHandleSQLExceptionsDuringSave() throws SQLException {
            // Given
            Discount discount = createTestDiscount();
            SQLException sqlException = new SQLException("Insert failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discountRepository.save(discount));
            assertTrue(exception.getMessage().contains("Failed to save discount"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Update Discount Tests")
    class UpdateDiscountTests {

        @Test
        @DisplayName("Should update discount successfully")
        void shouldUpdateDiscountSuccessfully() throws SQLException {
            // Given
            Discount discount = new Discount(123L, 100L, DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                LocalDate.now(), LocalDate.now().plusDays(45), false, "Updated discount", 1L);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // When
            Discount result = discountRepository.update(discount);

            // Then
            assertEquals(discount, result);
            verify(connection).prepareStatement(contains("UPDATE discounts"));
            verify(preparedStatement).setString(1, "PERCENTAGE");
            verify(preparedStatement).setBigDecimal(2, new BigDecimal("20.00"));
            verify(preparedStatement).setBoolean(5, false);
            verify(preparedStatement).setString(6, "Updated discount");
            verify(preparedStatement).setLong(7, 123L);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should throw exception when discount not found for update")
        void shouldThrowExceptionWhenDiscountNotFoundForUpdate() throws SQLException {
            // Given
            Discount discount = createTestDiscount();
            when(preparedStatement.executeUpdate()).thenReturn(0); // No rows updated

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discountRepository.update(discount));
            assertTrue(exception.getMessage().contains("Discount not found for update"));
        }

        @Test
        @DisplayName("Should handle SQL exceptions during update")
        void shouldHandleSQLExceptionsDuringUpdate() throws SQLException {
            // Given
            Discount discount = createTestDiscount();
            SQLException sqlException = new SQLException("Update failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discountRepository.update(discount));
            assertTrue(exception.getMessage().contains("Failed to update discount"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find discount by ID successfully")
        void shouldFindDiscountByIdSuccessfully() throws SQLException {
            // Given
            long discountId = 123L;
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // When
            Optional<Discount> result = discountRepository.findById(discountId);

            // Then
            assertTrue(result.isPresent());
            Discount discount = result.get();
            assertEquals(123L, discount.getId());
            assertEquals(DiscountType.PERCENTAGE, discount.getType());
            verify(preparedStatement).setLong(1, discountId);
        }

        @Test
        @DisplayName("Should return empty when discount not found by ID")
        void shouldReturnEmptyWhenDiscountNotFoundById() throws SQLException {
            // Given
            long discountId = 999L;
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            Optional<Discount> result = discountRepository.findById(discountId);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find by ID")
        void shouldHandleSQLExceptionsDuringFindById() throws SQLException {
            // Given
            long discountId = 123L;
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discountRepository.findById(discountId));
            assertTrue(exception.getMessage().contains("Failed to find discount by ID"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Find By Batch ID Tests")
    class FindByBatchIdTests {

        @Test
        @DisplayName("Should find discounts by batch ID")
        void shouldFindDiscountsByBatchId() throws SQLException {
            // Given
            long batchId = 100L;
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // When
            List<Discount> result = discountRepository.findByBatchId(batchId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(preparedStatement).setLong(1, batchId);
            verify(connection).prepareStatement(contains("WHERE batch_id = ?"));
        }

        @Test
        @DisplayName("Should return empty list when no discounts for batch")
        void shouldReturnEmptyListWhenNoDiscountsForBatch() throws SQLException {
            // Given
            long batchId = 999L;
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<Discount> result = discountRepository.findByBatchId(batchId);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find Active Discounts Tests")
    class FindActiveDiscountsTests {

        @Test
        @DisplayName("Should find active discounts for batch on specific date")
        void shouldFindActiveDiscountsForBatchOnSpecificDate() throws SQLException {
            // Given
            long batchId = 100L;
            LocalDate date = LocalDate.now();
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<Discount> result = discountRepository.findActiveDiscountsForBatch(batchId, date);

            // Then
            assertEquals(1, result.size());
            verify(preparedStatement).setLong(1, batchId);
            verify(preparedStatement).setDate(2, Date.valueOf(date));
            verify(preparedStatement).setDate(3, Date.valueOf(date));
            verify(connection).prepareStatement(contains("is_active = true"));
        }

        @Test
        @DisplayName("Should find active discounts in date range")
        void shouldFindActiveDiscountsInDateRange() throws SQLException {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(30);
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<Discount> result = discountRepository.findActiveDiscountsInDateRange(startDate, endDate);

            // Then
            assertEquals(1, result.size());
            verify(preparedStatement).setDate(1, Date.valueOf(endDate));
            verify(preparedStatement).setDate(2, Date.valueOf(startDate));
        }
    }

    @Nested
    @DisplayName("Delete Discount Tests")
    class DeleteDiscountTests {

        @Test
        @DisplayName("Should delete discount successfully")
        void shouldDeleteDiscountSuccessfully() throws SQLException {
            // Given
            long discountId = 123L;
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // When
            boolean result = discountRepository.delete(discountId);

            // Then
            assertTrue(result);
            verify(connection).prepareStatement("DELETE FROM discounts WHERE id = ?");
            verify(preparedStatement).setLong(1, discountId);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should return false when discount not found for delete")
        void shouldReturnFalseWhenDiscountNotFoundForDelete() throws SQLException {
            // Given
            long discountId = 999L;
            when(preparedStatement.executeUpdate()).thenReturn(0);

            // When
            boolean result = discountRepository.delete(discountId);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during delete")
        void shouldHandleSQLExceptionsDuringDelete() throws SQLException {
            // Given
            long discountId = 123L;
            SQLException sqlException = new SQLException("Delete failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discountRepository.delete(discountId));
            assertTrue(exception.getMessage().contains("Failed to delete discount"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Find All Discounts Tests")
    class FindAllDiscountsTests {

        @Test
        @DisplayName("Should find all discounts")
        void shouldFindAllDiscounts() throws SQLException {
            // Given
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // When
            List<Discount> result = discountRepository.findAll();

            // Then
            assertEquals(2, result.size());
            verify(connection).prepareStatement(contains("ORDER BY created_at DESC"));
        }

        @Test
        @DisplayName("Should return empty list when no discounts exist")
        void shouldReturnEmptyListWhenNoDiscountsExist() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<Discount> result = discountRepository.findAll();

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Product Code Tests")
    class FindByProductCodeTests {

        @Test
        @DisplayName("Should find discounts by product code")
        void shouldFindDiscountsByProductCode() throws SQLException {
            // Given
            String productCode = "PROD001";
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            // When
            List<Discount> result = discountRepository.findByProductCode(productCode);

            // Then
            assertEquals(1, result.size());
            verify(preparedStatement).setString(1, productCode);
            verify(connection).prepareStatement(contains("JOIN batch b ON d.batch_id = b.id"));
        }

        @Test
        @DisplayName("Should return empty list when no discounts for product")
        void shouldReturnEmptyListWhenNoDiscountsForProduct() throws SQLException {
            // Given
            String productCode = "NONEXISTENT";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<Discount> result = discountRepository.findByProductCode(productCode);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create SqlDiscountRepository with DataSource")
        void shouldCreateSqlDiscountRepositoryWithDataSource() {
            // When
            SqlDiscountRepository repository = new SqlDiscountRepository(dataSource);

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
            long discountId = 123L;
            setupMockResultSetForDiscount();
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // When
            discountRepository.findById(discountId);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            long discountId = 123L;
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> discountRepository.findById(discountId));
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large discount values")
        void shouldHandleVeryLargeDiscountValues() throws SQLException {
            // Given
            Discount discount = new Discount(0L, 100L, DiscountType.FIXED_AMOUNT, new BigDecimal("999999.99"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Huge discount", 1L);

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(126L);

            // When
            discountRepository.save(discount);

            // Then
            verify(preparedStatement).setBigDecimal(3, new BigDecimal("999999.99"));
        }

        @Test
        @DisplayName("Should handle zero discount values")
        void shouldHandleZeroDiscountValues() throws SQLException {
            // Given
            Discount discount = new Discount(0L, 100L, DiscountType.PERCENTAGE, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Zero discount", 1L);

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(127L);

            // When
            discountRepository.save(discount);

            // Then
            verify(preparedStatement).setBigDecimal(3, BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle very long descriptions")
        void shouldHandleVeryLongDescriptions() throws SQLException {
            // Given
            String longDescription = "This is a very detailed discount description that includes comprehensive information about the promotion. ".repeat(20);
            Discount discount = new Discount(0L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, longDescription, 1L);

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(128L);

            // When
            discountRepository.save(discount);

            // Then
            verify(preparedStatement).setString(7, longDescription);
        }
    }

    // Helper methods
    private Discount createTestDiscount() {
        return new Discount(123L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
            LocalDate.now(), LocalDate.now().plusDays(30), true, "Test discount", 1L);
    }

    private void setupMockResultSetForDiscount() throws SQLException {
        when(resultSet.getLong("id")).thenReturn(123L);
        when(resultSet.getLong("batch_id")).thenReturn(100L);
        when(resultSet.getString("discount_type")).thenReturn("PERCENTAGE");
        when(resultSet.getBigDecimal("discount_value")).thenReturn(new BigDecimal("10.00"));
        when(resultSet.getDate("start_date")).thenReturn(Date.valueOf(LocalDate.now()));
        when(resultSet.getDate("end_date")).thenReturn(Date.valueOf(LocalDate.now().plusDays(30)));
        when(resultSet.getBoolean("is_active")).thenReturn(true);
        when(resultSet.getString("description")).thenReturn("Test discount");
        when(resultSet.getLong("created_by")).thenReturn(1L);
    }
}
