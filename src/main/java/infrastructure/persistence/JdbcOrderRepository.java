package infrastructure.persistence;

import application.usecase.QuoteUseCase;
import domain.repository.OrderRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * JDBC implementation of OrderRepository
 */
public final class JdbcOrderRepository implements OrderRepository {
    private final DataSource dataSource;

    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long nextBillSerial(String type) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT next_val FROM bill_number WHERE scope = ? FOR UPDATE")) {
            ps.setString(1, type);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Unknown bill type scope: " + type);
                }

                long current = rs.getLong(1);
                long next = current + 1;

                try (PreparedStatement up = con.prepareStatement(
                        "UPDATE bill_number SET next_val = ? WHERE scope = ?")) {
                    up.setLong(1, next);
                    up.setString(2, type);
                    up.executeUpdate();
                }

                return current;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get next bill serial", e);
        }
    }

    @Override
    public long savePreview(String type, String location, Long userId, QuoteUseCase.Quote quote) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO orders (bill_serial, type, location, user_id, total_gross, discount, total_net, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'PREVIEW')",
                     Statement.RETURN_GENERATED_KEYS)) {

            // Get the bill serial first, outside the main INSERT
            long billSerial = nextBillSerial(type);

            ps.setLong(1, billSerial);
            ps.setString(2, type);
            ps.setString(3, location);
            if (userId != null) {
                ps.setLong(4, userId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setBigDecimal(5, quote.subtotal().amount());
            ps.setBigDecimal(6, quote.discount().amount());
            ps.setBigDecimal(7, quote.total().amount());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new RuntimeException("Failed to get generated order ID");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save preview order: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveLines(long orderId, java.util.List<domain.billing.BillLine> lines) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO order_lines (order_id, product_code, name, unit_price, qty, line_total) " +
                     "VALUES (?, ?, ?, ?, ?, ?)")) {

            for (domain.billing.BillLine line : lines) {
                ps.setLong(1, orderId);
                ps.setString(2, line.productCode().value());
                ps.setString(3, line.name());
                ps.setBigDecimal(4, line.unitPrice().amount());
                ps.setInt(5, line.qty().value());
                ps.setBigDecimal(6, line.lineTotal().amount());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save order lines", e);
        }
    }

    @Override
    public void saveFinal(long orderId, QuoteUseCase.Quote quote) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE orders SET status = 'FINAL', total_gross = ?, discount = ?, total_net = ? WHERE id = ?")) {

            ps.setBigDecimal(1, quote.subtotal().amount());
            ps.setBigDecimal(2, quote.discount().amount());
            ps.setBigDecimal(3, quote.total().amount());
            ps.setLong(4, orderId);

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to finalize order", e);
        }
    }
}
