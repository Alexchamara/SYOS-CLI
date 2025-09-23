package domain.repository;

/**
 * Repository interface for payment management
 */
public interface PaymentRepository {
    /**
     * Save card payment details
     * @param orderId order ID
     * @param last4 last 4 digits of card number
     * @param authRef authorization reference
     */
    void saveCard(long orderId, String last4, String authRef);
}
