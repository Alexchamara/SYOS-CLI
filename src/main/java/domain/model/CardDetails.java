package domain.model;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Card details for payment processing with validation rules
 */
public final class CardDetails {
    private final String number;
    private final int expMonth;
    private final int expYear;
    private final String cvv;

    public CardDetails(String number, int expMonth, int expYear, String cvv) {
        this.number = number;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvv = cvv;
    }

    public String number() { return number; }
    public int expMonth() { return expMonth; }
    public int expYear() { return expYear; }
    public String cvv() { return cvv; }

    /**
     * Validates card details according to business rules (no Luhn check)
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return isValidNumber() && isValidCvv() && isValidExpiry();
    }

    private boolean isValidNumber() {
        // Must be exactly 16 digits
        return number != null && number.matches("\\d{16}");
    }

    private boolean isValidCvv() {
        // Must be exactly 3 digits
        return cvv != null && cvv.matches("\\d{3}");
    }

    private boolean isValidExpiry() {
        // Month must be 1-12 and expiry must be in the future
        if (expMonth < 1 || expMonth > 12) {
            return false;
        }
        
        YearMonth cardExpiry = YearMonth.of(expYear, expMonth);
        YearMonth currentMonth = YearMonth.now();
        
        return cardExpiry.isAfter(currentMonth);
    }

    /**
     * Returns the last 4 digits of the card number for storage
     */
    public String getLast4() {
        if (number == null || number.length() < 4) {
            return "";
        }
        return number.substring(number.length() - 4);
    }
}
