package application.usecase;

import domain.repository.UserRepository;
import domain.user.Role;
import infrastructure.security.PasswordEncoder;

/**
 * Unified authentication use case for all user types (CASHIER, MANAGER, USER)
 */
public final class AuthenticationUseCase {

    public record Session(String identifier, Role role, Long userId) { }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Login for staff users (CASHIER, MANAGER) using username/password
     */
    public Session loginStaff(String username, String password) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return new Session(user.username(), user.role(), null);
    }

    /**
     * Login for web users (USER role) using email/password
     */
    public Session loginWebUser(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        String storedHash = userRepository.getPasswordHashByEmail(email);
        if (storedHash == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!passwordEncoder.matches(password, storedHash)) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        var user = userRepository.findWebCustomerByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return new Session(email, Role.USER, user.id());
    }

    /**
     * Register a new web user (USER role)
     */
    public long registerWebUser(String email, String password, String fullName) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (userRepository.findWebCustomerByEmail(email) != null) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        String passwordHash = passwordEncoder.hash(password);
        return userRepository.createWebCustomer(email, passwordHash, fullName);
    }
}
