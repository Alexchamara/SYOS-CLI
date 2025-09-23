package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcInventoryRepository;
import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.shared.Code;
import domain.shared.Quantity;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("JdbcInventoryRepository Tests")
class JdbcInventoryRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcInventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        inventoryRepository = new JdbcInventoryRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Find Deduction Candidates Tests")
    class FindDeductionCandidatesTests {

        @Test
        @DisplayName("Should find deduction candidates ordered by FEFO")
        void shouldFindDeductionCandidatesOrderedByFEFO() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            StockLocation location = StockLocation.MAIN_STORE;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            // First batch (earlier expiry)
            when(resultSet.getLong("id")).thenReturn(1L, 2L);
            when(resultSet.getString("product_code")).thenReturn("PROD001", "PROD001");
            when(resultSet.getString("location")).thenReturn("MAIN_STORE", "MAIN_STORE");
            when(resultSet.getTimestamp("received_at"))
                .thenReturn(Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                           Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            when(resultSet.getDate("expiry"))
                .thenReturn(Date.valueOf(LocalDate.now().plusDays(10)),
                           Date.valueOf(LocalDate.now().plusDays(20)));
            when(resultSet.getInt("quantity")).thenReturn(50, 75);

            // When
            List<Batch> result = inventoryRepository.findDeductionCandidates(connection, productCode, location);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            verify(preparedStatement).setString(1, "PROD001");
            verify(preparedStatement).setString(2, "MAIN_STORE");
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should return empty list when no candidates found")
        void shouldReturnEmptyListWhenNoCandidatesFound() throws SQLException {
            // Given
            Code productCode = new Code("NONEXISTENT");
            StockLocation location = StockLocation.SHELF;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<Batch> result = inventoryRepository.findDeductionCandidates(connection, productCode, location);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle batches with null expiry dates")
        void shouldHandleBatchesWithNullExpiryDates() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            StockLocation location = StockLocation.WEB;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getLong("id")).thenReturn(1L);
            when(resultSet.getString("product_code")).thenReturn("PROD001");
            when(resultSet.getString("location")).thenReturn("WEB");
            when(resultSet.getTimestamp("received_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getDate("expiry")).thenReturn(null); // No expiry
            when(resultSet.getInt("quantity")).thenReturn(100);

            // When
            List<Batch> result = inventoryRepository.findDeductionCandidates(connection, productCode, location);

            // Then
            assertEquals(1, result.size());
            Batch batch = result.get(0);
            assertNull(batch.expiry());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find candidates")
        void shouldHandleSQLExceptionsDuringFindCandidates() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            StockLocation location = StockLocation.SHELF;
            SQLException sqlException = new SQLException("Query execution failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.findDeductionCandidates(connection, productCode, location));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Deduct From Batch Tests")
    class DeductFromBatchTests {

        @Test
        @DisplayName("Should deduct from batch successfully")
        void shouldDeductFromBatchSuccessfully() throws SQLException {
            // Given
            long batchId = 123L;
            int takeQuantity = 25;
            when(preparedStatement.executeUpdate()).thenReturn(1); // One row updated

            // When
            inventoryRepository.deductFromBatch(connection, batchId, takeQuantity);

            // Then
            verify(preparedStatement).setInt(1, takeQuantity);
            verify(preparedStatement).setLong(2, batchId);
            verify(preparedStatement).setInt(3, takeQuantity);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should throw exception when no rows updated")
        void shouldThrowExceptionWhenNoRowsUpdated() throws SQLException {
            // Given
            long batchId = 123L;
            int takeQuantity = 25;
            when(preparedStatement.executeUpdate()).thenReturn(0); // No rows updated

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> inventoryRepository.deductFromBatch(connection, batchId, takeQuantity));
            assertTrue(exception.getMessage().contains("Concurrent update or insufficient qty"));
            assertTrue(exception.getMessage().contains("batch 123"));
        }

        @Test
        @DisplayName("Should handle SQL exceptions during deduction")
        void shouldHandleSQLExceptionsDuringDeduction() throws SQLException {
            // Given
            long batchId = 123L;
            int takeQuantity = 25;
            SQLException sqlException = new SQLException("Update failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.deductFromBatch(connection, batchId, takeQuantity));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle zero quantity deduction")
        void shouldHandleZeroQuantityDeduction() throws SQLException {
            // Given
            long batchId = 123L;
            int takeQuantity = 0;
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // When
            inventoryRepository.deductFromBatch(connection, batchId, takeQuantity);

            // Then
            verify(preparedStatement).setInt(1, 0);
            verify(preparedStatement).setLong(2, batchId);
            verify(preparedStatement).setInt(3, 0);
        }

        @Test
        @DisplayName("Should handle large quantity deduction")
        void shouldHandleLargeQuantityDeduction() throws SQLException {
            // Given
            long batchId = 123L;
            int takeQuantity = Integer.MAX_VALUE;
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // When
            inventoryRepository.deductFromBatch(connection, batchId, takeQuantity);

            // Then
            verify(preparedStatement).setInt(1, Integer.MAX_VALUE);
            verify(preparedStatement).setInt(3, Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("Total Available Tests")
    class TotalAvailableTests {

        @Test
        @DisplayName("Should return total available quantity")
        void shouldReturnTotalAvailableQuantity() throws SQLException {
            // Given
            String productCode = "PROD001";
            String location = "MAIN_STORE";
            int expectedQuantity = 150;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("q")).thenReturn(expectedQuantity);

            // When
            int result = inventoryRepository.totalAvailable(connection, productCode, location);

            // Then
            assertEquals(expectedQuantity, result);
            verify(preparedStatement).setString(1, productCode);
            verify(preparedStatement).setString(2, location);
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should return zero when no stock available")
        void shouldReturnZeroWhenNoStockAvailable() throws SQLException {
            // Given
            String productCode = "PROD002";
            String location = "SHELF";

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("q")).thenReturn(0);

            // When
            int result = inventoryRepository.totalAvailable(connection, productCode, location);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during total available check")
        void shouldHandleSQLExceptionsDuringTotalAvailableCheck() throws SQLException {
            // Given
            String productCode = "PROD001";
            String location = "WEB";
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.totalAvailable(connection, productCode, location));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Transfer Stock Tests")
    class TransferStockTests {

        @Test
        @DisplayName("Should transfer stock between locations")
        void shouldTransferStockBetweenLocations() throws SQLException {
            // Given
            String productCode = "PROD001";
            StockLocation fromLocation = StockLocation.MAIN_STORE;
            StockLocation toLocation = StockLocation.SHELF;
            int quantity = 50;

            // Mock the deduction candidates query
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false); // One batch available
            when(resultSet.getLong("id")).thenReturn(1L);
            when(resultSet.getString("product_code")).thenReturn(productCode);
            when(resultSet.getString("location")).thenReturn(fromLocation.name());
            when(resultSet.getTimestamp("received_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getDate("expiry")).thenReturn(Date.valueOf(LocalDate.now().plusDays(30)));
            when(resultSet.getInt("quantity")).thenReturn(100);

            // Mock the deduction update
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // When
            inventoryRepository.transferStock(connection, productCode, fromLocation, toLocation, quantity);

            // Then
            verify(connection, atLeast(2)).prepareStatement(anyString());
            verify(preparedStatement, atLeast(1)).executeUpdate();
        }

        @Test
        @DisplayName("Should throw exception for zero quantity transfer")
        void shouldThrowExceptionForZeroQuantityTransfer() {
            // Given
            String productCode = "PROD001";
            StockLocation fromLocation = StockLocation.MAIN_STORE;
            StockLocation toLocation = StockLocation.SHELF;
            int quantity = 0;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> inventoryRepository.transferStock(connection, productCode, fromLocation, toLocation, quantity));
            assertEquals("Transfer quantity must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for negative quantity transfer")
        void shouldThrowExceptionForNegativeQuantityTransfer() {
            // Given
            String productCode = "PROD001";
            StockLocation fromLocation = StockLocation.MAIN_STORE;
            StockLocation toLocation = StockLocation.SHELF;
            int quantity = -10;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> inventoryRepository.transferStock(connection, productCode, fromLocation, toLocation, quantity));
            assertEquals("Transfer quantity must be positive", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcInventoryRepository with DataSource")
        void shouldCreateJdbcInventoryRepositoryWithDataSource() {
            // When
            JdbcInventoryRepository repository = new JdbcInventoryRepository(dataSource);

            // Then
            assertNotNull(repository);
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

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("q")).thenReturn(0);

            // When
            int result = inventoryRepository.totalAvailable(connection, emptyProductCode, location);

            // Then
            assertEquals(0, result);
            verify(preparedStatement).setString(1, emptyProductCode);
        }

        @Test
        @DisplayName("Should handle very long product codes")
        void shouldHandleVeryLongProductCodes() throws SQLException {
            // Given
            String longProductCode = "PRODUCT-CODE-" + "X".repeat(100);
            String location = "SHELF";

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("q")).thenReturn(25);

            // When
            int result = inventoryRepository.totalAvailable(connection, longProductCode, location);

            // Then
            assertEquals(25, result);
            verify(preparedStatement).setString(1, longProductCode);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly in find candidates")
        void shouldCloseResourcesProperlyInFindCandidates() throws SQLException {
            // Given
            Code productCode = new Code("PROD001");
            StockLocation location = StockLocation.MAIN_STORE;

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            inventoryRepository.findDeductionCandidates(connection, productCode, location);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
        }

        @Test
        @DisplayName("Should close resources properly in total available")
        void shouldCloseResourcesProperlyInTotalAvailable() throws SQLException {
            // Given
            String productCode = "PROD001";
            String location = "MAIN_STORE";

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("q")).thenReturn(100);

            // When
            inventoryRepository.totalAvailable(connection, productCode, location);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
        }
    }
}
