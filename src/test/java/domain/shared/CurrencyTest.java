package domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.shared.Currency;
import domain.shared.Money;
import java.math.BigDecimal;

@DisplayName("Currency Utility Class Tests")
class CurrencyTest {

    @Test
    @DisplayName("Should have correct currency constants")
    void shouldHaveCorrectCurrencyConstants() {
        // Then
        assertEquals("Rs.", Currency.SYMBOL);
        assertEquals("LKR", Currency.CODE);
        assertNotNull(Currency.LOCALE);
    }

    @Test
    @DisplayName("Should format money amount with currency")
    void shouldFormatMoneyAmountWithCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        String formatted = Currency.format(amount);

        // Then
        assertNotNull(formatted);
        assertTrue(formatted.contains("100"));
    }

    @Test
    @DisplayName("Should format money amount simply")
    void shouldFormatMoneyAmountSimply() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        String formatted = Currency.formatSimple(amount);

        // Then
        assertEquals("Rs. 100.50", formatted);
    }

    @Test
    @DisplayName("Should format Money object simply")
    void shouldFormatMoneyObjectSimply() {
        // Given
        Money money = Money.of(new BigDecimal("100.50"));

        // When
        String formatted = Currency.formatSimple(money);

        // Then
        assertEquals("Rs. 100.50", formatted);
    }

    @Test
    @DisplayName("Should handle zero amounts")
    void shouldHandleZeroAmounts() {
        // Given
        BigDecimal zero = BigDecimal.ZERO;

        // When
        String formatted = Currency.formatSimple(zero);

        // Then
        assertEquals("Rs. 0", formatted);
    }

    @Test
    @DisplayName("Should handle large amounts")
    void shouldHandleLargeAmounts() {
        // Given
        BigDecimal largeAmount = new BigDecimal("99999.99");

        // When
        String formatted = Currency.formatSimple(largeAmount);

        // Then
        assertEquals("Rs. 99999.99", formatted);
    }
}
