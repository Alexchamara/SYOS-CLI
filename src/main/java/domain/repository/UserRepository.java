package domain.repository;

import java.util.Optional;

/**
 * Unified repository interface for all user types (CASHIER, MANAGER, USER)
 * Combines functionality from UserRepository and CustomerRepository
 */
public interface UserRepository {
    // Username-based operations (for CASHIER, MANAGER, and internal USER accounts)
    Optional<domain.user.User> findByUsername(String username);
    void upsert(domain.user.User user);

    // Email-based operations (for web customers with USER role)
    long createWebCustomer(String email, String passwordHash, String fullName);
    domain.user.User findWebCustomerByEmail(String email);
    String getPasswordHashByEmail(String email);

    // Common operations
    Optional<domain.user.User> findById(long id);
    String getPasswordHashByUsername(String username);
}
