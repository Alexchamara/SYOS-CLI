package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.ShortageEventRepository;
import java.sql.Connection;
import java.util.List;

@DisplayName("ShortageEventRepository Domain Interface Tests")
class ShortageEventRepositoryTest {

    private ShortageEventRepository shortageEventRepository;
    private Connection connection;

    @BeforeEach
    void setUp() {
        shortageEventRepository = mock(ShortageEventRepository.class);
        connection = mock(Connection.class);
    }

    @Test
    @DisplayName("Should save shortage message")
    void shouldSaveShortageMessage() {
        // Given
        String message = "LOW STOCK: PROD001 at SHELF";

        // When
        shortageEventRepository.save(connection, message);

        // Then
        verify(shortageEventRepository).save(connection, message);
    }

    @Test
    @DisplayName("Should list saved messages")
    void shouldListSavedMessages() {
        // Given
        List<String> expected = List.of("LOW STOCK: PROD001", "LOW STOCK: PROD002");
        when(shortageEventRepository.list(connection)).thenReturn(expected);

        // When
        List<String> messages = shortageEventRepository.list(connection);

        // Then
        assertEquals(expected, messages);
        verify(shortageEventRepository).list(connection);
    }

    @Test
    @DisplayName("Should clear saved messages")
    void shouldClearSavedMessages() {
        // When
        shortageEventRepository.clear(connection);

        // Then
        verify(shortageEventRepository).clear(connection);
    }
}
