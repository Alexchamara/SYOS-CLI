package domain.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import domain.pricing.NoDiscount;
import domain.billing.BillLine;
import domain.shared.Money;
import domain.shared.Code;
import domain.shared.Quantity;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@DisplayName("NoDiscount Domain Policy Tests")
class NoDiscountTest {

    private NoDiscount noDiscount;
    private List<BillLine> billLines;

    @BeforeEach
    void setUp() {
        noDiscount = new NoDiscount();
        billLines = new ArrayList<>();

        // Create test bill lines using the real BillLine constructor
        BillLine line1 = createMockBillLine(Money.of(new BigDecimal("50.00")));
        BillLine line2 = createMockBillLine(Money.of(new BigDecimal("30.00")));

        billLines.add(line1);
        billLines.add(line2);
    }

    @Test
    @DisplayName("Should return zero discount for any bill lines")
    void shouldReturnZeroDiscountForAnyBillLines() {
        // When
        Money discountAmount = noDiscount.discountFor(billLines);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
    }

    @Test
    @DisplayName("Should return zero discount for empty bill lines")
    void shouldReturnZeroDiscountForEmptyBillLines() {
        // Given
        List<BillLine> emptyLines = new ArrayList<>();

        // When
        Money discountAmount = noDiscount.discountFor(emptyLines);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
    }

    @Test
    @DisplayName("Should return zero discount regardless of bill total")
    void shouldReturnZeroDiscountRegardlessOfBillTotal() {
        // Given
        List<BillLine> expensiveLines = new ArrayList<>();
        expensiveLines.add(createMockBillLine(Money.of(new BigDecimal("500.00"))));
        expensiveLines.add(createMockBillLine(Money.of(new BigDecimal("1000.00"))));

        // When
        Money discountAmount = noDiscount.discountFor(expensiveLines);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
    }

    @Test
    @DisplayName("Should handle null bill lines gracefully")
    void shouldHandleNullBillLinesGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            Money discountAmount = noDiscount.discountFor(null);
            assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
        });
    }

    // Helper method to create BillLine matching a specific line total
    private BillLine createMockBillLine(Money lineTotal) {
        // Use qty=1 so that lineTotal equals unitPrice
        return new BillLine(new Code("TEST-PROD"), "Test Item", new Quantity(1), lineTotal);
    }
}
