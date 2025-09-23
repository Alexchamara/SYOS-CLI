package infrastructure.persistence;

import domain.pricing.Discount;
import domain.repository.DiscountRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL implementation of DiscountRepository
 */
public class SqlDiscountRepository implements DiscountRepository {
    private final DataSource ds;

    public SqlDiscountRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Discount save(Discount discount) {
        String sql = """
            INSERT INTO discounts (batch_id, discount_type, discount_value, start_date, 
                                 end_date, is_active, description, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, discount.getBatchId());
            stmt.setString(2, discount.getType().name());
            stmt.setBigDecimal(3, discount.getValue());
            stmt.setDate(4, Date.valueOf(discount.getStartDate()));
            stmt.setDate(5, Date.valueOf(discount.getEndDate()));
            stmt.setBoolean(6, discount.isActive());
            stmt.setString(7, discount.getDescription());
            stmt.setLong(8, discount.getCreatedBy());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Discount(id, discount.getBatchId(), discount.getType(),
                                      discount.getValue(), discount.getStartDate(),
                                      discount.getEndDate(), discount.isActive(),
                                      discount.getDescription(), discount.getCreatedBy());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save discount", e);
        }
        throw new RuntimeException("Failed to get generated discount ID");
    }

    @Override
    public Discount update(Discount discount) {
        String sql = """
            UPDATE discounts 
            SET discount_type = ?, discount_value = ?, start_date = ?, end_date = ?, 
                is_active = ?, description = ?
            WHERE id = ?
        """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, discount.getType().name());
            stmt.setBigDecimal(2, discount.getValue());
            stmt.setDate(3, Date.valueOf(discount.getStartDate()));
            stmt.setDate(4, Date.valueOf(discount.getEndDate()));
            stmt.setBoolean(5, discount.isActive());
            stmt.setString(6, discount.getDescription());
            stmt.setLong(7, discount.getId());

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new RuntimeException("Discount not found for update: " + discount.getId());
            }

            return discount;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update discount", e);
        }
    }

    @Override
    public Optional<Discount> findById(long id) {
        String sql = """
            SELECT id, batch_id, discount_type, discount_value, start_date, 
                   end_date, is_active, description, created_by
            FROM discounts WHERE id = ?
        """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDiscount(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find discount by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Discount> findByBatchId(long batchId) {
        String sql = """
            SELECT id, batch_id, discount_type, discount_value, start_date, 
                   end_date, is_active, description, created_by
            FROM discounts WHERE batch_id = ?
            ORDER BY created_at DESC
        """;

        return executeQueryForDiscountList(sql, stmt -> stmt.setLong(1, batchId));
    }

    @Override
    public List<Discount> findActiveDiscountsForBatch(long batchId, LocalDate date) {
        String sql = """
            SELECT id, batch_id, discount_type, discount_value, start_date, 
                   end_date, is_active, description, created_by
            FROM discounts 
            WHERE batch_id = ? AND is_active = true 
                  AND start_date <= ? AND end_date >= ?
            ORDER BY created_at DESC
        """;

        return executeQueryForDiscountList(sql, stmt -> {
            stmt.setLong(1, batchId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));
        });
    }

    @Override
    public List<Discount> findActiveDiscountsInDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT id, batch_id, discount_type, discount_value, start_date, 
                   end_date, is_active, description, created_by
            FROM discounts 
            WHERE is_active = true 
                  AND start_date <= ? AND end_date >= ?
            ORDER BY start_date, batch_id
        """;

        return executeQueryForDiscountList(sql, stmt -> {
            stmt.setDate(1, Date.valueOf(endDate));
            stmt.setDate(2, Date.valueOf(startDate));
        });
    }

    @Override
    public boolean delete(long id) {
        String sql = "DELETE FROM discounts WHERE id = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int deleted = stmt.executeUpdate();
            return deleted > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete discount", e);
        }
    }

    @Override
    public List<Discount> findAll() {
        String sql = """
            SELECT id, batch_id, discount_type, discount_value, start_date, 
                   end_date, is_active, description, created_by
            FROM discounts 
            ORDER BY created_at DESC
        """;

        return executeQueryForDiscountList(sql, stmt -> {});
    }

    @Override
    public List<Discount> findByProductCode(String productCode) {
        String sql = """
            SELECT d.id, d.batch_id, d.discount_type, d.discount_value, d.start_date, 
                   d.end_date, d.is_active, d.description, d.created_by
            FROM discounts d
            JOIN batch b ON d.batch_id = b.id
            WHERE b.product_code = ?
            ORDER BY d.created_at DESC
        """;

        return executeQueryForDiscountList(sql, stmt -> stmt.setString(1, productCode));
    }

    private List<Discount> executeQueryForDiscountList(String sql, StatementParameterSetter parameterSetter) {
        List<Discount> discounts = new ArrayList<>();

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            parameterSetter.setParameters(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    discounts.add(mapResultSetToDiscount(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute discount query", e);
        }

        return discounts;
    }

    private Discount mapResultSetToDiscount(ResultSet rs) throws SQLException {
        return new Discount(
            rs.getLong("id"),
            rs.getLong("batch_id"),
            Discount.DiscountType.valueOf(rs.getString("discount_type")),
            rs.getBigDecimal("discount_value"),
            rs.getDate("start_date").toLocalDate(),
            rs.getDate("end_date").toLocalDate(),
            rs.getBoolean("is_active"),
            rs.getString("description"),
            rs.getLong("created_by")
        );
    }

    @FunctionalInterface
    private interface StatementParameterSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }
}
