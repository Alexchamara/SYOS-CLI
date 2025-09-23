package infrastructure.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import infrastructure.events.LowStockPrinter;
import domain.events.DomainEvent;
import domain.events.LowStockEvent;
import domain.shared.Code;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@DisplayName("LowStockPrinter Tests")
class LowStockPrinterTest {

    private LowStockPrinter lowStockPrinter;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        lowStockPrinter = new LowStockPrinter();

        // Capture System.out for testing print statements
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }

    @Nested
    @DisplayName("LowStockEvent Handling Tests")
    class LowStockEventHandlingTests {

        @Test
        @DisplayName("Should print low stock message for LowStockEvent")
        void shouldPrintLowStockMessageForLowStockEvent() {
            // Given
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 5);

            // When
            lowStockPrinter.accept(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD001 = 5"));
        }

        @Test
        @DisplayName("Should handle zero remaining stock")
        void shouldHandleZeroRemainingStock() {
            // Given
            LowStockEvent event = new LowStockEvent(new Code("PROD002"), 0);

            // When
            lowStockPrinter.accept(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD002 = 0"));
        }

        @Test
        @DisplayName("Should handle different product codes")
        void shouldHandleDifferentProductCodes() {
            // Given
            LowStockEvent event1 = new LowStockEvent(new Code("LAPTOP001"), 3);
            LowStockEvent event2 = new LowStockEvent(new Code("MOUSE-WIRELESS-001"), 1);

            // When
            lowStockPrinter.accept(event1);
            lowStockPrinter.accept(event2);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: LAPTOP001 = 3"));
            assertTrue(output.contains("LOW STOCK: MOUSE-WIRELESS-001 = 1"));
        }

        @Test
        @DisplayName("Should handle large remaining quantities")
        void shouldHandleLargeRemainingQuantities() {
            // Given
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), Integer.MAX_VALUE);

            // When
            lowStockPrinter.accept(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD001 = " + Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Should handle negative remaining quantities")
        void shouldHandleNegativeRemainingQuantities() {
            // Given
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), -10);

            // When
            lowStockPrinter.accept(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD001 = -10"));
        }
    }

    @Nested
    @DisplayName("Non-LowStockEvent Handling Tests")
    class NonLowStockEventHandlingTests {

        @Test
        @DisplayName("Should ignore non-LowStockEvent events")
        void shouldIgnoreNonLowStockEventEvents() {
            // Given
            TestDomainEvent nonLowStockEvent = new TestDomainEvent("test data");

            // When
            lowStockPrinter.accept(nonLowStockEvent);

            // Then
            String output = outputStream.toString();
            assertTrue(output.isEmpty()); // No output for non-LowStockEvent
        }

        @Test
        @DisplayName("Should handle null events gracefully")
        void shouldHandleNullEventsGracefully() {
            // When
            lowStockPrinter.accept(null);

            // Then
            String output = outputStream.toString();
            assertTrue(output.isEmpty()); // No output for null events
        }

        @Test
        @DisplayName("Should handle mixed event types")
        void shouldHandleMixedEventTypes() {
            // Given
            LowStockEvent lowStockEvent = new LowStockEvent(new Code("PROD001"), 2);
            TestDomainEvent otherEvent = new TestDomainEvent("other");
            AnotherTestEvent anotherEvent = new AnotherTestEvent(123);

            // When
            lowStockPrinter.accept(lowStockEvent);
            lowStockPrinter.accept(otherEvent);
            lowStockPrinter.accept(anotherEvent);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD001 = 2"));
            assertFalse(output.contains("other"));
            assertFalse(output.contains("123"));
        }
    }

    @Nested
    @DisplayName("Consumer Interface Tests")
    class ConsumerInterfaceTests {

        @Test
        @DisplayName("Should implement Consumer<DomainEvent> interface")
        void shouldImplementConsumerDomainEventInterface() {
            // Then
            assertTrue(lowStockPrinter instanceof java.util.function.Consumer);
        }

        @Test
        @DisplayName("Should be usable as functional interface")
        void shouldBeUsableAsFunctionalInterface() {
            // Given
            java.util.function.Consumer<DomainEvent> consumer = lowStockPrinter;
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 7);

            // When
            consumer.accept(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD001 = 7"));
        }
    }

    @Nested
    @DisplayName("Output Formatting Tests")
    class OutputFormattingTests {

        @Test
        @DisplayName("Should format output consistently")
        void shouldFormatOutputConsistently() {
            // Given
            LowStockEvent event1 = new LowStockEvent(new Code("A"), 1);
            LowStockEvent event2 = new LowStockEvent(new Code("VERY-LONG-PRODUCT-CODE-123"), 999);

            // When
            lowStockPrinter.accept(event1);
            lowStockPrinter.accept(event2);

            // Then
            String output = outputStream.toString();
            String[] lines = output.trim().split("\n");

            assertTrue(lines[0].matches("LOW STOCK: A = 1"));
            assertTrue(lines[1].matches("LOW STOCK: VERY-LONG-PRODUCT-CODE-123 = 999"));
        }

        @Test
        @DisplayName("Should handle special characters in product codes")
        void shouldHandleSpecialCharactersInProductCodes() {
            // Given
            LowStockEvent event = new LowStockEvent(new Code("PROD-001_SPECIAL@CHAR"), 8);

            // When
            lowStockPrinter.accept(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD-001_SPECIAL@CHAR = 8"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work with SimpleBus integration")
        void shouldWorkWithSimpleBusIntegration() {
            // Given
            infrastructure.events.SimpleBus bus = new infrastructure.events.SimpleBus();
            bus.subscribe(lowStockPrinter);
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 4);

            // When
            bus.publish(event);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("LOW STOCK: PROD001 = 4"));
        }

        @Test
        @DisplayName("Should work with multiple events in sequence")
        void shouldWorkWithMultipleEventsInSequence() {
            // Given
            LowStockEvent event1 = new LowStockEvent(new Code("PROD001"), 5);
            LowStockEvent event2 = new LowStockEvent(new Code("PROD002"), 3);
            LowStockEvent event3 = new LowStockEvent(new Code("PROD003"), 1);

            // When
            lowStockPrinter.accept(event1);
            lowStockPrinter.accept(event2);
            lowStockPrinter.accept(event3);

            // Then
            String output = outputStream.toString();
            String[] lines = output.trim().split("\n");
            assertEquals(3, lines.length);
            assertTrue(lines[0].contains("PROD001 = 5"));
            assertTrue(lines[1].contains("PROD002 = 3"));
            assertTrue(lines[2].contains("PROD003 = 1"));
        }
    }

    // Test helper classes
    private static class TestDomainEvent implements DomainEvent {
        private final String data;

        public TestDomainEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    private static class AnotherTestEvent implements DomainEvent {
        private final int value;

        public AnotherTestEvent(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static class TestMutableEvent implements DomainEvent {
        private boolean processed = false;

        public boolean isProcessed() {
            return processed;
        }

        public void setProcessed(boolean processed) {
            this.processed = processed;
        }
    }
}
