package domain.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.events.DomainEvent;

@DisplayName("DomainEvent Interface Tests")
class DomainEventTest {

    @Test
    @DisplayName("Should implement DomainEvent interface")
    void shouldImplementDomainEventInterface() {
        // When
        TestDomainEvent event = new TestDomainEvent();

        // Then
        assertNotNull(event);
        assertTrue(event instanceof DomainEvent);
    }

    @Test
    @DisplayName("Should create multiple event implementations")
    void shouldCreateMultipleEventImplementations() {
        // When
        TestDomainEvent event1 = new TestDomainEvent();
        AnotherTestEvent event2 = new AnotherTestEvent();

        // Then
        assertTrue(event1 instanceof DomainEvent);
        assertTrue(event2 instanceof DomainEvent);
        assertNotSame(event1, event2);
    }

    @Test
    @DisplayName("Should support different event types")
    void shouldSupportDifferentEventTypes() {
        // When
        TestDomainEvent testEvent = new TestDomainEvent();
        AnotherTestEvent anotherEvent = new AnotherTestEvent();

        // Then
        assertNotEquals(testEvent.getClass(), anotherEvent.getClass());
        assertTrue(testEvent instanceof DomainEvent);
        assertTrue(anotherEvent instanceof DomainEvent);
    }

    // Test implementations of DomainEvent
    private static class TestDomainEvent implements DomainEvent {
        // Empty implementation for testing
    }

    private static class AnotherTestEvent implements DomainEvent {
        // Another empty implementation for testing
    }
}
