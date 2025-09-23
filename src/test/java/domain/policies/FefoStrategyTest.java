package domain.policies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.policies.FefoStrategy;
import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import domain.shared.Code;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;

@DisplayName("FefoStrategy Domain Policy Tests")
class FefoStrategyTest {

    private InventoryRepository inventoryRepository;
    private Connection connection;
    private Batch batch1;
    private Batch batch2;

    private FefoStrategy fefoStrategy;
    private Code productCode;
    private StockLocation stockLocation;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        connection = mock(Connection.class);
        batch1 = mock(Batch.class);
        batch2 = mock(Batch.class);

        fefoStrategy = new FefoStrategy(inventoryRepository);
        productCode = new Code("PROD001");
        stockLocation = StockLocation.MAIN_STORE;
    }

    @Test
    @DisplayName("Should create FefoStrategy with inventory repository")
    void shouldCreateFefoStrategyWithInventoryRepository() {
        // When
        FefoStrategy strategy = new FefoStrategy(inventoryRepository);

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

        // When: call deductUpTo (which uses candidates under the hood)
        fefoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then: verify repository was delegated to for candidates
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
        fefoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then
        verify(inventoryRepository).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should delegate to repository for proper FEFO ordering")
    void shouldDelegateToRepositoryForProperFefoOrdering() {
        // Given
        List<Batch> orderedBatches = List.of(batch1, batch2);
        when(inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation))
            .thenReturn(orderedBatches);

        // When
        fefoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then
        // Verify the repository is called exactly once with correct parameters
        verify(inventoryRepository, times(1)).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should rely on repository for expiry date prioritization")
    void shouldRelyOnRepositoryForExpiryDatePrioritization() {
        // Given
        List<Batch> expiryOrderedBatches = List.of(batch1, batch2);
        when(inventoryRepository.findDeductionCandidates(connection, productCode, stockLocation))
            .thenReturn(expiryOrderedBatches);

        // When
        fefoStrategy.deductUpTo(connection, productCode, 0, stockLocation);

        // Then
        // FEFO strategy relies on the repository's ordering which prioritizes earlier expiry first
        verify(inventoryRepository).findDeductionCandidates(connection, productCode, stockLocation);
    }

    @Test
    @DisplayName("Should throw exception when inventory repository is null")
    void shouldThrowExceptionWhenInventoryRepositoryIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> new FefoStrategy(null));
    }
}
