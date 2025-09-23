package domain.policies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.policies.AbstractBatchStrategy;
import domain.repository.InventoryRepository;
import domain.shared.Code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AbstractBatchStrategy Domain Policy Tests")
class AbstractBatchStrategyTest {

    private InventoryRepository inventory;
    private TestBatchStrategy strategy;
    private Code productCode;
    private StockLocation location;

    @BeforeEach
    void setUp() {
        inventory = mock(InventoryRepository.class);
        strategy = new TestBatchStrategy(inventory);
        var batches = new ArrayList<Batch>();
        productCode = new Code("FOOD001");
        location = StockLocation.MAIN_STORE;

        // Create test batches using the real constructor: (id, productCode, location, receivedAt, expiry, quantity)
        batches.add(new Batch(1L, productCode, location, LocalDateTime.now().minusDays(10), LocalDate.now().minusDays(1), new domain.shared.Quantity(0)));   // expired, zero qty
        batches.add(new Batch(2L, productCode, location, LocalDateTime.now().minusDays(5), LocalDate.now().plusDays(7), new domain.shared.Quantity(20)));   // available
        batches.add(new Batch(3L, productCode, location, LocalDateTime.now().minusDays(2), LocalDate.now().plusDays(15), new domain.shared.Quantity(30)));  // available

        strategy.setCandidates(batches);
    }

    @Test
    @DisplayName("Should deduct across candidate batches in order until fulfilled")
    void shouldDeductAcrossBatchesInOrder() {
        // Given
        int qtyNeeded = 45; // should take 20 from batch 2 and 25 from batch 3

        // When
        strategy.deduct(null, productCode, qtyNeeded, location);

        // Then
        verify(inventory, times(1)).deductFromBatch(isNull(), eq(2L), eq(20));
        verify(inventory, times(1)).deductFromBatch(isNull(), eq(3L), eq(25));
        verify(inventory, never()).deductFromBatch(isNull(), eq(1L), anyInt());
    }

    @Test
    @DisplayName("Should throw when insufficient stock remains after iterating candidates")
    void shouldThrowWhenInsufficientStock() {
        // Given total available = 50, request > 50
        int qtyNeeded = 60;

        // When
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> strategy.deduct(null, productCode, qtyNeeded, location));

        // Then
        assertTrue(ex.getMessage().contains("Insufficient stock"));
        // Still deducts everything it can before throwing
        verify(inventory, times(1)).deductFromBatch(isNull(), eq(2L), eq(20));
        verify(inventory, times(1)).deductFromBatch(isNull(), eq(3L), eq(30));
    }

    @Test
    @DisplayName("deductUpTo should return taken amount when not enough stock")
    void deductUpToShouldReturnTakenAmount() {
        // Given
        int qtyNeeded = 60; // more than available 50

        // When
        int taken = strategy.deductUpTo(null, productCode, qtyNeeded, location);

        // Then
        assertEquals(50, taken);
        verify(inventory, times(1)).deductFromBatch(isNull(), eq(2L), eq(20));
        verify(inventory, times(1)).deductFromBatch(isNull(), eq(3L), eq(30));
    }

    // Concrete test strategy that exposes candidates via a setter
    private static class TestBatchStrategy extends AbstractBatchStrategy {
        private List<Batch> preset = List.of();

        TestBatchStrategy(InventoryRepository inventory) {
            super(inventory);
        }

        void setCandidates(List<Batch> batches) {
            this.preset = new ArrayList<>(batches);
        }

        @Override
        protected List<Batch> candidates(Connection con, Code productCode, StockLocation location) {
            return preset;
        }
    }
}
