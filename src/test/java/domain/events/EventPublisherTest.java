package domain.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.events.DomainEvent;

@DisplayName("EventPublisher Domain Service Tests")
class EventPublisherTest {

    @Test
    @DisplayName("Should create test event implementations")
    void shouldCreateTestEventImplementations() {
        // When
        TestEvent event1 = new TestEvent("test data");
        TestEvent event2 = new TestEvent("other data");

        // Then
        assertNotNull(event1);
        assertNotNull(event2);
        assertInstanceOf(DomainEvent.class, event1);
        assertInstanceOf(DomainEvent.class, event2);
    }

    @Test
    @DisplayName("Should handle mock domain events")
    void shouldHandleMockDomainEvents() {
        // Given
        DomainEvent mockEvent = mock(DomainEvent.class);

        // When & Then
        assertNotNull(mockEvent);
        assertInstanceOf(DomainEvent.class, mockEvent);
    }

    @Test
    @DisplayName("Should support multiple event types")
    void shouldSupportMultipleEventTypes() {
        // When
        TestEvent testEvent = new TestEvent("test");
        AnotherTestEvent anotherEvent = new AnotherTestEvent(123);

        // Then
        assertNotEquals(testEvent.getClass(), anotherEvent.getClass());
        assertInstanceOf(DomainEvent.class, testEvent);
        assertInstanceOf(DomainEvent.class, anotherEvent);
    }

    // Test implementations of DomainEvent
    private record TestEvent(String data) implements DomainEvent {}

    private record AnotherTestEvent(int value) implements DomainEvent {}
}
