package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcUserRepository;
import domain.user.User;
import domain.user.Role;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@DisplayName("JdbcUserRepository Tests")
class JdbcUserRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcUserRepository userRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        userRepository = new JdbcUserRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Find By Username Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should find user by username successfully")
        void shouldFindUserByUsernameSuccessfully() throws SQLException {
            // Given
            String username = "cashier1";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong("id")).thenReturn(1L);
            when(resultSet.getString("username")).thenReturn(username);
            when(resultSet.getString("password_hash")).thenReturn("hashedPassword");
            when(resultSet.getString("email")).thenReturn("cashier1@store.com");
            when(resultSet.getString("full_name")).thenReturn("John Cashier");
            when(resultSet.getString("role")).thenReturn("CASHIER");

            // When
            Optional<User> result = userRepository.findByUsername(username);

            // Then
            assertTrue(result.isPresent());
            User user = result.get();
            assertEquals(1L, user.id());
            assertEquals(username, user.username());
            assertEquals("hashedPassword", user.passwordHash());
            assertEquals("cashier1@store.com", user.email());
            assertEquals("John Cashier", user.fullName());
            assertEquals(Role.CASHIER, user.role());

            verify(preparedStatement).setString(1, username);
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() throws SQLException {
            // Given
            String username = "nonexistent";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            Optional<User> result = userRepository.findByUsername(username);

            // Then
            assertFalse(result.isPresent());
            verify(preparedStatement).setString(1, username);
        }

        @Test
        @DisplayName("Should handle different user roles")
        void shouldHandleDifferentUserRoles() throws SQLException {
            // Given
            String managerUsername = "manager1";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong("id")).thenReturn(2L);
            when(resultSet.getString("username")).thenReturn(managerUsername);
            when(resultSet.getString("password_hash")).thenReturn("managerHash");
            when(resultSet.getString("email")).thenReturn("manager1@store.com");
            when(resultSet.getString("full_name")).thenReturn("Jane Manager");
            when(resultSet.getString("role")).thenReturn("MANAGER");

            // When
            Optional<User> result = userRepository.findByUsername(managerUsername);

            // Then
            assertTrue(result.isPresent());
            assertEquals(Role.MANAGER, result.get().role());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find by username")
        void shouldHandleSQLExceptionsDuringFindByUsername() throws SQLException {
            // Given
            String username = "testuser";
            SQLException sqlException = new SQLException("Database error");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userRepository.findByUsername(username));
            assertTrue(exception.getMessage().contains("Failed to find user by username"));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle null full name")
        void shouldHandleNullFullName() throws SQLException {
            // Given
            String username = "user_no_name";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong("id")).thenReturn(3L);
            when(resultSet.getString("username")).thenReturn(username);
            when(resultSet.getString("password_hash")).thenReturn("hash");
            when(resultSet.getString("email")).thenReturn("user@store.com");
            when(resultSet.getString("full_name")).thenReturn(null);
            when(resultSet.getString("role")).thenReturn("USER");

            // When
            Optional<User> result = userRepository.findByUsername(username);

            // Then
            assertTrue(result.isPresent());
            assertNull(result.get().fullName());
        }
    }

    @Nested
    @DisplayName("Upsert User Tests")
    class UpsertUserTests {

        @Test
        @DisplayName("Should upsert user successfully")
        void shouldUpsertUserSuccessfully() throws SQLException {
            // Given
            User user = new User(1L, "cashier2", "newHash", "cashier2@store.com", "New Cashier", Role.CASHIER);

            // When
            userRepository.upsert(user);

            // Then
            verify(connection).prepareStatement(contains("INSERT INTO users"));
            verify(preparedStatement).setString(1, "cashier2");
            verify(preparedStatement).setString(2, "newHash");
            verify(preparedStatement).setString(3, "cashier2@store.com");
            verify(preparedStatement).setString(4, "New Cashier");
            verify(preparedStatement).setString(5, "CASHIER");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle user with null full name in upsert")
        void shouldHandleUserWithNullFullNameInUpsert() throws SQLException {
            // Given
            User user = new User(1L, "user1", "hash", "user1@store.com", null, Role.USER);

            // When
            userRepository.upsert(user);

            // Then
            verify(preparedStatement).setString(4, null);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle SQL exceptions during upsert")
        void shouldHandleSQLExceptionsDuringUpsert() throws SQLException {
            // Given
            User user = new User(1L, "testuser", "hash", "test@store.com", "Test User", Role.USER);
            SQLException sqlException = new SQLException("Constraint violation");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userRepository.upsert(user));
            assertTrue(exception.getMessage().contains("Failed to upsert user"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Create Web Customer Tests")
    class CreateWebCustomerTests {

        @Test
        @DisplayName("Should create web customer successfully")
        void shouldCreateWebCustomerSuccessfully() throws SQLException {
            // Given
            String email = "customer@example.com";
            String passwordHash = "hashedPassword";
            String fullName = "John Customer";
            long expectedUserId = 100L;

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(expectedUserId);

            // When
            long result = userRepository.createWebCustomer(email, passwordHash, fullName);

            // Then
            assertEquals(expectedUserId, result);
            verify(connection).prepareStatement(contains("INSERT INTO users"), eq(Statement.RETURN_GENERATED_KEYS));
            verify(preparedStatement).setString(1, email);
            verify(preparedStatement).setString(2, passwordHash);
            verify(preparedStatement).setString(3, fullName);
            verify(preparedStatement).setString(4, email); // username = email for web customers
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should throw exception when no generated keys returned")
        void shouldThrowExceptionWhenNoGeneratedKeysReturned() throws SQLException {
            // Given
            String email = "customer@example.com";
            String passwordHash = "hashedPassword";
            String fullName = "John Customer";

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No generated keys

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userRepository.createWebCustomer(email, passwordHash, fullName));
            assertEquals("Failed to get generated user ID", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during web customer creation")
        void shouldHandleSQLExceptionsDuringWebCustomerCreation() throws SQLException {
            // Given
            String email = "customer@example.com";
            String passwordHash = "hashedPassword";
            String fullName = "John Customer";
            SQLException sqlException = new SQLException("Duplicate email");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userRepository.createWebCustomer(email, passwordHash, fullName));
            assertTrue(exception.getMessage().contains("Failed to create web customer"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Find Web Customer Tests")
    class FindWebCustomerTests {

        @Test
        @DisplayName("Should find web customer by email successfully")
        void shouldFindWebCustomerByEmailSuccessfully() throws SQLException {
            // Given
            String email = "customer@example.com";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong("id")).thenReturn(100L);
            when(resultSet.getString("username")).thenReturn(email);
            when(resultSet.getString("password_hash")).thenReturn("hashedPassword");
            when(resultSet.getString("email")).thenReturn(email);
            when(resultSet.getString("full_name")).thenReturn("John Customer");
            when(resultSet.getString("role")).thenReturn("USER");

            // When
            User result = userRepository.findWebCustomerByEmail(email);

            // Then
            assertNotNull(result);
            assertEquals(100L, result.id());
            assertEquals(email, result.email());
            assertEquals("John Customer", result.fullName());
            assertEquals(Role.USER, result.role());

            verify(preparedStatement).setString(1, email);
            verify(connection).prepareStatement(contains("role = 'USER'"));
        }

        @Test
        @DisplayName("Should return null when web customer not found")
        void shouldReturnNullWhenWebCustomerNotFound() throws SQLException {
            // Given
            String email = "nonexistent@example.com";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            User result = userRepository.findWebCustomerByEmail(email);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find web customer")
        void shouldHandleSQLExceptionsDuringFindWebCustomer() throws SQLException {
            // Given
            String email = "customer@example.com";
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userRepository.findWebCustomerByEmail(email));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Get Password Hash Tests")
    class GetPasswordHashTests {

        @Test
        @DisplayName("Should get password hash by email successfully")
        void shouldGetPasswordHashByEmailSuccessfully() throws SQLException {
            // Given
            String email = "user@example.com";
            String expectedHash = "expectedHashValue";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("password_hash")).thenReturn(expectedHash);

            // When
            String result = userRepository.getPasswordHashByEmail(email);

            // Then
            assertEquals(expectedHash, result);
            verify(preparedStatement).setString(1, email);
        }

        @Test
        @DisplayName("Should return null when user not found by email")
        void shouldReturnNullWhenUserNotFoundByEmail() throws SQLException {
            // Given
            String email = "nonexistent@example.com";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            String result = userRepository.getPasswordHashByEmail(email);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during password hash retrieval")
        void shouldHandleSQLExceptionsDuringPasswordHashRetrieval() throws SQLException {
            // Given
            String email = "user@example.com";
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userRepository.getPasswordHashByEmail(email));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcUserRepository with DataSource")
        void shouldCreateJdbcUserRepositoryWithDataSource() {
            // When
            JdbcUserRepository repository = new JdbcUserRepository(dataSource);

            // Then
            assertNotNull(repository);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly in findByUsername")
        void shouldCloseResourcesProperlyInFindByUsername() throws SQLException {
            // Given
            String username = "testuser";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            userRepository.findByUsername(username);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources properly in upsert")
        void shouldCloseResourcesProperlyInUpsert() throws SQLException {
            // Given
            User user = new User(1L, "user", "hash", "user@store.com", "User", Role.USER);

            // When
            userRepository.upsert(user);

            // Then
            verify(preparedStatement).close();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close resources even when exceptions occur")
        void shouldCloseResourcesEvenWhenExceptionsOccur() throws SQLException {
            // Given
            String username = "testuser";
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> userRepository.findByUsername(username));
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty username search")
        void shouldHandleEmptyUsernameSearch() throws SQLException {
            // Given
            String emptyUsername = "";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            Optional<User> result = userRepository.findByUsername(emptyUsername);

            // Then
            assertFalse(result.isPresent());
            verify(preparedStatement).setString(1, emptyUsername);
        }

        @Test
        @DisplayName("Should handle very long email addresses")
        void shouldHandleVeryLongEmailAddresses() throws SQLException {
            // Given
            String longEmail = "very.long.email.address.with.many.characters@" + "domain".repeat(20) + ".com";
            String passwordHash = "hash";
            String fullName = "Customer";

            when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(200L);

            // When
            long result = userRepository.createWebCustomer(longEmail, passwordHash, fullName);

            // Then
            assertEquals(200L, result);
            verify(preparedStatement).setString(1, longEmail);
            verify(preparedStatement).setString(4, longEmail); // username = email
        }

        @Test
        @DisplayName("Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() throws SQLException {
            // Given
            User user = new User(1L, "user1", "hash", "user@store.com", "José María García-López", Role.USER);

            // When
            userRepository.upsert(user);

            // Then
            verify(preparedStatement).setString(4, "José María García-López");
        }
    }
}
