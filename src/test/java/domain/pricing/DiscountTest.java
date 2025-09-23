package domain.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import domain.pricing.Discount;
import domain.pricing.Discount.DiscountType;
import domain.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

@DisplayName("Discount Domain Entity Tests")
class DiscountTest {

    private Discount percentageDiscount;
    private Discount fixedAmountDiscount;
    private Money originalPrice;

    @BeforeEach
    void setUp() {
        originalPrice = Money.of(new BigDecimal("100.00"));

        percentageDiscount = new Discount(
            1L,                              // id
            100L,                            // batchId
            DiscountType.PERCENTAGE,         // type
            new BigDecimal("10.00"),         // value (10%)
            LocalDate.now(),                 // startDate
            LocalDate.now().plusDays(30),    // endDate
            true,                            // isActive
            "10% off promotion",             // description
            1L                               // createdBy
        );

        fixedAmountDiscount = new Discount(
            2L,                              // id
            101L,                            // batchId
            DiscountType.FIXED_AMOUNT,       // type
            new BigDecimal("15.00"),         // value ($15 off)
            LocalDate.now(),                 // startDate
            LocalDate.now().plusDays(30),    // endDate
            true,                            // isActive
            "$15 off promotion",             // description
            1L                               // createdBy
        );
    }

    @Test
    @DisplayName("Should create Discount with all properties")
    void shouldCreateDiscountWithAllProperties() {
        // Then
        assertEquals(1L, percentageDiscount.getId());
        assertEquals(100L, percentageDiscount.getBatchId());
        assertEquals(DiscountType.PERCENTAGE, percentageDiscount.getType());
        assertEquals(new BigDecimal("10.00"), percentageDiscount.getValue());
        assertEquals(LocalDate.now(), percentageDiscount.getStartDate());
        assertEquals(LocalDate.now().plusDays(30), percentageDiscount.getEndDate());
        assertTrue(percentageDiscount.isActive());
        assertEquals("10% off promotion", percentageDiscount.getDescription());
        assertEquals(1L, percentageDiscount.getCreatedBy());
    }

    @Test
    @DisplayName("Should validate discount for current date")
    void shouldValidateDiscountForCurrentDate() {
        // Given
        LocalDate today = LocalDate.now();

        // Then
        assertTrue(percentageDiscount.isValidForDate(today));
    }

    @Test
    @DisplayName("Should not validate discount before start date")
    void shouldNotValidateDiscountBeforeStartDate() {
        // Given
        LocalDate beforeStart = LocalDate.now().minusDays(1);

        // Then
        assertFalse(percentageDiscount.isValidForDate(beforeStart));
    }

    @Test
    @DisplayName("Should not validate discount after end date")
    void shouldNotValidateDiscountAfterEndDate() {
        // Given
        LocalDate afterEnd = LocalDate.now().plusDays(31);

        // Then
        assertFalse(percentageDiscount.isValidForDate(afterEnd));
    }

    @Test
    @DisplayName("Should not validate inactive discount")
    void shouldNotValidateInactiveDiscount() {
        // Given
        Discount inactiveDiscount = new Discount(
            3L, 102L, DiscountType.PERCENTAGE, new BigDecimal("20.00"),
            LocalDate.now(), LocalDate.now().plusDays(30), false,
            "Inactive discount", 1L
        );

        // Then
        assertFalse(inactiveDiscount.isValidForDate(LocalDate.now()));
    }

    @Test
    @DisplayName("Should calculate percentage discount amount correctly")
    void shouldCalculatePercentageDiscountAmountCorrectly() {
        // When
        Money discountAmount = percentageDiscount.calculateDiscountAmount(originalPrice);

        // Then
        assertEquals(Money.of(new BigDecimal("10.00")), discountAmount); // 10% of $100
    }

    @Test
    @DisplayName("Should calculate fixed amount discount correctly")
    void shouldCalculateFixedAmountDiscountCorrectly() {
        // When
        Money discountAmount = fixedAmountDiscount.calculateDiscountAmount(originalPrice);

        // Then
        assertEquals(Money.of(new BigDecimal("15.00")), discountAmount);
    }

    @Test
    @DisplayName("Should apply percentage discount to price")
    void shouldApplyPercentageDiscountToPrice() {
        // When
        Money discountedPrice = percentageDiscount.applyDiscount(originalPrice);

        // Then
        assertEquals(Money.of(new BigDecimal("90.00")), discountedPrice); // $100 - $10
    }

    @Test
    @DisplayName("Should apply fixed amount discount to price")
    void shouldApplyFixedAmountDiscountToPrice() {
        // When
        Money discountedPrice = fixedAmountDiscount.applyDiscount(originalPrice);

        // Then
        assertEquals(Money.of(new BigDecimal("85.00")), discountedPrice); // $100 - $15
    }

    @Test
    @DisplayName("Should not allow negative price after discount")
    void shouldNotAllowNegativePriceAfterDiscount() {
        // Given
        Money smallPrice = Money.of(new BigDecimal("5.00"));

        // When
        Money discountedPrice = fixedAmountDiscount.applyDiscount(smallPrice);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountedPrice); // Should be $0, not negative
    }

    @Test
    @DisplayName("Should handle percentage discount calculation with rounding")
    void shouldHandlePercentageDiscountCalculationWithRounding() {
        // Given
        Discount discountWithRounding = new Discount(
            4L, 103L, DiscountType.PERCENTAGE, new BigDecimal("33.33"),
            LocalDate.now(), LocalDate.now().plusDays(30), true,
            "Rounding test", 1L
        );
        Money priceForRounding = Money.of(new BigDecimal("99.99"));

        // When
        Money discountAmount = discountWithRounding.calculateDiscountAmount(priceForRounding);

        // Then
        // 33.33% of $99.99 = $33.33 (rounded to 2 decimal places)
        assertEquals(Money.of(new BigDecimal("33.33")), discountAmount);
    }

    @Test
    @DisplayName("Should validate discount on start date boundary")
    void shouldValidateDiscountOnStartDateBoundary() {
        // Given
        LocalDate startDate = LocalDate.now();

        // Then
        assertTrue(percentageDiscount.isValidForDate(startDate));
    }

    @Test
    @DisplayName("Should validate discount on end date boundary")
    void shouldValidateDiscountOnEndDateBoundary() {
        // Given
        LocalDate endDate = LocalDate.now().plusDays(30);

        // Then
        assertTrue(percentageDiscount.isValidForDate(endDate));
    }

    @Test
    @DisplayName("Should handle zero discount value")
    void shouldHandleZeroDiscountValue() {
        // Given
        Discount zeroDiscount = new Discount(
            5L, 104L, DiscountType.PERCENTAGE, BigDecimal.ZERO,
            LocalDate.now(), LocalDate.now().plusDays(30), true,
            "Zero discount", 1L
        );

        // When
        Money discountAmount = zeroDiscount.calculateDiscountAmount(originalPrice);
        Money discountedPrice = zeroDiscount.applyDiscount(originalPrice);

        // Then
        assertEquals(Money.of(BigDecimal.ZERO), discountAmount);
        assertEquals(originalPrice, discountedPrice);
    }
}
