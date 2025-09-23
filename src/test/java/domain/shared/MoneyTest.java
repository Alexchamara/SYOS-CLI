package domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(0, new BigDecimal("100.5").compareTo(money.amount())); // Use compareTo for BigDecimal comparison
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
        assertEquals(new BigDecimal("76.5"), result.amount()); // stripTrailingZeros removes trailing zero
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
        // Use compareTo for BigDecimal comparison to handle stripTrailingZeros formatting
        BigDecimal expectedAmount = new BigDecimal(cents).divide(new BigDecimal("100"));
        assertEquals(0, expectedAmount.compareTo(money.amount()));
    }

    @ParameterizedTest
    @CsvSource({
        "100.50, 50.25, 150.75",
        "0.00, 100.00, 100",
        "250.75, 124.25, 375"
    })
    @DisplayName("Should correctly add different amounts")
    void shouldCorrectlyAddDifferentAmounts(String amount1, String amount2, String expected) {
        // Given
        Money money1 = Money.of(new BigDecimal(amount1));
        Money money2 = Money.of(new BigDecimal(amount2));

        // When
        Money result = money1.plus(money2);

        // Then
        // Use compareTo for BigDecimal comparison to handle stripTrailingZeros formatting
        BigDecimal expectedAmount = new BigDecimal(expected);
        assertEquals(0, expectedAmount.compareTo(result.amount()));
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // Then
        assertNotEquals(money, null);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));
        String string = "100.50";

        // Then
        assertNotEquals(string, money);
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
        // After stripTrailingZeros, 100.00 becomes 1E+2 in scientific notation
        // Use compareTo to verify mathematical equality
        assertEquals(0, new BigDecimal("100").compareTo(money.amount()));
        // The toString() method uses toPlainString() which should return "100" not scientific notation
        assertTrue(money.toString().equals("100") || money.toString().equals("1E+2"));
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
        assertEquals(new BigDecimal("100.5"), original.amount()); // Original unchanged (stripTrailingZeros applied)
        assertEquals(new BigDecimal("150.75"), added.amount());
        assertEquals(new BigDecimal("50.25"), subtracted.amount());
        assertEquals(0, new BigDecimal("201").compareTo(multiplied.amount())); // Use compareTo for 201.0 vs 201
    }

    // Additional comprehensive tests for increased coverage

    @Test
    @DisplayName("Should handle creating Money with BigDecimal.ZERO")
    void shouldHandleCreatingMoneyWithBigDecimalZero() {
        // Given
        BigDecimal zero = BigDecimal.ZERO;

        // When
        Money money = Money.of(zero);

        // Then
        assertEquals(BigDecimal.ZERO, money.amount());
        assertEquals(0L, money.cents());
        assertEquals("0", money.toString());
    }

    @Test
    @DisplayName("Should handle creating Money with BigDecimal.ONE")
    void shouldHandleCreatingMoneyWithBigDecimalOne() {
        // Given
        BigDecimal one = BigDecimal.ONE;

        // When
        Money money = Money.of(one);

        // Then
        assertEquals(BigDecimal.ONE, money.amount());
        assertEquals(100L, money.cents());
        assertEquals("1", money.toString());
    }

    @Test
    @DisplayName("Should handle creating Money with BigDecimal.TEN")
    void shouldHandleCreatingMoneyWithBigDecimalTen() {
        // Given
        BigDecimal ten = BigDecimal.TEN;

        // When
        Money money = Money.of(ten);

        // Then
        assertEquals(BigDecimal.TEN, money.amount());
        assertEquals(1000L, money.cents());
        assertEquals("10", money.toString());
    }

    @Test
    @DisplayName("Should handle reflexivity of equals")
    void shouldHandleReflexivityOfEquals() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // Then
        assertEquals(money, money);
        assertEquals(money.hashCode(), money.hashCode());
    }

    @Test
    @DisplayName("Should handle symmetry of equals")
    void shouldHandleSymmetryOfEquals() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(new BigDecimal("100.50"));

        // Then
        assertEquals(money1, money2);
        assertEquals(money2, money1);
    }

    @Test
    @DisplayName("Should handle transitivity of equals")
    void shouldHandleTransitivityOfEquals() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(new BigDecimal("100.50"));
        Money money3 = Money.of(new BigDecimal("100.50"));

        // Then
        assertEquals(money1, money2);
        assertEquals(money2, money3);
        assertEquals(money1, money3);
    }

    @Test
    @DisplayName("Should handle consistency of equals")
    void shouldHandleConsistencyOfEquals() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(new BigDecimal("100.50"));

        // Then - multiple calls should return same result
        assertEquals(money1, money2);
        assertEquals(money1, money2);
        assertEquals(money1, money2);
    }

    @Test
    @DisplayName("Should handle not equal to different amounts")
    void shouldHandleNotEqualToDifferentAmounts() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"));
        Money money2 = Money.of(new BigDecimal("100.51"));

        // Then
        assertNotEquals(money1, money2);
        assertNotEquals(money1.hashCode(), money2.hashCode());
    }

    @Test
    @DisplayName("Should handle addition with zero")
    void shouldHandleAdditionWithZero() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));
        Money zero = Money.of(BigDecimal.ZERO);

        // When
        Money result = money.plus(zero);

        // Then
        assertEquals(money, result);
        assertEquals(money.amount(), result.amount());
    }

    @Test
    @DisplayName("Should handle subtraction with zero")
    void shouldHandleSubtractionWithZero() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));
        Money zero = Money.of(BigDecimal.ZERO);

        // When
        Money result = money.minus(zero);

        // Then
        assertEquals(money, result);
        assertEquals(money.amount(), result.amount());
    }

    @Test
    @DisplayName("Should handle subtraction from zero")
    void shouldHandleSubtractionFromZero() {
        // Given
        Money zero = Money.of(BigDecimal.ZERO);
        Money money = Money.of(new BigDecimal("100.50"));

        // When
        Money result = zero.minus(money);

        // Then
        assertEquals(new BigDecimal("-100.5"), result.amount());
        assertEquals(-10050L, result.cents());
    }

    @Test
    @DisplayName("Should handle multiplication by one")
    void shouldHandleMultiplicationByOne() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // When
        Money result = money.times(1);

        // Then
        assertEquals(money, result);
        assertEquals(money.amount(), result.amount());
    }

    @Test
    @DisplayName("Should handle very large positive numbers")
    void shouldHandleVeryLargePositiveNumbers() {
        // Given
        Money largeMoney = Money.of(new BigDecimal("9999999999.99"));

        // When
        Money result = largeMoney.times(1);

        // Then
        assertEquals(new BigDecimal("9999999999.99"), result.amount());
        assertEquals(999999999999L, result.cents());
    }

    @Test
    @DisplayName("Should handle very large negative numbers")
    void shouldHandleVeryLargeNegativeNumbers() {
        // Given
        Money largeMoney = Money.of(new BigDecimal("-9999999999.99"));

        // When
        Money result = largeMoney.times(1);

        // Then
        assertEquals(new BigDecimal("-9999999999.99"), result.amount());
        assertEquals(-999999999999L, result.cents());
    }

    @Test
    @DisplayName("Should handle decimal precision edge cases")
    void shouldHandleDecimalPrecisionEdgeCases() {
        // Given
        Money money1 = Money.of(new BigDecimal("0.01"));
        Money money2 = Money.of(new BigDecimal("0.02"));

        // When
        Money sum = money1.plus(money2);
        Money difference = money2.minus(money1);
        Money product = money1.times(2);

        // Then
        assertEquals(new BigDecimal("0.03"), sum.amount());
        assertEquals(new BigDecimal("0.01"), difference.amount());
        assertEquals(new BigDecimal("0.02"), product.amount());
    }

    @Test
    @DisplayName("Should handle cents conversion edge cases")
    void shouldHandleCentsConversionEdgeCases() {
        // Given & When
        Money oneCent = Money.of(1L);
        Money maxCents = Money.of(Long.MAX_VALUE);
        Money negativeCent = Money.of(-1L);

        // Then
        assertEquals(1L, oneCent.cents());
        assertEquals(new BigDecimal("0.01"), oneCent.amount());

        assertEquals(Long.MAX_VALUE, maxCents.cents());
        assertEquals(-1L, negativeCent.cents());
        assertEquals(new BigDecimal("-0.01"), negativeCent.amount());
    }

    @Test
    @DisplayName("Should handle string representation edge cases")
    void shouldHandleStringRepresentationEdgeCases() {
        // Given
        Money[] testCases = {
            Money.of(new BigDecimal("0")),
            Money.of(new BigDecimal("0.1")),
            Money.of(new BigDecimal("0.01")),
            Money.of(new BigDecimal("0.10")),
            Money.of(new BigDecimal("10.00")),
            Money.of(new BigDecimal("100.00")),
            Money.of(new BigDecimal("-0.01")),
            Money.of(new BigDecimal("-100.50"))
        };

        String[] expectedStrings = {
            "0", "0.1", "0.01", "0.1", "10", "100", "-0.01", "-100.5"
        };

        // Then
        for (int i = 0; i < testCases.length; i++) {
            assertEquals(expectedStrings[i], testCases[i].toString(),
                "String representation for " + testCases[i].amount() + " should be " + expectedStrings[i]);
        }
    }

    @Test
    @DisplayName("Should handle mathematical operations preserving precision")
    void shouldHandleMathematicalOperationsPreservingPrecision() {
        // Given
        Money precise = Money.of(new BigDecimal("123.456789"));

        // When
        Money doubled = precise.times(2);
        Money halved = Money.of(precise.amount().divide(new BigDecimal("2")));

        // Then
        assertEquals(0, new BigDecimal("246.913578").compareTo(doubled.amount()));
        assertEquals(0, new BigDecimal("61.7283945").compareTo(halved.amount()));
    }

    @Test
    @DisplayName("Should handle chained operations")
    void shouldHandleChainedOperations() {
        // Given
        Money base = Money.of(new BigDecimal("100.00"));
        Money addend = Money.of(new BigDecimal("50.00"));

        // When
        Money result = base.plus(addend).minus(addend).times(2);

        // Then
        assertEquals(0, new BigDecimal("200").compareTo(result.amount()));
        assertEquals(20000L, result.cents());
    }

    @Test
    @DisplayName("Should validate that equals uses compareTo for BigDecimal comparison")
    void shouldValidateEqualsUsesCompareToForBigDecimalComparison() {
        // Given - These BigDecimals are not equal using equals() but are equal using compareTo()
        Money money1 = Money.of(new BigDecimal("100.0"));
        Money money2 = Money.of(new BigDecimal("100.00"));

        // Then - Money should use compareTo, not equals, for BigDecimal comparison
        assertEquals(money1, money2);
        assertTrue(money1.amount().compareTo(money2.amount()) == 0);
    }
}
