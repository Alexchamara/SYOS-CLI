package domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import static org.junit.jupiter.api.Assertions.*;

import domain.shared.Code;

@DisplayName("Code Value Object Tests")
class CodeTest {

    @Test
    @DisplayName("Should create Code with valid value")
    void shouldCreateCodeWithValidValue() {
        // Given
        String value = "PROD001";

        // When
        Code code = new Code(value);

        // Then
        assertEquals(value, code.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for null, empty or blank codes")
    void shouldThrowExceptionForInvalidCodes(String invalidCode) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Code(invalidCode));
        assertEquals("Product Code cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should trim whitespace from code")
    void shouldTrimWhitespaceFromCode() {
        // Given
        String codeWithWhitespace = "  PROD001  ";

        // When
        Code code = new Code(codeWithWhitespace);

        // Then
        assertEquals("PROD001", code.value());
    }

    @Test
    @DisplayName("Should convert code to uppercase")
    void shouldConvertCodeToUppercase() {
        // Given
        String lowercaseCode = "prod001";

        // When
        Code code = new Code(lowercaseCode);

        // Then
        assertEquals("PROD001", code.value());
    }

    @Test
    @DisplayName("Should be equal when values are same (case insensitive)")
    void shouldBeEqualWhenValuesAreSameCaseInsensitive() {
        // Given
        Code code1 = new Code("PROD001");
        Code code2 = new Code("prod001");

        // Then
        assertEquals(code1, code2);
        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values differ")
    void shouldNotBeEqualWhenValuesDiffer() {
        // Given
        Code code1 = new Code("PROD001");
        Code code2 = new Code("PROD002");

        // Then
        assertNotEquals(code1, code2);
    }

    @Test
    @DisplayName("Should return value as string representation")
    void shouldReturnValueAsStringRepresentation() {
        // Given
        String value = "PROD001";
        Code code = new Code(value);

        // When
        String result = code.toString();

        // Then
        assertEquals(value, result);
    }

    @Test
    @DisplayName("Should handle mixed case inputs")
    void shouldHandleMixedCaseInputs() {
        // Given
        Code upperCode = new Code("PROD001");
        Code lowerCode = new Code("prod001");
        Code mixedCode = new Code("Prod001");

        // Then
        assertEquals(upperCode, lowerCode);
        assertEquals(lowerCode, mixedCode);
        assertEquals(upperCode, mixedCode);
    }
}
