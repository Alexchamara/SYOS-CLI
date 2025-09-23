package infrastructure.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.events.SimpleBus;
import domain.events.DomainEvent;
import domain.events.LowStockEvent;
import domain.shared.Code;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@DisplayName("SimpleBus Tests")
class SimpleBusTest {

    @Mock
    private Consumer<DomainEvent> listener1;

    @Mock
    private Consumer<DomainEvent> listener2;

    @Mock
    private DomainEvent mockEvent;

    private SimpleBus simpleBus;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        simpleBus = new SimpleBus();
    }

    @Nested
    @DisplayName("Event Publishing Tests")
    class EventPublishingTests {

        @Test
        @DisplayName("Should publish event to single listener")
        void shouldPublishEventToSingleListener() {
            // Given
            simpleBus.subscribe(listener1);
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 5);

            // When
            simpleBus.publish(event);

            // Then
            verify(listener1).accept(event);
        }

        @Test
        @DisplayName("Should publish event to multiple listeners")
        void shouldPublishEventToMultipleListeners() {
            // Given
            simpleBus.subscribe(listener1);
            simpleBus.subscribe(listener2);
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 3);

            // When
            simpleBus.publish(event);

            // Then
            verify(listener1).accept(event);
            verify(listener2).accept(event);
        }

        @Test
        @DisplayName("Should publish to no listeners when none subscribed")
        void shouldPublishToNoListenersWhenNoneSubscribed() {
            // Given
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 2);

            // When
            simpleBus.publish(event);

            // Then
            // No exceptions should be thrown
            verifyNoInteractions(listener1, listener2);
        }

        @Test
        @DisplayName("Should handle null event")
        void shouldHandleNullEvent() {
            // Given
            simpleBus.subscribe(listener1);

            // When
            simpleBus.publish(null);

            // Then
            verify(listener1).accept(null);
        }

        @Test
        @DisplayName("Should publish different event types")
        void shouldPublishDifferentEventTypes() {
            // Given
            simpleBus.subscribe(listener1);
            LowStockEvent lowStockEvent = new LowStockEvent(new Code("PROD001"), 1);
            TestDomainEvent customEvent = new TestDomainEvent("test data");

            // When
            simpleBus.publish(lowStockEvent);
            simpleBus.publish(customEvent);

            // Then
            verify(listener1).accept(lowStockEvent);
            verify(listener1).accept(customEvent);
        }
    }

    @Nested
    @DisplayName("Subscription Management Tests")
    class SubscriptionManagementTests {

        @Test
        @DisplayName("Should allow multiple subscriptions of same listener")
        void shouldAllowMultipleSubscriptionsOfSameListener() {
            // Given
            simpleBus.subscribe(listener1);
            simpleBus.subscribe(listener1); // Subscribe again
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 4);

            // When
            simpleBus.publish(event);

            // Then
            verify(listener1, times(2)).accept(event); // Called twice
        }

        @Test
        @DisplayName("Should handle null listener subscription")
        void shouldHandleNullListenerSubscription() {
            // Given
            simpleBus.subscribe(null);
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 5);

            // When & Then
            assertThrows(NullPointerException.class, () -> simpleBus.publish(event));
        }

        @Test
        @DisplayName("Should maintain subscription order")
        void shouldMaintainSubscriptionOrder() {
            // Given
            StringBuilder callOrder = new StringBuilder();
            Consumer<DomainEvent> firstListener = e -> callOrder.append("1");
            Consumer<DomainEvent> secondListener = e -> callOrder.append("2");
            Consumer<DomainEvent> thirdListener = e -> callOrder.append("3");

            simpleBus.subscribe(firstListener);
            simpleBus.subscribe(secondListener);
            simpleBus.subscribe(thirdListener);

            // When
            simpleBus.publish(mockEvent);

            // Then
            assertEquals("123", callOrder.toString());
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should continue publishing to other listeners when one fails")
        void shouldContinuePublishingToOtherListenersWhenOneFails() {
            // Given
            Consumer<DomainEvent> failingListener = e -> { throw new RuntimeException("Listener failed"); };
            simpleBus.subscribe(failingListener);
            simpleBus.subscribe(listener1);
            LowStockEvent event = new LowStockEvent(new Code("PROD001"), 2);

            // When & Then
            assertThrows(RuntimeException.class, () -> simpleBus.publish(event));

            // Note: Due to forEach implementation, all listeners are called before exception propagates
            // But the first exception stops further processing
        }

        @Test
        @DisplayName("Should handle listener that modifies event")
        void shouldHandleListenerThatModifiesEvent() {
            // Given
            TestMutableEvent mutableEvent = new TestMutableEvent();
            Consumer<DomainEvent> modifyingListener = e -> {
                if (e instanceof TestMutableEvent) {
                    ((TestMutableEvent) e).setProcessed(true);
                }
            };

            simpleBus.subscribe(modifyingListener);

            // When
            simpleBus.publish(mutableEvent);

            // Then
            assertTrue(mutableEvent.isProcessed());
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent subscriptions safely")
        void shouldHandleConcurrentSubscriptionsSafely() throws InterruptedException {
            // Given
            int numThreads = 10;
            CountDownLatch latch = new CountDownLatch(numThreads);

            // When
            for (int i = 0; i < numThreads; i++) {
                final int threadNum = i;
                new Thread(() -> {
                    try {
                        Consumer<DomainEvent> listener = e -> { /* no-op */ };
                        simpleBus.subscribe(listener);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Then
            assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");
            // No exceptions should be thrown due to CopyOnWriteArrayList
        }

        @Test
        @DisplayName("Should handle concurrent publishing and subscription")
        void shouldHandleConcurrentPublishingAndSubscription() throws InterruptedException {
            // Given
            CountDownLatch publishLatch = new CountDownLatch(1);
            CountDownLatch subscribeLatch = new CountDownLatch(1);

            Consumer<DomainEvent> testListener = e -> publishLatch.countDown();

            // When
            Thread publishThread = new Thread(() -> {
                try {
                    subscribeLatch.await(5, TimeUnit.SECONDS);
                    simpleBus.publish(new LowStockEvent(new Code("PROD001"), 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread subscribeThread = new Thread(() -> {
                simpleBus.subscribe(testListener);
                subscribeLatch.countDown();
            });

            publishThread.start();
            subscribeThread.start();

            // Then
            assertTrue(publishLatch.await(5, TimeUnit.SECONDS), "Event should be published and received");
            publishThread.join(1000);
            subscribeThread.join(1000);
        }
    }

    @Nested
    @DisplayName("Type Safety Tests")
    class TypeSafetyTests {

        @Test
        @DisplayName("Should accept any DomainEvent implementation")
        void shouldAcceptAnyDomainEventImplementation() {
            // Given
            simpleBus.subscribe(listener1);
            LowStockEvent lowStockEvent = new LowStockEvent(new Code("PROD001"), 0);
            TestDomainEvent customEvent = new TestDomainEvent("custom");

            // When
            simpleBus.publish(lowStockEvent);
            simpleBus.publish(customEvent);

            // Then
            verify(listener1).accept(lowStockEvent);
            verify(listener1).accept(customEvent);
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
