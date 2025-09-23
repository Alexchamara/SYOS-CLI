package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.InventoryRepository;
import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.shared.Code;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DisplayName("InventoryRepository Domain Interface Tests")
class InventoryRepositoryTest {

    private InventoryRepository inventoryRepository;
    private Connection connection;
    private Batch batch;

    private Code productCode;
    private long batchId;
    private StockLocation stockLocation;

    private String productCodeStr;
    private String locationStr;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        connection = mock(Connection.class);
        batch = mock(Batch.class);
        productCode = new Code("PROD001");
        batchId = 101L;
        stockLocation = StockLocation.MAIN_STORE;
        productCodeStr = "PROD001";
        locationStr = "MAIN_STORE";
    }

    @Test
    @DisplayName("Should define contract for creating batches and returning id")
    void shouldDefineContractForCreatingBatches() {
        // Given
        LocalDateTime receivedAt = LocalDateTime.now();
        LocalDate expiry = LocalDate.now().plusDays(30);
        when(inventoryRepository.createBatch(connection, productCode, stockLocation, receivedAt, expiry, 20))
            .thenReturn(555L);

        // When
        long id = inventoryRepository.createBatch(connection, productCode, stockLocation, receivedAt, expiry, 20);

        // Then
        assertEquals(555L, id);
        verify(inventoryRepository).createBatch(connection, productCode, stockLocation, receivedAt, expiry, 20);
    }

    @Test
    @DisplayName("Should define contract for finding batch by id")
    void shouldDefineContractForFindingBatchById() {
        // Given
        when(inventoryRepository.findBatchById(connection, batchId)).thenReturn(Optional.of(batch));

        // When
        Optional<Batch> foundBatch = inventoryRepository.findBatchById(connection, batchId);

        // Then
        assertTrue(foundBatch.isPresent());
        assertEquals(batch, foundBatch.get());
        verify(inventoryRepository).findBatchById(connection, batchId);
    }

    @Test
    @DisplayName("Should define contract for finding deduction candidates")
    void shouldDefineContractForFindingDeductionCandidates() {
        // Given
        List<Batch> expectedBatches = List.of(batch);
        when(inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation))
            .thenReturn(expectedBatches);

        // When
        List<Batch> candidates = inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation);

        // Then
        assertEquals(expectedBatches, candidates);
        verify(inventoryRepository).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should define contract for finding batches by product")
    void shouldDefineContractForFindingBatchesByProduct() {
        // Given
        List<Batch> expectedBatches = List.of(batch);
        when(inventoryRepository.findBatchesByProduct(connection, productCode)).thenReturn(expectedBatches);

        // When
        List<Batch> batches = inventoryRepository.findBatchesByProduct(connection, productCode);

        // Then
        assertEquals(expectedBatches, batches);
        verify(inventoryRepository).findBatchesByProduct(connection, productCode);
    }

    @Test
    @DisplayName("Should define contract for finding batches by location")
    void shouldDefineContractForFindingBatchesByLocation() {
        // Given
        List<Batch> expectedBatches = List.of(batch);
        when(inventoryRepository.findBatchesByLocation(connection, stockLocation)).thenReturn(expectedBatches);

        // When
        List<Batch> batches = inventoryRepository.findBatchesByLocation(connection, stockLocation);

        // Then
        assertEquals(expectedBatches, batches);
        verify(inventoryRepository).findBatchesByLocation(connection, stockLocation);
    }

    @Test
    @DisplayName("Should define contract for updating a batch")
    void shouldDefineContractForUpdatingBatch() {
        // Given
        LocalDate newExpiry = LocalDate.now().plusDays(60);
        int newQty = 50;

        // When
        inventoryRepository.updateBatch(connection, batchId, newExpiry, newQty);

        // Then
        verify(inventoryRepository).updateBatch(connection, batchId, newExpiry, newQty);
    }

    @Test
    @DisplayName("Should define contract for totalAvailable for product at location")
    void shouldDefineContractForTotalAvailable() {
        // Given
        when(inventoryRepository.totalAvailable(connection, productCodeStr, locationStr))
            .thenReturn(120);

        // When
        int total = inventoryRepository.totalAvailable(connection, productCodeStr, locationStr);

        // Then
        assertEquals(120, total);
        verify(inventoryRepository).totalAvailable(connection, productCodeStr, locationStr);
    }
}
