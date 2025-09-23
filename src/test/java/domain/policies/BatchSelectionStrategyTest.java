package domain.policies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.policies.BatchSelectionStrategy;
import domain.inventory.StockLocation;
import domain.shared.Code;

@DisplayName("BatchSelectionStrategy Interface Tests")
class BatchSelectionStrategyTest {

    @Test
    @DisplayName("Should define contract for batch selection")
    void shouldDefineContractForBatchSelection() {
        // Given
        TestBatchSelectionStrategy strategy = new TestBatchSelectionStrategy();

        // Then
        assertNotNull(strategy);
        assertInstanceOf(BatchSelectionStrategy.class, strategy);
    }

    @Test
    @DisplayName("Should allow calling deductUpTo and return a value within [0, requested]")
    void shouldAllowCallingDeductUpTo() {
        // Given
        TestBatchSelectionStrategy strategy = new TestBatchSelectionStrategy();
        int requested = 10;

        // When
        int taken = strategy.deductUpTo(null, new Code("FOO"), requested, StockLocation.MAIN_STORE);

        // Then
        assertTrue(taken >= 0 && taken <= requested);
    }

    @Test
    @DisplayName("Should allow calling deduct without throwing")
    void shouldAllowCallingDeduct() {
        // Given
        TestBatchSelectionStrategy strategy = new TestBatchSelectionStrategy();

        // Then
        assertDoesNotThrow(() -> strategy.deduct(null, new Code("BAR"), 5, StockLocation.MAIN_STORE));
    }

    // Minimal test implementation of BatchSelectionStrategy interface
    private static class TestBatchSelectionStrategy implements BatchSelectionStrategy {
        @Override
        public void deduct(java.sql.Connection con, Code productCode, int quantity, StockLocation location) {
            // no-op in test stub
        }

        @Override
        public int deductUpTo(java.sql.Connection con, Code productCode, int quantity, StockLocation location) {
            // For testing contract stub: always return 0 which is within [0, quantity]
            return 0;
        }
    }
}
