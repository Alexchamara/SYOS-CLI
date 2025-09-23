package domain.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.user.User;
import domain.user.Role;

@DisplayName("User Domain Entity Tests")
class UserTest {

    @Test
    @DisplayName("Should create User with valid parameters")
    void shouldCreateUserWithValidParameters() {
        // Given
        long id = 1L;
        String username = "john.doe";
        String passwordHash = "hashedPassword123";
        String email = "john.doe@example.com";
        String fullName = "John Doe";
        Role role = Role.CASHIER;

        // When
        User user = new User(id, username, passwordHash, email, fullName, role);

        // Then
        assertEquals(id, user.id());
        assertEquals(username, user.username());
        assertEquals(passwordHash, user.passwordHash());
        assertEquals(email, user.email());
        assertEquals(fullName, user.fullName());
        assertEquals(role, user.role());
    }

    @Test
    @DisplayName("Should create User without full name")
    void shouldCreateUserWithoutFullName() {
        // Given
        long id = 1L;
        String username = "john.doe";
        String passwordHash = "hashedPassword123";
        String email = "john.doe@example.com";
        Role role = Role.CASHIER;

        // When
        User user = new User(id, username, passwordHash, email, role);

        // Then
        assertEquals(id, user.id());
        assertEquals(username, user.username());
        assertEquals(passwordHash, user.passwordHash());
        assertEquals(email, user.email());
        assertNull(user.fullName());
        assertEquals(role, user.role());
    }

    @Test
    @DisplayName("Should handle different user roles")
    void shouldHandleDifferentUserRoles() {
        // Given
        long id = 1L;
        String username = "user";
        String passwordHash = "hash";
        String email = "user@example.com";

        // When
        User cashier = new User(id, username, passwordHash, email, Role.CASHIER);
        User manager = new User(id + 1, username + "2", passwordHash, email, Role.MANAGER);
        User regularUser = new User(id + 2, username + "3", passwordHash, email, Role.USER);

        // Then
        assertEquals(Role.CASHIER, cashier.role());
        assertEquals(Role.MANAGER, manager.role());
        assertEquals(Role.USER, regularUser.role());
    }

    @Test
    @DisplayName("Should handle different user IDs")
    void shouldHandleDifferentUserIds() {
        // Given
        String username = "user";
        String passwordHash = "hash";
        String email = "user@example.com";
        Role role = Role.CASHIER;

        // When
        User user1 = new User(1L, username, passwordHash, email, role);
        User user2 = new User(2L, username, passwordHash, email, role);

        // Then
        assertEquals(1L, user1.id());
        assertEquals(2L, user2.id());
        assertNotEquals(user1.id(), user2.id());
    }

    @Test
    @DisplayName("Should handle null full name")
    void shouldHandleNullFullName() {
        // When
        User user = new User(1L, "username", "hash", "email@example.com", null, Role.CASHIER);

        // Then
        assertNull(user.fullName());
        assertEquals("username", user.username());
    }

    @Test
    @DisplayName("Should handle empty email")
    void shouldHandleEmptyEmail() {
        // When
        User user = new User(1L, "username", "hash", "", "Full Name", Role.CASHIER);

        // Then
        assertEquals("", user.email());
        assertEquals("username", user.username());
    }
}
