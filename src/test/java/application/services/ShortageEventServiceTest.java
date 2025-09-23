package application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.ShortageEventService;
import domain.repository.ShortageEventRepository;
import infrastructure.concurrency.Tx;

import java.sql.Connection;
import java.util.List;
import java.util.function.Function;

@DisplayName("ShortageEventService Tests")
class ShortageEventServiceTest {

    @Mock
    private Tx tx;

    @Mock
    private ShortageEventRepository shortageEventRepository;

    @Mock
    private Connection connection;

    private ShortageEventService shortageEventService;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        shortageEventService = new ShortageEventService(tx, shortageEventRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Record Shortage Event Tests")
    class RecordShortageEventTests {

        @Test
        @DisplayName("Should record shortage event message successfully")
        void shouldRecordShortageEventMessageSuccessfully() {
            // Given
            String message = "Low stock alert: Product PROD001 has only 5 units remaining";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.record(message);

            // Then
            verify(tx).inTx(any());
            verify(shortageEventRepository).save(connection, message);
        }

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() {
            // Given
            String emptyMessage = "";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.record(emptyMessage);

            // Then
            verify(shortageEventRepository).save(connection, emptyMessage);
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() {
            // Given
            String nullMessage = null;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.record(nullMessage);

            // Then
            verify(shortageEventRepository).save(connection, nullMessage);
        }

        @Test
        @DisplayName("Should handle very long messages")
        void shouldHandleVeryLongMessages() {
            // Given
            String longMessage = "Critical shortage alert: ".repeat(100) + "Product out of stock";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.record(longMessage);

            // Then
            verify(shortageEventRepository).save(connection, longMessage);
        }

        @Test
        @DisplayName("Should handle transaction exceptions during record")
        void shouldHandleTransactionExceptionsDuringRecord() {
            // Given
            String message = "Test shortage message";

            when(tx.inTx(any())).thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventService.record(message));
            assertEquals("Database connection failed", exception.getMessage());

            verify(tx).inTx(any());
        }

        @Test
        @DisplayName("Should handle repository exceptions during save")
        void shouldHandleRepositoryExceptionsDuringSave() {
            // Given
            String message = "Test shortage message";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });
            doThrow(new RuntimeException("Save operation failed")).when(shortageEventRepository)
                .save(connection, message);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventService.record(message));
            assertEquals("Save operation failed", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("List Shortage Events Tests")
    class ListShortageEventsTests {

        @Test
        @DisplayName("Should return list of shortage events")
        void shouldReturnListOfShortageEvents() {
            // Given
            List<String> expectedEvents = List.of(
                "Low stock: PROD001 - 5 units remaining",
                "Critical shortage: PROD002 - 2 units remaining",
                "Out of stock: PROD003 - 0 units remaining"
            );

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, List<String>> function = (Function<Connection, List<String>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(shortageEventRepository.list(connection)).thenReturn(expectedEvents);

            // When
            List<String> result = shortageEventService.list();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(expectedEvents, result);
            verify(tx).inTx(any());
            verify(shortageEventRepository).list(connection);
        }

        @Test
        @DisplayName("Should return empty list when no events exist")
        void shouldReturnEmptyListWhenNoEventsExist() {
            // Given
            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, List<String>> function = (Function<Connection, List<String>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(shortageEventRepository.list(connection)).thenReturn(List.of());

            // When
            List<String> result = shortageEventService.list();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(shortageEventRepository).list(connection);
        }

        @Test
        @DisplayName("Should handle single event in list")
        void shouldHandleSingleEventInList() {
            // Given
            List<String> singleEvent = List.of("Critical: PROD001 shortage detected");

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, List<String>> function = (Function<Connection, List<String>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(shortageEventRepository.list(connection)).thenReturn(singleEvent);

            // When
            List<String> result = shortageEventService.list();

            // Then
            assertEquals(1, result.size());
            assertEquals("Critical: PROD001 shortage detected", result.get(0));
        }

        @Test
        @DisplayName("Should handle transaction exceptions during list")
        void shouldHandleTransactionExceptionsDuringList() {
            // Given
            when(tx.inTx(any())).thenThrow(new RuntimeException("Database error during list"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventService.list());
            assertEquals("Database error during list", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions during list")
        void shouldHandleRepositoryExceptionsDuringList() {
            // Given
            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, List<String>> function = (Function<Connection, List<String>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(shortageEventRepository.list(connection)).thenThrow(new RuntimeException("List operation failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventService.list());
            assertEquals("List operation failed", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Clear Shortage Events Tests")
    class ClearShortageEventsTests {

        @Test
        @DisplayName("Should clear all shortage events successfully")
        void shouldClearAllShortageEventsSuccessfully() {
            // Given
            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.clear();

            // Then
            verify(tx).inTx(any());
            verify(shortageEventRepository).clear(connection);
        }

        @Test
        @DisplayName("Should handle transaction exceptions during clear")
        void shouldHandleTransactionExceptionsDuringClear() {
            // Given
            when(tx.inTx(any())).thenThrow(new RuntimeException("Transaction failed during clear"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventService.clear());
            assertEquals("Transaction failed during clear", exception.getMessage());

            verify(tx).inTx(any());
        }

        @Test
        @DisplayName("Should handle repository exceptions during clear")
        void shouldHandleRepositoryExceptionsDuringClear() {
            // Given
            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });
            doThrow(new RuntimeException("Clear operation failed")).when(shortageEventRepository)
                .clear(connection);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shortageEventService.clear());
            assertEquals("Clear operation failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should successfully clear when repository is already empty")
        void shouldSuccessfullyClearWhenRepositoryIsAlreadyEmpty() {
            // Given
            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });
            // Repository clear operation completes normally even if no events exist

            // When
            shortageEventService.clear();

            // Then
            verify(shortageEventRepository).clear(connection);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ShortageEventService with required dependencies")
        void shouldCreateShortageEventServiceWithRequiredDependencies() {
            // When
            ShortageEventService service = new ShortageEventService(tx, shortageEventRepository);

            // Then
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should handle record and list operations in sequence")
        void shouldHandleRecordAndListOperationsInSequence() {
            // Given
            String message1 = "First shortage event";
            String message2 = "Second shortage event";
            List<String> expectedEvents = List.of(message1, message2);

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(shortageEventRepository.list(connection)).thenReturn(expectedEvents);

            // When
            shortageEventService.record(message1);
            shortageEventService.record(message2);
            List<String> result = shortageEventService.list();

            // Then
            assertEquals(expectedEvents, result);
            verify(shortageEventRepository, times(2)).save(eq(connection), anyString());
            verify(shortageEventRepository).list(connection);
        }

        @Test
        @DisplayName("Should handle record, list, and clear operations sequence")
        void shouldHandleRecordListAndClearOperationsSequence() {
            // Given
            String message = "Test shortage event";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(shortageEventRepository.list(connection))
                .thenReturn(List.of(message))  // Before clear
                .thenReturn(List.of());        // After clear

            // When
            shortageEventService.record(message);
            List<String> beforeClear = shortageEventService.list();
            shortageEventService.clear();
            List<String> afterClear = shortageEventService.list();

            // Then
            assertEquals(1, beforeClear.size());
            assertEquals(0, afterClear.size());
            verify(shortageEventRepository).save(connection, message);
            verify(shortageEventRepository).clear(connection);
            verify(shortageEventRepository, times(2)).list(connection);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle special characters in messages")
        void shouldHandleSpecialCharactersInMessages() {
            // Given
            String specialMessage = "Alert: Product «PROD-001» has été épuisé! 🚨 Quantity: 0";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.record(specialMessage);

            // Then
            verify(shortageEventRepository).save(connection, specialMessage);
        }

        @Test
        @DisplayName("Should handle messages with line breaks")
        void shouldHandleMessagesWithLineBreaks() {
            // Given
            String multilineMessage = "Critical shortage detected:\nProduct: PROD001\nQuantity: 0\nAction required!";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            shortageEventService.record(multilineMessage);

            // Then
            verify(shortageEventRepository).save(connection, multilineMessage);
        }
    }
}
