package domain.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.pricing.DiscountPolicy;
import domain.billing.BillLine;
import domain.shared.Money;
import domain.shared.Code;
import domain.shared.Quantity;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@DisplayName("DiscountPolicy Interface Tests")
class DiscountPolicyTest {

    @Test
    @DisplayName("Should define contract for discount calculation on bill lines")
    void shouldDefineContractForDiscountCalculationOnBillLines() {
        // Given
        TestDiscountPolicy policy = new TestDiscountPolicy();
        List<BillLine> billLines = createTestBillLines();

        // When
        Money discount = policy.discountFor(billLines);

        // Then
        assertNotNull(discount);
        assertEquals(Money.of(new BigDecimal("10.00")), discount);
    }

    @Test
    @DisplayName("Should enforce implementation of discountFor method")
    void shouldEnforceImplementationOfDiscountForMethod() {
        // Given
        TestDiscountPolicy policy = new TestDiscountPolicy();
        List<BillLine> emptyLines = new ArrayList<>();

        // When
        Money result = policy.discountFor(emptyLines);

        // Then
        assertNotNull(result);
        assertEquals(Money.of(BigDecimal.ZERO), result);
    }

    @Test
    @DisplayName("Should handle various bill line scenarios")
    void shouldHandleVariousBillLineScenarios() {
        // Given
        FixedAmountDiscountPolicy policy = new FixedAmountDiscountPolicy(new BigDecimal("5.00"));
        List<BillLine> billLines = createTestBillLines();

        // When
        Money discount = policy.discountFor(billLines);

        // Then
        assertEquals(Money.of(new BigDecimal("5.00")), discount);
    }

    @Test
    @DisplayName("Should work with different discount policy implementations")
    void shouldWorkWithDifferentDiscountPolicyImplementations() {
        // Given
        PercentageDiscountPolicy percentPolicy = new PercentageDiscountPolicy(10);
        FixedAmountDiscountPolicy fixedPolicy = new FixedAmountDiscountPolicy(new BigDecimal("15.00"));
        List<BillLine> billLines = createTestBillLines();

        // When
        Money percentDiscount = percentPolicy.discountFor(billLines);
        Money fixedDiscount = fixedPolicy.discountFor(billLines);

        // Then
        assertEquals(Money.of(new BigDecimal("10.00")), percentDiscount); // 10% of $100
        assertEquals(Money.of(new BigDecimal("15.00")), fixedDiscount);
    }

    // Helper method to create test bill lines
    private List<BillLine> createTestBillLines() {
        List<BillLine> lines = new ArrayList<>();
        lines.add(createMockBillLine(Money.of(new BigDecimal("60.00"))));
        lines.add(createMockBillLine(Money.of(new BigDecimal("40.00"))));
        return lines; // Total: $100.00
    }

    // Helper method to create BillLine matching a specific line total
    private BillLine createMockBillLine(Money lineTotal) {
        // Use qty=1 so that lineTotal equals unitPrice
        return new BillLine(new Code("TEST-PROD"), "Test Item", new Quantity(1), lineTotal);
    }

    // Test implementations of DiscountPolicy interface
    private static class TestDiscountPolicy implements DiscountPolicy {
        @Override
        public Money discountFor(List<BillLine> lines) {
            if (lines == null || lines.isEmpty()) {
                return Money.of(BigDecimal.ZERO);
            }
            // Fixed $10 discount for testing
            return Money.of(new BigDecimal("10.00"));
        }
    }

    private static class FixedAmountDiscountPolicy implements DiscountPolicy {
        private final BigDecimal fixedAmount;

        public FixedAmountDiscountPolicy(BigDecimal fixedAmount) {
            this.fixedAmount = fixedAmount;
        }

        @Override
        public Money discountFor(List<BillLine> lines) {
            return Money.of(fixedAmount);
        }
    }

    private static class PercentageDiscountPolicy implements DiscountPolicy {
        private final int percentage;

        public PercentageDiscountPolicy(int percentage) {
            this.percentage = percentage;
        }

        @Override
        public Money discountFor(List<BillLine> lines) {
            if (lines == null || lines.isEmpty()) {
                return Money.of(BigDecimal.ZERO);
            }

            Money total = lines.stream()
                .map(BillLine::lineTotal)
                .reduce(Money.of(BigDecimal.ZERO), Money::plus);

            BigDecimal discountAmount = total.amount()
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            return Money.of(discountAmount);
        }
    }
}
