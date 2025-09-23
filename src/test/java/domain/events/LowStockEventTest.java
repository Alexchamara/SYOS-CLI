package domain.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.events.LowStockEvent;
import domain.shared.Code;

@DisplayName("LowStockEvent Domain Event Tests")
class LowStockEventTest {

    @Test
    @DisplayName("Should create LowStockEvent with valid parameters")
    void shouldCreateLowStockEventWithValidParameters() {
        // Given
        Code productCode = new Code("PROD001");
        int remaining = 5;

        // When
        LowStockEvent event = new LowStockEvent(productCode, remaining);

        // Then
        assertEquals(productCode, event.productCode());
        assertEquals(remaining, event.remaining());
        assertTrue(event instanceof domain.events.DomainEvent);
    }

    @Test
    @DisplayName("Should handle different product codes")
    void shouldHandleDifferentProductCodes() {
        // Given
        Code code1 = new Code("PROD001");
        Code code2 = new Code("PROD002");

        // When
        LowStockEvent event1 = new LowStockEvent(code1, 10);
        LowStockEvent event2 = new LowStockEvent(code2, 5);

        // Then
        assertEquals(code1, event1.productCode());
        assertEquals(code2, event2.productCode());
        assertNotEquals(event1.productCode(), event2.productCode());
    }

    @Test
    @DisplayName("Should handle different remaining quantities")
    void shouldHandleDifferentRemainingQuantities() {
        // Given
        Code productCode = new Code("PROD001");

        // When
        LowStockEvent event1 = new LowStockEvent(productCode, 0);
        LowStockEvent event2 = new LowStockEvent(productCode, 15);

        // Then
        assertEquals(0, event1.remaining());
        assertEquals(15, event2.remaining());
    }

    @Test
    @DisplayName("Should handle zero remaining stock")
    void shouldHandleZeroRemainingStock() {
        // Given
        Code productCode = new Code("PROD001");

        // When
        LowStockEvent event = new LowStockEvent(productCode, 0);

        // Then
        assertEquals(0, event.remaining());
        assertEquals(productCode, event.productCode());
    }

    @Test
    @DisplayName("Should be equal when productCode and remaining are same")
    void shouldBeEqualWhenProductCodeAndRemainingAreSame() {
        // Given
        Code productCode = new Code("PROD001");
        int remaining = 5;

        // When
        LowStockEvent event1 = new LowStockEvent(productCode, remaining);
        LowStockEvent event2 = new LowStockEvent(productCode, remaining);

        // Then
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when parameters differ")
    void shouldNotBeEqualWhenParametersDiffer() {
        // Given
        Code productCode1 = new Code("PROD001");
        Code productCode2 = new Code("PROD002");

        // When
        LowStockEvent event1 = new LowStockEvent(productCode1, 5);
        LowStockEvent event2 = new LowStockEvent(productCode2, 5);
        LowStockEvent event3 = new LowStockEvent(productCode1, 10);

        // Then
        assertNotEquals(event1, event2); // Different product codes
        assertNotEquals(event1, event3); // Different remaining quantities
    }
}
