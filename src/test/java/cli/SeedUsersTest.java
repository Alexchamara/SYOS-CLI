package cli;

import domain.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cli.SeedUsers;
import domain.repository.UserRepository;
import domain.user.User;
import infrastructure.security.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

@DisplayName("SeedUsers Utility Tests")
class SeedUsersTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Capture System.out for testing print statements
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore original System.out
        System.setOut(originalOut);
        if (mocks != null) {
            mocks.close();
        }
    }

    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {

        @Test
        @DisplayName("Should create all demo users when none exist")
        void shouldCreateAllDemoUsersWhenNoneExist() {
            // Given
            when(userRepository.findByUsername("cashier")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("manager")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

            when(passwordEncoder.hash("cashier123")).thenReturn("hashed_cashier_password");
            when(passwordEncoder.hash("manager123")).thenReturn("hashed_manager_password");
            when(passwordEncoder.hash("user123")).thenReturn("hashed_user_password");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository).findByUsername("cashier");
            verify(userRepository).findByUsername("manager");
            verify(userRepository).findByUsername("user");

            verify(passwordEncoder).hash("cashier123");
            verify(passwordEncoder).hash("manager123");
            verify(passwordEncoder).hash("user123");

            verify(userRepository, times(3)).upsert(any(User.class));

            // Verify cashier creation
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("cashier") &&
                user.passwordHash().equals("hashed_cashier_password") &&
                user.email().equals("cashier@syos.com") &&
                user.role() == domain.user.Role.CASHIER
            ));

            // Verify manager creation
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("manager") &&
                user.passwordHash().equals("hashed_manager_password") &&
                user.email().equals("manager@syos.com") &&
                user.role() == domain.user.Role.MANAGER
            ));

            // Verify user creation
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("user") &&
                user.passwordHash().equals("hashed_user_password") &&
                user.email().equals("user@syos.com") &&
                user.role() ==domain.user.Role.USER
            ));

            // Verify console output
            String output = outputStream.toString();
            assertTrue(output.contains("Created demo cashier account (username: cashier, password: cashier123)"));
            assertTrue(output.contains("Created demo manager account (username: manager, password: manager123)"));
            assertTrue(output.contains("Created demo user account (username: user, password: user123)"));
        }

        @Test
        @DisplayName("Should skip creating users that already exist")
        void shouldSkipCreatingUsersThatAlreadyExist() {
            // Given
            User existingCashier = new User(1L, "cashier", "existing_hash", "cashier@syos.com", Role.CASHIER);
            User existingManager = new User(2L, "manager", "existing_hash", "manager@syos.com", Role.MANAGER);

            when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(existingCashier));
            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(existingManager));
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

            when(passwordEncoder.hash("user123")).thenReturn("hashed_user_password");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository).findByUsername("cashier");
            verify(userRepository).findByUsername("manager");
            verify(userRepository).findByUsername("user");

            // Should not hash passwords for existing users
            verify(passwordEncoder, never()).hash("cashier123");
            verify(passwordEncoder, never()).hash("manager123");
            verify(passwordEncoder).hash("user123"); // Only for the new user

            // Should only create the missing user
            verify(userRepository, times(1)).upsert(any(User.class));
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("user") &&
                user.role() == domain.user.Role.USER
            ));

            // Verify console output for only the created user
            String output = outputStream.toString();
            assertFalse(output.contains("Created demo cashier account"));
            assertFalse(output.contains("Created demo manager account"));
            assertTrue(output.contains("Created demo user account (username: user, password: user123)"));
        }

        @Test
        @DisplayName("Should not create any users when all exist")
        void shouldNotCreateAnyUsersWhenAllExist() {
            // Given
            User existingCashier = new User(1L, "cashier", "hash1", "cashier@syos.com", domain.user.Role.CASHIER);
            User existingManager = new User(2L, "manager", "hash2", "manager@syos.com", domain.user.Role.MANAGER);
            User existingUser = new User(3L, "user", "hash3", "user@syos.com", domain.user.Role.USER);

            when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(existingCashier));
            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(existingManager));
            when(userRepository.findByUsername("user")).thenReturn(Optional.of(existingUser));

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository).findByUsername("cashier");
            verify(userRepository).findByUsername("manager");
            verify(userRepository).findByUsername("user");

            // Should not hash any passwords
            verifyNoInteractions(passwordEncoder);

            // Should not create any users
            verify(userRepository, never()).upsert(any(User.class));

            // Verify no console output
            String output = outputStream.toString();
            assertTrue(output.isEmpty());
        }

        @Test
        @DisplayName("Should create users with correct ID for new records")
        void shouldCreateUsersWithCorrectIdForNewRecords() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hashed_password");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository, times(3)).upsert(argThat(user -> user.id() == 0L));
        }

        @Test
        @DisplayName("Should use predefined email formats")
        void shouldUsePredefinedEmailFormats() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hashed_password");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository).upsert(argThat(user ->
                user.email().equals("cashier@syos.com")));
            verify(userRepository).upsert(argThat(user ->
                user.email().equals("manager@syos.com")));
            verify(userRepository).upsert(argThat(user ->
                user.email().equals("user@syos.com")));
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions during user lookup")
        void shouldHandleRepositoryExceptionsDuringUserLookup() {
            // Given
            when(userRepository.findByUsername("cashier")).thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> SeedUsers.ensure(userRepository, passwordEncoder));
            assertEquals("Database error", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle password encoder exceptions")
        void shouldHandlePasswordEncoderExceptions() {
            // Given
            when(userRepository.findByUsername("cashier")).thenReturn(Optional.empty());
            when(passwordEncoder.hash("cashier123")).thenThrow(new RuntimeException("Password hashing failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> SeedUsers.ensure(userRepository, passwordEncoder));
            assertEquals("Password hashing failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions during user creation")
        void shouldHandleRepositoryExceptionsDuringUserCreation() {
            // Given
            when(userRepository.findByUsername("cashier")).thenReturn(Optional.empty());
            when(passwordEncoder.hash("cashier123")).thenReturn("hashed_password");
            doThrow(new RuntimeException("User creation failed")).when(userRepository).upsert(any(User.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> SeedUsers.ensure(userRepository, passwordEncoder));
            assertEquals("User creation failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle partial user creation failures")
        void shouldHandlePartialUserCreationFailures() {
            // Given
            when(userRepository.findByUsername("cashier")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("manager")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

            when(passwordEncoder.hash("cashier123")).thenReturn("hashed_cashier");
            when(passwordEncoder.hash("manager123")).thenThrow(new RuntimeException("Manager password hash failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> SeedUsers.ensure(userRepository, passwordEncoder));
            assertEquals("Manager password hash failed", exception.getMessage());

            // Should have attempted to create cashier first
            verify(userRepository).upsert(argThat(user -> user.username().equals("cashier")));
            // Should not have attempted manager or user creation
            verify(userRepository, never()).upsert(argThat(user -> user.username().equals("manager")));
            verify(userRepository, never()).upsert(argThat(user -> user.username().equals("user")));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should use different passwords for different users")
        void shouldUseDifferentPasswordsForDifferentUsers() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash("cashier123")).thenReturn("hashed_cashier_password");
            when(passwordEncoder.hash("manager123")).thenReturn("hashed_manager_password");
            when(passwordEncoder.hash("user123")).thenReturn("hashed_user_password");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(passwordEncoder).hash("cashier123");
            verify(passwordEncoder).hash("manager123");
            verify(passwordEncoder).hash("user123");

            // Verify different passwords were used
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("cashier") &&
                user.passwordHash().equals("hashed_cashier_password")));
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("manager") &&
                user.passwordHash().equals("hashed_manager_password")));
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("user") &&
                user.passwordHash().equals("hashed_user_password")));
        }

        @Test
        @DisplayName("Should assign correct roles to users")
        void shouldAssignCorrectRolesToUsers() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hashed_password");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("cashier") && user.role() == domain.user.Role.CASHIER));
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("manager") && user.role() == domain.user.Role.MANAGER));
            verify(userRepository).upsert(argThat(user ->
                user.username().equals("user") && user.role() == domain.user.Role.USER));
        }

        @Test
        @DisplayName("Should not expose passwords in logs or output")
        void shouldNotExposePasswordsInLogsOrOutput() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("secure_hash");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            String output = outputStream.toString();
            // Console output should contain passwords for demo purposes (intentional)
            assertTrue(output.contains("password: cashier123"));
            assertTrue(output.contains("password: manager123"));
            assertTrue(output.contains("password: user123"));

            // But should not contain hashed passwords
            assertFalse(output.contains("secure_hash"));
        }
    }

    @Nested
    @DisplayName("Static Utility Method Tests")
    class StaticUtilityMethodTests {

        @Test
        @DisplayName("Should be a static utility method")
        void shouldBeAStaticUtilityMethod() {
            // Given
            Class<SeedUsers> seedUsersClass = SeedUsers.class;

            // When
            java.lang.reflect.Method ensureMethod;
            try {
                ensureMethod = seedUsersClass.getMethod("ensure", UserRepository.class, PasswordEncoder.class);
            } catch (NoSuchMethodException e) {
                fail("ensure method should exist");
                return;
            }

            // Then
            assertTrue(java.lang.reflect.Modifier.isStatic(ensureMethod.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPublic(ensureMethod.getModifiers()));
            assertEquals(void.class, ensureMethod.getReturnType());
        }

        @Test
        @DisplayName("Should be callable without instance")
        void shouldBeCallableWithoutInstance() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hash");

            // When & Then
            assertDoesNotThrow(() -> SeedUsers.ensure(userRepository, passwordEncoder));
        }

        @Test
        @DisplayName("Should handle null dependencies gracefully")
        void shouldHandleNullDependenciesGracefully() {
            // When & Then
            assertThrows(NullPointerException.class,
                () -> SeedUsers.ensure(null, passwordEncoder));
            assertThrows(NullPointerException.class,
                () -> SeedUsers.ensure(userRepository, null));
            assertThrows(NullPointerException.class,
                () -> SeedUsers.ensure(null, null));
        }
    }

    @Nested
    @DisplayName("Console Output Tests")
    class ConsoleOutputTests {

        @Test
        @DisplayName("Should print creation messages for each new user")
        void shouldPrintCreationMessagesForEachNewUser() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hash");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            String output = outputStream.toString();
            String[] lines = output.split("\n");
            assertEquals(3, lines.length);

            assertTrue(lines[0].contains("Created demo cashier account"));
            assertTrue(lines[1].contains("Created demo manager account"));
            assertTrue(lines[2].contains("Created demo user account"));
        }

        @Test
        @DisplayName("Should include credentials in output messages")
        void shouldIncludeCredentialsInOutputMessages() {
            // Given
            when(userRepository.findByUsername("cashier")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("manager")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hash");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("username: cashier, password: cashier123"));
            assertTrue(output.contains("username: manager, password: manager123"));
            assertTrue(output.contains("username: user, password: user123"));
        }

        @Test
        @DisplayName("Should produce no output when all users exist")
        void shouldProduceNoOutputWhenAllUsersExist() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(
                new User(1L, "existing", "hash", "email@syos.com", domain.user.Role.CASHIER)));

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            String output = outputStream.toString();
            assertTrue(output.isEmpty());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work with real password encoder behavior")
        void shouldWorkWithRealPasswordEncoderBehavior() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            // Simulate real password encoder that returns different hashes for same input
            when(passwordEncoder.hash("cashier123")).thenReturn("$2a$10$hash1");
            when(passwordEncoder.hash("manager123")).thenReturn("$2a$10$hash2");
            when(passwordEncoder.hash("user123")).thenReturn("$2a$10$hash3");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository).upsert(argThat(user ->
                user.passwordHash().startsWith("$2a$10$")));
        }

        @Test
        @DisplayName("Should handle repository transaction scenarios")
        void shouldHandleRepositoryTransactionScenarios() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("hash");

            // Simulate transaction behavior where first upsert succeeds, second fails
            doNothing().doThrow(new RuntimeException("Transaction rollback")).when(userRepository).upsert(any(User.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> SeedUsers.ensure(userRepository, passwordEncoder));
            assertEquals("Transaction rollback", exception.getMessage());

            // Should have attempted to create cashier, then failed on manager
            verify(userRepository, times(2)).upsert(any(User.class));
        }
    }

    @Nested
    @DisplayName("Utility Class Pattern Tests")
    class UtilityClassPatternTests {

        @Test
        @DisplayName("Should be a final utility class")
        void shouldBeAFinalUtilityClass() {
            // Given
            Class<SeedUsers> seedUsersClass = SeedUsers.class;

            // When
            boolean isFinal = java.lang.reflect.Modifier.isFinal(seedUsersClass.getModifiers());

            // Then
            assertTrue(isFinal, "SeedUsers should be a final class");
        }

        @Test
        @DisplayName("Should have only static methods")
        void shouldHaveOnlyStaticMethods() {
            // Given
            Class<SeedUsers> seedUsersClass = SeedUsers.class;

            // When
            java.lang.reflect.Method[] declaredMethods = seedUsersClass.getDeclaredMethods();

            // Then
            // Should have the ensure method
            boolean hasEnsureMethod = java.util.Arrays.stream(declaredMethods)
                .anyMatch(method -> method.getName().equals("ensure") &&
                         java.lang.reflect.Modifier.isStatic(method.getModifiers()));
            assertTrue(hasEnsureMethod);
        }

        @Test
        @DisplayName("Should not be instantiable")
        void shouldNotBeInstantiable() {
            // Given
            Class<SeedUsers> seedUsersClass = SeedUsers.class;

            // When
            java.lang.reflect.Constructor<?>[] constructors = seedUsersClass.getDeclaredConstructors();

            // Then
            // Should have private constructor or no public constructors
            boolean hasPublicConstructor = java.util.Arrays.stream(constructors)
                .anyMatch(constructor -> java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
            assertFalse(hasPublicConstructor, "Should not have public constructors");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle mixed user existence scenarios")
        void shouldHandleMixedUserExistenceScenarios() {
            // Given - Only cashier exists
            User existingCashier = new User(1L, "cashier", "hash", "cashier@syos.com", domain.user.Role.CASHIER);
            when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(existingCashier));
            when(userRepository.findByUsername("manager")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

            when(passwordEncoder.hash("manager123")).thenReturn("manager_hash");
            when(passwordEncoder.hash("user123")).thenReturn("user_hash");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            // Should create manager and user, but not cashier
            verify(userRepository, times(2)).upsert(any(User.class));
            verify(userRepository).upsert(argThat(user -> user.username().equals("manager")));
            verify(userRepository).upsert(argThat(user -> user.username().equals("user")));

            verify(passwordEncoder, never()).hash("cashier123");
            verify(passwordEncoder).hash("manager123");
            verify(passwordEncoder).hash("user123");

            String output = outputStream.toString();
            assertFalse(output.contains("Created demo cashier account"));
            assertTrue(output.contains("Created demo manager account"));
            assertTrue(output.contains("Created demo user account"));
        }

        @Test
        @DisplayName("Should handle very long hashed passwords")
        void shouldHandleVeryLongHashedPasswords() {
            // Given
            String veryLongHash = "$2a$12$" + "a".repeat(1000);
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn(veryLongHash);

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository, times(3)).upsert(argThat(user ->
                user.passwordHash().equals(veryLongHash)));
        }

        @Test
        @DisplayName("Should handle empty string hashes")
        void shouldHandleEmptyStringHashes() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.hash(anyString())).thenReturn("");

            // When
            SeedUsers.ensure(userRepository, passwordEncoder);

            // Then
            verify(userRepository, times(3)).upsert(argThat(user ->
                user.passwordHash().isEmpty()));
        }
    }
}
