package domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import domain.shared.Money;
import java.math.BigDecimal;

@DisplayName("Money Domain Object Tests")
class MoneyTest {

    @Test
    @DisplayName("Should create Money with valid BigDecimal amount")
    void shouldCreateMoneyWithValidBigDecimalAmount() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        Money money = Money.of(amount);

        // Then
        assertEquals(amount.stripTrailingZeros(), money.amount());
    }

    @Test
    @DisplayName("Should create Money from cents")
    void shouldCreateMoneyFromCents() {
        // Given
        long cents = 10050L; // 100.50

        // When
        Money money = Money.of(cents);

        // Then
        assertEquals(new BigDecimal("100.50"), money.amount());
        assertEquals(10050L, money.cents());
    }

    @Test
    @DisplayName("Should add two Money amounts")
    void shouldAddTwoMoneyAmounts() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(new BigDecimal("50.25"));

        // When
        Money result = money1.plus(money2);

        // Then
        assertEquals(new BigDecimal("150.75"), result.amount());
    }

    @Test
    @DisplayName("Should subtract two Money amounts")
    void shouldSubtractTwoMoneyAmounts() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(new BigDecimal("50.25"));

        // When
        Money result = money1.minus(money2);

        // Then
        assertEquals(new BigDecimal("50.25"), result.amount());
    }

    @Test
    @DisplayName("Should multiply Money by integer")
    void shouldMultiplyMoneyByInteger() {
        // Given
        Money money = Money.of(new BigDecimal("25.50"));

        // When
        Money result = money.times(3);

        // Then
        assertEquals(new BigDecimal("76.50"), result.amount());
    }

    @Test
    @DisplayName("Should handle zero amounts")
    void shouldHandleZeroAmounts() {
        // When
        Money zeroFromCents = Money.of(0L);
        Money zeroFromBigDecimal = Money.of(BigDecimal.ZERO);

        // Then
        assertEquals(BigDecimal.ZERO, zeroFromCents.amount());
        assertEquals(BigDecimal.ZERO, zeroFromBigDecimal.amount());
        assertEquals(0L, zeroFromCents.cents());
    }

    @Test
    @DisplayName("Should be equal when amounts are same")
    void shouldBeEqualWhenAmountsAreSame() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(10050L); // Same amount in cents

        // Then
        assertEquals(money1, money2);
        assertEquals(money1.hashCode(), money2.hashCode());
    }

    @Test
    @DisplayName("Should convert to string representation")
    void shouldConvertToStringRepresentation() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // When
        String result = money.toString();

        // Then
        assertEquals("100.5", result); // stripTrailingZeros removes .0
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 100, 1050, 10050, 100000})
    @DisplayName("Should correctly convert between cents and BigDecimal")
    void shouldCorrectlyConvertBetweenCentsAndBigDecimal(long cents) {
        // When
        Money money = Money.of(cents);

        // Then
        assertEquals(cents, money.cents());
        assertEquals(new BigDecimal(cents).divide(new BigDecimal("100")), money.amount());
    }

    @ParameterizedTest
    @CsvSource({
        "100.50, 50.25, 150.75",
        "0.00, 100.00, 100.00",
        "250.75, 124.25, 375.00"
    })
    @DisplayName("Should correctly add different amounts")
    void shouldCorrectlyAddDifferentAmounts(String amount1, String amount2, String expected) {
        // Given
        Money money1 = Money.of(new BigDecimal(amount1));
        Money money2 = Money.of(new BigDecimal(amount2));

        // When
        Money result = money1.plus(money2);

        // Then
        assertEquals(new BigDecimal(expected), result.amount());
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // Then
        assertNotEquals(null, money);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));
        String string = "100.50";

        // Then
        assertNotEquals(money, string);
    }

    @Test
    @DisplayName("Should handle negative amounts")
    void shouldHandleNegativeAmounts() {
        // Given
        Money negativeMoney = Money.of(new BigDecimal("-50.25"));

        // Then
        assertEquals(new BigDecimal("-50.25"), negativeMoney.amount());
        assertEquals(-5025L, negativeMoney.cents());
    }

    @Test
    @DisplayName("Should handle subtraction resulting in negative")
    void shouldHandleSubtractionResultingInNegative() {
        // Given
        Money money1 = Money.of(new BigDecimal("25.00"));
        Money money2 = Money.of(new BigDecimal("50.00"));

        // When
        Money result = money1.minus(money2);

        // Then
        assertEquals(new BigDecimal("-25"), result.amount());
    }

    @Test
    @DisplayName("Should strip trailing zeros")
    void shouldStripTrailingZeros() {
        // Given
        Money money = Money.of(new BigDecimal("100.00"));

        // Then
        assertEquals(new BigDecimal("100"), money.amount());
        assertEquals("100", money.toString());
    }

    @Test
    @DisplayName("Should handle multiplication by zero")
    void shouldHandleMultiplicationByZero() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // When
        Money result = money.times(0);

        // Then
        assertEquals(BigDecimal.ZERO, result.amount());
    }

    @Test
    @DisplayName("Should handle multiplication by negative number")
    void shouldHandleMultiplicationByNegativeNumber() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // When
        Money result = money.times(-2);

        // Then
        assertEquals(new BigDecimal("-201"), result.amount());
    }

    @Test
    @DisplayName("Should maintain immutability in operations")
    void shouldMaintainImmutabilityInOperations() {
        // Given
        Money original = Money.of(new BigDecimal("100.50"));
        Money other = Money.of(new BigDecimal("50.25"));

        // When
        Money added = original.plus(other);
        Money subtracted = original.minus(other);
        Money multiplied = original.times(2);

        // Then
        assertEquals(new BigDecimal("100.5"), original.amount()); // Original unchanged
        assertEquals(new BigDecimal("150.75"), added.amount());
        assertEquals(new BigDecimal("50.25"), subtracted.amount());
        assertEquals(new BigDecimal("201"), multiplied.amount());
    }
}
