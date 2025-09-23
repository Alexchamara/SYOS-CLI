package infrastructure.persistence;

import domain.repository.CartRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of CartRepository
 */
public final class JdbcCartRepository implements CartRepository {
    private final DataSource dataSource;

    public JdbcCartRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long getOrCreateCart(long userId) {
        try (Connection con = dataSource.getConnection()) {
            // Try to find existing cart
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id FROM carts WHERE user_id = ?")) {
                ps.setLong(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }

            // Create new cart if not found
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO carts (user_id) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    throw new RuntimeException("Failed to get generated cart ID");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get or create cart: " + e.getMessage(), e);
        }
    }

    @Override
    public void upsertItem(long cartId, String productCode, int qty) {
        try (Connection con = dataSource.getConnection()) {
            if (qty <= 0) {
                removeItem(cartId, productCode);
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cart_items (cart_id, product_code, quantity) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)")) {
                ps.setLong(1, cartId);
                ps.setString(2, productCode);
                ps.setInt(3, qty);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upsert cart item", e);
        }
    }

    @Override
    public void removeItem(long cartId, String productCode) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM cart_items WHERE cart_id = ? AND product_code = ?")) {
            ps.setLong(1, cartId);
            ps.setString(2, productCode);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove cart item", e);
        }
    }

    @Override
    public List<CartItem> items(long cartId) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT product_code, quantity FROM cart_items WHERE cart_id = ?")) {
            ps.setLong(1, cartId);

            List<CartItem> items = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                            rs.getString("product_code"),
                            rs.getInt("quantity")
                    ));
                }
            }
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cart items", e);
        }
    }

    @Override
    public void clearCart(long cartId) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM cart_items WHERE cart_id = ?")) {
            ps.setLong(1, cartId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear cart", e);
        }
    }
}
