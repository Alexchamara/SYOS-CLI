package domain.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.user.Role;

@DisplayName("Role Enum Tests")
class RoleTest {

    @Test
    @DisplayName("Should have all expected role values")
    void shouldHaveAllExpectedRoleValues() {
        // Given & When
        Role[] roles = Role.values();

        // Then
        assertEquals(3, roles.length);
        assertTrue(java.util.Arrays.asList(roles).contains(Role.CASHIER));
        assertTrue(java.util.Arrays.asList(roles).contains(Role.MANAGER));
        assertTrue(java.util.Arrays.asList(roles).contains(Role.USER));
    }

    @Test
    @DisplayName("Should get role by name")
    void shouldGetRoleByName() {
        // Given & When
        Role cashier = Role.valueOf("CASHIER");
        Role manager = Role.valueOf("MANAGER");
        Role user = Role.valueOf("USER");

        // Then
        assertEquals(Role.CASHIER, cashier);
        assertEquals(Role.MANAGER, manager);
        assertEquals(Role.USER, user);
    }

    @Test
    @DisplayName("Should throw exception for invalid role name")
    void shouldThrowExceptionForInvalidRoleName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> Role.valueOf("INVALID_ROLE"));
    }

    @Test
    @DisplayName("Should convert to string correctly")
    void shouldConvertToStringCorrectly() {
        // Then
        assertEquals("CASHIER", Role.CASHIER.toString());
        assertEquals("MANAGER", Role.MANAGER.toString());
        assertEquals("USER", Role.USER.toString());
    }

    @Test
    @DisplayName("Should support values method")
    void shouldSupportValuesMethod() {
        // When
        Role[] roles = Role.values();

        // Then
        assertNotNull(roles);
        assertTrue(roles.length > 0);
        assertEquals(3, roles.length);
    }
}
