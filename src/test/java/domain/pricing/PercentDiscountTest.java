package domain.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import domain.pricing.PercentDiscount;
import domain.billing.BillLine;
import domain.shared.Money;
import domain.shared.Code;
import domain.shared.Quantity;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@DisplayName("PercentDiscount Domain Policy Tests")
class PercentDiscountTest {

    private List<BillLine> billLines;

    @BeforeEach
    void setUp() {
        billLines = new ArrayList<>();

        // Create mock bill lines with known totals
        BillLine line1 = createMockBillLine(Money.of(new BigDecimal("50.00")));
        BillLine line2 = createMockBillLine(Money.of(new BigDecimal("30.00")));

        billLines.add(line1);
        billLines.add(line2);
        // Total: $80.00
    }

    @Test
    @DisplayName("Should create PercentDiscount with valid percentage")
    void shouldCreatePercentDiscountWithValidPercentage() {
        // When
        PercentDiscount discount = new PercentDiscount(10);

        // Then
        assertNotNull(discount);
        assertEquals(10, discount.getPercentage());
    }

    @Test
    @DisplayName("Should throw exception for negative percentage")
    void shouldThrowExceptionForNegativePercentage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new PercentDiscount(-5));
        assertEquals("percent 0..100", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for percentage over 100")
    void shouldThrowExceptionForPercentageOver100() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new PercentDiscount(150));
        assertEquals("percent 0..100", exception.getMessage());
    }

    @Test
    @DisplayName("Should calculate discount correctly for 10% discount")
    void shouldCalculateDiscountCorrectlyFor10PercentDiscount() {
        // Given
        PercentDiscount discount = new PercentDiscount(10);

        // When
        Money discountAmount = discount.discountFor(billLines);

        // Then
        // 10% of $80.00 = $8.00
        assertEquals(Money.of(new BigDecimal("8.00")), discountAmount);
    }

    @Test
    @DisplayName("Should calculate discount correctly for 25% discount")
    void shouldCalculateDiscountCorrectlyFor25PercentDiscount() {
        // Given
        PercentDiscount discount = new PercentDiscount(25);

        // When
        Money discountAmount = discount.discountFor(billLines);

        // Then
        // 25% of $80.00 = $20.00
        assertEquals(Money.of(new BigDecimal("20.00")), discountAmount);
    }

    @Test
    @DisplayName("Should handle zero percent discount")
    void shouldHandleZeroPercentDiscount() {
        // Given
        PercentDiscount discount = new PercentDiscount(0);

        // When
        Money discountAmount = discount.discountFor(billLines);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
    }

    @Test
    @DisplayName("Should handle 100% discount")
    void shouldHandle100PercentDiscount() {
        // Given
        PercentDiscount discount = new PercentDiscount(100);

        // When
        Money discountAmount = discount.discountFor(billLines);

        // Then
        // 100% of $80.00 = $80.00
        assertEquals(Money.of(new BigDecimal("80.00")), discountAmount);
    }

    @Test
    @DisplayName("Should return zero discount for empty bill lines")
    void shouldReturnZeroDiscountForEmptyBillLines() {
        // Given
        PercentDiscount discount = new PercentDiscount(20);
        List<BillLine> emptyLines = new ArrayList<>();

        // When
        Money discountAmount = discount.discountFor(emptyLines);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
    }

    @Test
    @DisplayName("Should handle rounding correctly for fractional cents")
    void shouldHandleRoundingCorrectlyForFractionalCents() {
        // Given
        PercentDiscount discount = new PercentDiscount(33); // 33%
        List<BillLine> oddAmountLines = new ArrayList<>();
        oddAmountLines.add(createMockBillLine(Money.of(new BigDecimal("10.01"))));

        // When
        Money discountAmount = discount.discountFor(oddAmountLines);

        // Then
        // 33% of $10.01 = $3.3033, should round to $3.30
        assertEquals(Money.of(new BigDecimal("3.30")), discountAmount);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 25, 50, 75, 99, 100})
    @DisplayName("Should accept valid percentage values")
    void shouldAcceptValidPercentageValues(int percent) {
        // When
        PercentDiscount discount = new PercentDiscount(percent);

        // Then
        assertEquals(percent, discount.getPercentage());
    }

    @Test
    @DisplayName("Should calculate discount for single bill line")
    void shouldCalculateDiscountForSingleBillLine() {
        // Given
        PercentDiscount discount = new PercentDiscount(15);
        List<BillLine> singleLine = List.of(createMockBillLine(Money.of(new BigDecimal("100.00"))));

        // When
        Money discountAmount = discount.discountFor(singleLine);

        // Then
        // 15% of $100.00 = $15.00
        assertEquals(Money.of(new BigDecimal("15.00")), discountAmount);
    }

    @Test
    @DisplayName("Should calculate discount for multiple bill lines")
    void shouldCalculateDiscountForMultipleBillLines() {
        // Given
        PercentDiscount discount = new PercentDiscount(20);
        List<BillLine> multipleLines = new ArrayList<>();
        multipleLines.add(createMockBillLine(Money.of(new BigDecimal("25.00"))));
        multipleLines.add(createMockBillLine(Money.of(new BigDecimal("35.00"))));
        multipleLines.add(createMockBillLine(Money.of(new BigDecimal("40.00"))));

        // When
        Money discountAmount = discount.discountFor(multipleLines);

        // Then
        // 20% of ($25 + $35 + $40) = 20% of $100 = $20.00
        assertEquals(Money.of(new BigDecimal("20.00")), discountAmount);
    }

    // Helper method to create a BillLine with a specific line total
    private BillLine createMockBillLine(Money lineTotal) {
        // Use qty=1 so that lineTotal equals unitPrice
        return new BillLine(new Code("TEST-PROD"), "Test Item", new Quantity(1), lineTotal);
    }
}
