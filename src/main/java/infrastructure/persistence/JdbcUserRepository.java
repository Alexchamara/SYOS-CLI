package infrastructure.persistence;

import domain.repository.UserRepository;
import domain.user.Role;
import domain.user.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

/**
 * Unified JDBC implementation for all user types (CASHIER, MANAGER, USER)
 */
public final class JdbcUserRepository implements UserRepository {
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, email, full_name, role FROM users WHERE username=?";
        try (var con = dataSource.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by username: " + e.getMessage(), e);
        }
    }

    @Override
    public void upsert(User user) {
        String sql = """
        INSERT INTO users(username,password_hash,email,full_name,role) VALUES(?,?,?,?,?)
        ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), email=VALUES(email), full_name=VALUES(full_name), role=VALUES(role)
        """;
        try (var con = dataSource.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.email());
            ps.setString(4, user.fullName());
            ps.setString(5, user.role().name());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upsert user: " + e.getMessage(), e);
        }
    }

    @Override
    public long createWebCustomer(String email, String passwordHash, String fullName) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO users (email, password_hash, full_name, role, username) VALUES (?, ?, ?, 'USER', ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, email);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new RuntimeException("Failed to get generated user ID");
            }
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Failed to get generated user ID")) {
                throw e;
            }
            throw new RuntimeException("Failed to create web customer: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create web customer: " + e.getMessage(), e);
        }
    }

    @Override
    public User findWebCustomerByEmail(String email) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, username, password_hash, email, full_name, role FROM users WHERE email = ? AND role = 'USER'")) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            Role.valueOf(rs.getString("role"))
                    );
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find web customer by email: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPasswordHashByEmail(String email) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT password_hash FROM users WHERE email = ? AND role = 'USER'")) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get password hash by email: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT id, username, password_hash, email, full_name, role FROM users WHERE id = ?";
        try (var con = dataSource.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPasswordHashByUsername(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (var con = dataSource.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get password hash by username: " + e.getMessage(), e);
        }
    }
}
