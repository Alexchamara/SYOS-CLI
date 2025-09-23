package domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import domain.shared.Quantity;

@DisplayName("Quantity Value Object Tests")
class QuantityTest {

    @Test
    @DisplayName("Should create Quantity with valid value")
    void shouldCreateQuantityWithValidValue() {
        // Given
        int value = 10;

        // When
        Quantity quantity = new Quantity(value);

        // Then
        assertEquals(value, quantity.value());
    }

    @Test
    @DisplayName("Should throw exception when quantity is negative")
    void shouldThrowExceptionWhenQuantityIsNegative() {
        // Given
        int negativeValue = -5;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Quantity(negativeValue));
        assertEquals("Quantity value cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero quantity")
    void shouldAllowZeroQuantity() {
        // Given
        int zeroValue = 0;

        // When
        Quantity quantity = new Quantity(zeroValue);

        // Then
        assertEquals(0, quantity.value());
    }

    @Test
    @DisplayName("Should add quantities")
    void shouldAddQuantities() {
        // Given
        Quantity quantity = new Quantity(10);

        // When
        Quantity result = quantity.plus(5);

        // Then
        assertEquals(15, result.value());
        assertEquals(10, quantity.value()); // Original unchanged
    }

    @Test
    @DisplayName("Should subtract quantities")
    void shouldSubtractQuantities() {
        // Given
        Quantity quantity = new Quantity(10);

        // When
        Quantity result = quantity.minus(3);

        // Then
        assertEquals(7, result.value());
        assertEquals(10, quantity.value()); // Original unchanged
    }

    @Test
    @DisplayName("Should throw exception when subtracting more than available")
    void shouldThrowExceptionWhenSubtractingMoreThanAvailable() {
        // Given
        Quantity quantity = new Quantity(5);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> quantity.minus(10));
        assertEquals("Insufficient quantity", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle subtraction resulting in zero")
    void shouldHandleSubtractionResultingInZero() {
        // Given
        Quantity quantity = new Quantity(5);

        // When
        Quantity result = quantity.minus(5);

        // Then
        assertEquals(0, result.value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 100, 1000})
    @DisplayName("Should create quantities with different valid values")
    void shouldCreateQuantitiesWithDifferentValidValues(int value) {
        // When
        Quantity quantity = new Quantity(value);

        // Then
        assertEquals(value, quantity.value());
    }

    @ParameterizedTest
    @CsvSource({
        "10, 5, 15",
        "0, 10, 10",
        "100, 50, 150"
    })
    @DisplayName("Should correctly add different quantities")
    void shouldCorrectlyAddDifferentQuantities(int initial, int toAdd, int expected) {
        // Given
        Quantity quantity = new Quantity(initial);

        // When
        Quantity result = quantity.plus(toAdd);

        // Then
        assertEquals(expected, result.value());
    }

    @ParameterizedTest
    @CsvSource({
        "10, 5, 5",
        "100, 50, 50",
        "20, 20, 0"
    })
    @DisplayName("Should correctly subtract different quantities")
    void shouldCorrectlySubtractDifferentQuantities(int initial, int toSubtract, int expected) {
        // Given
        Quantity quantity = new Quantity(initial);

        // When
        Quantity result = quantity.minus(toSubtract);

        // Then
        assertEquals(expected, result.value());
    }

    @Test
    @DisplayName("Should convert to string representation")
    void shouldConvertToStringRepresentation() {
        // Given
        Quantity quantity = new Quantity(42);

        // When
        String result = quantity.toString();

        // Then
        assertEquals("42", result);
    }
}
