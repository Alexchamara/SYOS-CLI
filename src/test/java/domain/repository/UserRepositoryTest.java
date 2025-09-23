package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.UserRepository;
import domain.user.User;

import java.util.Optional;

@DisplayName("UserRepository Domain Interface Tests")
class UserRepositoryTest {

    private UserRepository userRepository;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        user = mock(User.class);
    }

    @Test
    @DisplayName("Should define contract for upserting users")
    void shouldDefineContractForUpsertingUsers() {
        // When
        userRepository.upsert(user);

        // Then
        verify(userRepository).upsert(user);
    }

    @Test
    @DisplayName("Should define contract for finding by username")
    void shouldDefineContractForFindingByUsername() {
        // Given
        String username = "alice";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        Optional<User> found = userRepository.findByUsername(username);

        // Then
        assertTrue(found.isPresent());
        assertEquals(user, found.get());
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should define contract for creating web customer")
    void shouldDefineContractForCreatingWebCustomer() {
        // Given
        String email = "alice@example.com";
        String hash = "hashed";
        String fullName = "Alice Doe";
        when(userRepository.createWebCustomer(email, hash, fullName)).thenReturn(101L);

        // When
        long id = userRepository.createWebCustomer(email, hash, fullName);

        // Then
        assertEquals(101L, id);
        verify(userRepository).createWebCustomer(email, hash, fullName);
    }

    @Test
    @DisplayName("Should define contract for finding web customer by email")
    void shouldDefineContractForFindingWebCustomerByEmail() {
        // Given
        String email = "alice@example.com";
        when(userRepository.findWebCustomerByEmail(email)).thenReturn(user);

        // When
        User found = userRepository.findWebCustomerByEmail(email);

        // Then
        assertEquals(user, found);
        verify(userRepository).findWebCustomerByEmail(email);
    }

    @Test
    @DisplayName("Should define contract for getting password hash by email")
    void shouldDefineContractForGettingPasswordHashByEmail() {
        // Given
        String email = "alice@example.com";
        when(userRepository.getPasswordHashByEmail(email)).thenReturn("hashed");

        // When
        String hash = userRepository.getPasswordHashByEmail(email);

        // Then
        assertEquals("hashed", hash);
        verify(userRepository).getPasswordHashByEmail(email);
    }

    @Test
    @DisplayName("Should define contract for finding by id")
    void shouldDefineContractForFindingById() {
        // Given
        long id = 101L;
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // When
        Optional<User> found = userRepository.findById(id);

        // Then
        assertTrue(found.isPresent());
        assertEquals(user, found.get());
        verify(userRepository).findById(id);
    }

    @Test
    @DisplayName("Should define contract for getting password hash by username")
    void shouldDefineContractForGettingPasswordHashByUsername() {
        // Given
        String username = "alice";
        when(userRepository.getPasswordHashByUsername(username)).thenReturn("hashed");

        // When
        String hash = userRepository.getPasswordHashByUsername(username);

        // Then
        assertEquals("hashed", hash);
        verify(userRepository).getPasswordHashByUsername(username);
    }
}

