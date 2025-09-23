package domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import domain.model.CardDetails;

@DisplayName("CardDetails Domain Model Tests")
class CardDetailsTest {

    @Test
    @DisplayName("Should create CardDetails with valid parameters")
    void shouldCreateCardDetailsWithValidParameters() {
        // Given
        String cardNumber = "4111111111111111";
        int expMonth = 12;
        int expYear = 2025;
        String cvv = "123";

        // When
        CardDetails cardDetails = new CardDetails(cardNumber, expMonth, expYear, cvv);

        // Then
        assertEquals(cardNumber, cardDetails.number());
        assertEquals(expMonth, cardDetails.expMonth());
        assertEquals(expYear, cardDetails.expYear());
        assertEquals(cvv, cardDetails.cvv());
        assertTrue(cardDetails.isValid());
    }

    @Test
    @DisplayName("Should validate card number correctly")
    void shouldValidateCardNumberCorrectly() {
        // Given
        CardDetails validCard = new CardDetails("4111111111111111", 12, 2025, "123");
        CardDetails invalidCard = new CardDetails("411111111111111", 12, 2025, "123"); // 15 digits

        // Then
        assertTrue(validCard.isValid());
        assertFalse(invalidCard.isValid());
    }

    @Test
    @DisplayName("Should validate CVV correctly")
    void shouldValidateCvvCorrectly() {
        // Given
        CardDetails validCard = new CardDetails("4111111111111111", 12, 2025, "123");
        CardDetails invalidCard = new CardDetails("4111111111111111", 12, 2025, "12"); // 2 digits

        // Then
        assertTrue(validCard.isValid());
        assertFalse(invalidCard.isValid());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 12})
    @DisplayName("Should accept valid months")
    void shouldAcceptValidMonths(int month) {
        // When
        CardDetails cardDetails = new CardDetails("4111111111111111", month, 2025, "123");

        // Then
        assertEquals(month, cardDetails.expMonth());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1, 15})
    @DisplayName("Should reject invalid months")
    void shouldRejectInvalidMonths(int invalidMonth) {
        // When
        CardDetails cardDetails = new CardDetails("4111111111111111", invalidMonth, 2025, "123");

        // Then
        assertFalse(cardDetails.isValid());
    }

    @Test
    @DisplayName("Should handle future expiry dates")
    void shouldHandleFutureExpiryDates() {
        // Given
        int currentYear = java.time.Year.now().getValue();
        CardDetails futureCard = new CardDetails("4111111111111111", 12, currentYear + 1, "123");

        // Then
        assertTrue(futureCard.isValid());
    }

    @Test
    @DisplayName("Should reject past expiry dates")
    void shouldRejectPastExpiryDates() {
        // Given
        CardDetails pastCard = new CardDetails("4111111111111111", 1, 2020, "123");

        // Then
        assertFalse(pastCard.isValid());
    }

    @ParameterizedTest
    @CsvSource({
        "4111111111111111, true",
        "5555555555554444, true",
        "411111111111111, false",  // 15 digits
        "41111111111111111, false", // 17 digits
        "411111111111111a, false"   // contains letter
    })
    @DisplayName("Should validate different card numbers")
    void shouldValidateDifferentCardNumbers(String cardNumber, boolean expectedValid) {
        // When
        CardDetails cardDetails = new CardDetails(cardNumber, 12, 2025, "123");

        // Then
        assertEquals(expectedValid, cardDetails.isValid());
    }

    @ParameterizedTest
    @CsvSource({
        "123, true",
        "000, true",
        "999, true",
        "12, false",   // 2 digits
        "1234, false", // 4 digits
        "12a, false"   // contains letter
    })
    @DisplayName("Should validate different CVV values")
    void shouldValidateDifferentCvvValues(String cvv, boolean expectedValid) {
        // When
        CardDetails cardDetails = new CardDetails("4111111111111111", 12, 2025, cvv);

        // Then
        assertEquals(expectedValid, cardDetails.isValid());
    }
}
