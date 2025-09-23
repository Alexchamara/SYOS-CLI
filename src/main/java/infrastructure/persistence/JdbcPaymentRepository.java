package infrastructure.persistence;

import domain.repository.PaymentRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * JDBC implementation of PaymentRepository
 */
public final class JdbcPaymentRepository implements PaymentRepository {
    private final DataSource dataSource;

    public JdbcPaymentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveCard(long orderId, String last4, String authRef) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO payments (order_id, payment_type, card_last4, auth_reference, amount) VALUES (?, 'CARD', ?, ?, 0)")) {

            ps.setLong(1, orderId);
            ps.setString(2, last4);
            ps.setString(3, authRef);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save card payment", e);
        }
    }
}
