package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcShortageEventRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("JdbcShortageEventRepository Tests")
class JdbcShortageEventRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcShortageEventRepository shortageEventRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        shortageEventRepository = new JdbcShortageEventRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Save Shortage Event Tests")
    class SaveShortageEventTests {

        @Test
        @DisplayName("Should save shortage event message successfully")
        void shouldSaveShortageEventMessageSuccessfully() throws SQLException {
            // Given
            String message = "Low stock alert: Product PROD001 has only 5 units remaining";

            // When
            shortageEventRepository.save(connection, message);

            // Then
            verify(connection).prepareStatement("INSERT INTO notify_shortage(message) VALUES (?)");
            verify(preparedStatement).setString(1, message);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() throws SQLException {
            // Given
            String emptyMessage = "";

            // When
            shortageEventRepository.save(connection, emptyMessage);

            // Then
            verify(preparedStatement).setString(1, emptyMessage);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() throws SQLException {
            // Given
            String nullMessage = null;

            // When
            shortageEventRepository.save(connection, nullMessage);

            // Then
            verify(preparedStatement).setString(1, null);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle very long messages")
        void shouldHandleVeryLongMessages() throws SQLException {
            // Given
            String longMessage = "Critical shortage alert: ".repeat(100) + "Immediate action required";

            // When
            shortageEventRepository.save(connection, longMessage);

            // Then
            verify(preparedStatement).setString(1, longMessage);
        }

        @Test
        @DisplayName("Should handle messages with special characters")
        void shouldHandleMessagesWithSpecialCharacters() throws SQLException {
            // Given
            String specialMessage = "Alert: Product «PROD-001» has été épuisé! 🚨 Quantity: 0";

            // When
            shortageEventRepository.save(connection, specialMessage);

            // Then
            verify(preparedStatement).setString(1, specialMessage);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during save")
        void shouldHandleSQLExceptionsDuringSave() throws SQLException {
            // Given
            String message = "Test shortage message";
            SQLException sqlException = new SQLException("Insert failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventRepository.save(connection, message));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("List Shortage Events Tests")
    class ListShortageEventsTests {

        @Test
        @DisplayName("Should list shortage events successfully")
        void shouldListShortageEventsSuccessfully() throws SQLException {
            // Given
            Timestamp timestamp1 = Timestamp.valueOf(LocalDateTime.now().minusHours(2));
            Timestamp timestamp2 = Timestamp.valueOf(LocalDateTime.now().minusHours(1));

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            when(resultSet.getTimestamp("created_at")).thenReturn(timestamp1, timestamp2);
            when(resultSet.getString("message")).thenReturn("First shortage event", "Second shortage event");

            // When
            List<String> result = shortageEventRepository.list(connection);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.get(0).contains("First shortage event"));
            assertTrue(result.get(1).contains("Second shortage event"));
            assertTrue(result.get(0).contains(timestamp1.toString()));
            assertTrue(result.get(1).contains(timestamp2.toString()));

            verify(connection).prepareStatement(contains("ORDER BY created_at DESC"));
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should return empty list when no events exist")
        void shouldReturnEmptyListWhenNoEventsExist() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<String> result = shortageEventRepository.list(connection);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle single event")
        void shouldHandleSingleEvent() throws SQLException {
            // Given
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getTimestamp("created_at")).thenReturn(timestamp);
            when(resultSet.getString("message")).thenReturn("Single event");

            // When
            List<String> result = shortageEventRepository.list(connection);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.get(0).contains("Single event"));
        }

        @Test
        @DisplayName("Should handle SQL exceptions during list")
        void shouldHandleSQLExceptionsDuringList() throws SQLException {
            // Given
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventRepository.list(connection));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should format timestamp and message correctly")
        void shouldFormatTimestampAndMessageCorrectly() throws SQLException {
            // Given
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(2025, 9, 22, 10, 30, 0));
            String message = "Critical shortage: PROD001";

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getTimestamp("created_at")).thenReturn(timestamp);
            when(resultSet.getString("message")).thenReturn(message);

            // When
            List<String> result = shortageEventRepository.list(connection);

            // Then
            assertEquals(1, result.size());
            String formattedEntry = result.get(0);
            assertTrue(formattedEntry.contains(timestamp.toString()));
            assertTrue(formattedEntry.contains(" | "));
            assertTrue(formattedEntry.contains(message));
        }
    }

    @Nested
    @DisplayName("Clear Shortage Events Tests")
    class ClearShortageEventsTests {

        @Test
        @DisplayName("Should clear all shortage events successfully")
        void shouldClearAllShortageEventsSuccessfully() throws SQLException {
            // When
            shortageEventRepository.clear(connection);

            // Then
            verify(connection).prepareStatement("DELETE FROM notify_shortage");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle SQL exceptions during clear")
        void shouldHandleSQLExceptionsDuringClear() throws SQLException {
            // Given
            SQLException sqlException = new SQLException("Delete failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventRepository.clear(connection));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should succeed when table is already empty")
        void shouldSucceedWhenTableIsAlreadyEmpty() throws SQLException {
            // Given
            when(preparedStatement.executeUpdate()).thenReturn(0); // No rows deleted

            // When
            shortageEventRepository.clear(connection);

            // Then
            verify(preparedStatement).executeUpdate();
            // Should not throw exception even if no rows were deleted
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JdbcShortageEventRepository with DataSource")
        void shouldCreateJdbcShortageEventRepositoryWithDataSource() {
            // When
            JdbcShortageEventRepository repository = new JdbcShortageEventRepository(dataSource);

            // Then
            assertNotNull(repository);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly in list operation")
        void shouldCloseResourcesProperlyInListOperation() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            shortageEventRepository.list(connection);

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> shortageEventRepository.list(connection));
            verify(preparedStatement).close();
        }
    }

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should handle save and list operations in sequence")
        void shouldHandleSaveAndListOperationsInSequence() throws SQLException {
            // Given
            String message1 = "First shortage event";
            String message2 = "Second shortage event";
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            // Mock save operations
            // Mock list operation
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            when(resultSet.getTimestamp("created_at")).thenReturn(now, now);
            when(resultSet.getString("message")).thenReturn(message1, message2);

            // When
            shortageEventRepository.save(connection, message1);
            shortageEventRepository.save(connection, message2);
            List<String> events = shortageEventRepository.list(connection);

            // Then
            assertEquals(2, events.size());
            verify(preparedStatement, times(2)).executeUpdate(); // Two saves
            verify(preparedStatement, times(1)).executeQuery();  // One list
        }

        @Test
        @DisplayName("Should handle save, list, and clear operations sequence")
        void shouldHandleSaveListAndClearOperationsSequence() throws SQLException {
            // Given
            String message = "Test shortage event";

            // Mock list before clear
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getString("message")).thenReturn(message);

            // When
            shortageEventRepository.save(connection, message);
            List<String> beforeClear = shortageEventRepository.list(connection);
            shortageEventRepository.clear(connection);

            // Then
            assertEquals(1, beforeClear.size());
            verify(preparedStatement, times(2)).executeUpdate(); // Save + Clear
            verify(preparedStatement, times(1)).executeQuery();  // List
        }
    }
}
