package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.AuthenticationUseCase;
import application.usecase.AuthenticationUseCase.Session;
import domain.repository.UserRepository;
import domain.user.User;
import domain.user.Role;
import infrastructure.security.PasswordEncoder;
import java.util.Optional;

@DisplayName("AuthenticationUseCase Tests")
class AuthenticationUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationUseCase authenticationUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationUseCase = new AuthenticationUseCase(userRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("Staff Login Tests")
    class StaffLoginTests {

        @Test
        @DisplayName("Should login staff user successfully with valid credentials")
        void shouldLoginStaffUserSuccessfully() {
            // Given
            String username = "cashier1";
            String password = "password123";
            String hashedPassword = "hashedPassword123";
            User staffUser = new User(1L, username, hashedPassword, "cashier1@store.com", Role.CASHIER);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(staffUser));
            when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);

            // When
            Session session = authenticationUseCase.loginStaff(username, password);

            // Then
            assertNotNull(session);
            assertEquals(username, session.identifier());
            assertEquals(Role.CASHIER, session.role());
            assertNull(session.userId());

            verify(userRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, hashedPassword);
        }

        @Test
        @DisplayName("Should throw exception when staff user not found")
        void shouldThrowExceptionWhenStaffUserNotFound() {
            // Given
            String username = "nonexistent";
            String password = "password123";

            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginStaff(username, password));
            assertEquals("User not found", exception.getMessage());

            verify(userRepository).findByUsername(username);
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when staff password is invalid")
        void shouldThrowExceptionWhenStaffPasswordIsInvalid() {
            // Given
            String username = "cashier1";
            String password = "wrongPassword";
            String hashedPassword = "hashedPassword123";
            User staffUser = new User(1L, username, hashedPassword, "cashier1@store.com", Role.CASHIER);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(staffUser));
            when(passwordEncoder.matches(password, hashedPassword)).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginStaff(username, password));
            assertEquals("Invalid credentials", exception.getMessage());

            verify(userRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, hashedPassword);
        }

        @Test
        @DisplayName("Should login manager successfully")
        void shouldLoginManagerSuccessfully() {
            // Given
            String username = "manager1";
            String password = "managerPass";
            String hashedPassword = "hashedManagerPass";
            User managerUser = new User(2L, username, hashedPassword, "manager1@store.com", Role.MANAGER);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(managerUser));
            when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);

            // When
            Session session = authenticationUseCase.loginStaff(username, password);

            // Then
            assertEquals(username, session.identifier());
            assertEquals(Role.MANAGER, session.role());
            assertNull(session.userId());
        }
    }

    @Nested
    @DisplayName("Web User Login Tests")
    class WebUserLoginTests {

        @Test
        @DisplayName("Should login web user successfully with valid credentials")
        void shouldLoginWebUserSuccessfully() {
            // Given
            String email = "customer@example.com";
            String password = "password123";
            String hashedPassword = "hashedPassword123";
            User webUser = new User(100L, "customer", hashedPassword, email, "John Doe", Role.USER);

            when(userRepository.getPasswordHashByEmail(email)).thenReturn(hashedPassword);
            when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
            when(userRepository.findWebCustomerByEmail(email)).thenReturn(webUser);

            // When
            Session session = authenticationUseCase.loginWebUser(email, password);

            // Then
            assertNotNull(session);
            assertEquals(email, session.identifier());
            assertEquals(Role.USER, session.role());
            assertEquals(100L, session.userId());

            verify(userRepository).getPasswordHashByEmail(email);
            verify(passwordEncoder).matches(password, hashedPassword);
            verify(userRepository).findWebCustomerByEmail(email);
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginWebUser(null, "password"));
            assertEquals("Email and password are required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when email is empty")
        void shouldThrowExceptionWhenEmailIsEmpty() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginWebUser("  ", "password"));
            assertEquals("Email and password are required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when password is null")
        void shouldThrowExceptionWhenPasswordIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginWebUser("test@example.com", null));
            assertEquals("Email and password are required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when web user not found")
        void shouldThrowExceptionWhenWebUserNotFound() {
            // Given
            String email = "nonexistent@example.com";
            String password = "password123";

            when(userRepository.getPasswordHashByEmail(email)).thenReturn(null);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginWebUser(email, password));
            assertEquals("User not found", exception.getMessage());

            verify(userRepository).getPasswordHashByEmail(email);
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when web user password is invalid")
        void shouldThrowExceptionWhenWebUserPasswordIsInvalid() {
            // Given
            String email = "customer@example.com";
            String password = "wrongPassword";
            String hashedPassword = "hashedPassword123";

            when(userRepository.getPasswordHashByEmail(email)).thenReturn(hashedPassword);
            when(passwordEncoder.matches(password, hashedPassword)).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginWebUser(email, password));
            assertEquals("Invalid credentials", exception.getMessage());

            verify(userRepository).getPasswordHashByEmail(email);
            verify(passwordEncoder).matches(password, hashedPassword);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should throw exception when web customer not found after password verification")
        void shouldThrowExceptionWhenWebCustomerNotFoundAfterPasswordVerification() {
            // Given
            String email = "customer@example.com";
            String password = "password123";
            String hashedPassword = "hashedPassword123";

            when(userRepository.getPasswordHashByEmail(email)).thenReturn(hashedPassword);
            when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
            when(userRepository.findWebCustomerByEmail(email)).thenReturn(null);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.loginWebUser(email, password));
            assertEquals("User not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Web User Registration Tests")
    class WebUserRegistrationTests {

        @Test
        @DisplayName("Should register new web user successfully")
        void shouldRegisterNewWebUserSuccessfully() {
            // Given
            String email = "newuser@example.com";
            String password = "password123";
            String fullName = "New User";
            String hashedPassword = "hashedPassword123";
            long expectedUserId = 200L;

            when(userRepository.findWebCustomerByEmail(email)).thenReturn(null);
            when(passwordEncoder.hash(password)).thenReturn(hashedPassword);
            when(userRepository.createWebCustomer(email, hashedPassword, fullName)).thenReturn(expectedUserId);

            // When
            long userId = authenticationUseCase.registerWebUser(email, password, fullName);

            // Then
            assertEquals(expectedUserId, userId);

            verify(userRepository).findWebCustomerByEmail(email);
            verify(passwordEncoder).hash(password);
            verify(userRepository).createWebCustomer(email, hashedPassword, fullName);
        }

        @Test
        @DisplayName("Should throw exception when email is null for registration")
        void shouldThrowExceptionWhenEmailIsNullForRegistration() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser(null, "password", "Full Name"));
            assertEquals("Email is required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when email is empty for registration")
        void shouldThrowExceptionWhenEmailIsEmptyForRegistration() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser("  ", "password", "Full Name"));
            assertEquals("Email is required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when password is null for registration")
        void shouldThrowExceptionWhenPasswordIsNullForRegistration() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser("test@example.com", null, "Full Name"));
            assertEquals("Password is required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when password is empty for registration")
        void shouldThrowExceptionWhenPasswordIsEmptyForRegistration() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser("test@example.com", "  ", "Full Name"));
            assertEquals("Password is required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when full name is null for registration")
        void shouldThrowExceptionWhenFullNameIsNullForRegistration() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser("test@example.com", "password", null));
            assertEquals("Full name is required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when full name is empty for registration")
        void shouldThrowExceptionWhenFullNameIsEmptyForRegistration() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser("test@example.com", "password", "  "));
            assertEquals("Full name is required", exception.getMessage());

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when user already exists")
        void shouldThrowExceptionWhenUserAlreadyExists() {
            // Given
            String email = "existing@example.com";
            String password = "password123";
            String fullName = "Existing User";
            User existingUser = new User(150L, "existing", "hash", email, fullName, Role.USER);

            when(userRepository.findWebCustomerByEmail(email)).thenReturn(existingUser);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationUseCase.registerWebUser(email, password, fullName));
            assertEquals("User with this email already exists", exception.getMessage());

            verify(userRepository).findWebCustomerByEmail(email);
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Session Record Tests")
    class SessionRecordTests {

        @Test
        @DisplayName("Should create session for staff user")
        void shouldCreateSessionForStaffUser() {
            // When
            Session session = new Session("cashier1", Role.CASHIER, null);

            // Then
            assertEquals("cashier1", session.identifier());
            assertEquals(Role.CASHIER, session.role());
            assertNull(session.userId());
        }

        @Test
        @DisplayName("Should create session for web user")
        void shouldCreateSessionForWebUser() {
            // When
            Session session = new Session("user@example.com", Role.USER, 123L);

            // Then
            assertEquals("user@example.com", session.identifier());
            assertEquals(Role.USER, session.role());
            assertEquals(123L, session.userId());
        }

        @Test
        @DisplayName("Should support session equality")
        void shouldSupportSessionEquality() {
            // Given
            Session session1 = new Session("test", Role.USER, 1L);
            Session session2 = new Session("test", Role.USER, 1L);
            Session session3 = new Session("different", Role.USER, 1L);

            // Then
            assertEquals(session1, session2);
            assertNotEquals(session1, session3);
            assertEquals(session1.hashCode(), session2.hashCode());
        }
    }
}
