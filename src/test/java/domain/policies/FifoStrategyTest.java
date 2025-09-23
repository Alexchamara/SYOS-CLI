package domain.policies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.policies.FifoStrategy;
import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import domain.shared.Code;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;

@DisplayName("FifoStrategy Domain Policy Tests")
class FifoStrategyTest {

    private InventoryRepository inventoryRepository;
    private Connection connection;
    private Batch batch1;
    private Batch batch2;

    private FifoStrategy fifoStrategy;
    private Code productCode;
    private StockLocation stockLocation;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        connection = mock(Connection.class);
        batch1 = mock(Batch.class);
        batch2 = mock(Batch.class);

        fifoStrategy = new FifoStrategy(inventoryRepository);
        productCode = new Code("PROD001");
        stockLocation = StockLocation.MAIN_STORE;
    }

    @Test
    @DisplayName("Should create FifoStrategy with inventory repository")
    void shouldCreateFifoStrategyWithInventoryRepository() {
        // When
        FifoStrategy strategy = new FifoStrategy(inventoryRepository);

        // Then
        assertNotNull(strategy);
    }

    @Test
    @DisplayName("Should fetch candidates from inventory repository")
    void shouldFetchCandidatesFromInventoryRepository() {
        // Given
        List<Batch> expectedBatches = List.of(batch1, batch2);
        when(inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation))
            .thenReturn(expectedBatches);

        // When: call deductUpTo (which internally obtains candidates)
        fifoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then
        verify(inventoryRepository).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should handle empty candidates list")
    void shouldHandleEmptyCandidatesList() {
        // Given
        List<Batch> emptyList = new ArrayList<>();
        when(inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation))
            .thenReturn(emptyList);

        // When
        fifoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then
        verify(inventoryRepository).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should delegate to repository for proper FIFO ordering")
    void shouldDelegateToRepositoryForProperFifoOrdering() {
        // Given
        List<Batch> orderedBatches = List.of(batch1, batch2);
        when(inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation))
            .thenReturn(orderedBatches);

        // When
        fifoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then
        // Verify the repository is called exactly once with correct parameters
        verify(inventoryRepository, times(1)).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should throw exception when inventory repository is null")
    void shouldThrowExceptionWhenInventoryRepositoryIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> new FifoStrategy(null));
    }
}
