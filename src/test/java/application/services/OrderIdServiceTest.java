package application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import application.services.OrderIdService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@DisplayName("OrderIdService Tests")
class OrderIdServiceTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    @BeforeEach
    void setUp() {
        OrderIdService.resetCounter();
    }

    @AfterEach
    void tearDown() {
        OrderIdService.resetCounter();
    }

    @Test
    @DisplayName("Should generate order ID with correct format")
    void shouldGenerateOrderIdWithCorrectFormat() {
        // Given
        String prefix = "WEB";

        // When
        String orderId = OrderIdService.generateOrderId(prefix);

        // Then
        assertNotNull(orderId);
        assertTrue(orderId.matches("WEB-\\d{8}-\\d{6}-\\d{6}"));
    }

    @Test
    @DisplayName("Should generate order ID with different prefixes")
    void shouldGenerateOrderIdWithDifferentPrefixes() {
        // Given
        String[] prefixes = {"WEB", "MOBILE", "API", "ADMIN"};

        // When & Then
        for (String prefix : prefixes) {
            String orderId = OrderIdService.generateOrderId(prefix);
            assertTrue(orderId.startsWith(prefix + "-"));
        }
    }

    @Test
    @DisplayName("Should generate order ID with current date")
    void shouldGenerateOrderIdWithCurrentDate() {
        // Given
        String prefix = "ORDER";
        String expectedDate = LocalDateTime.now().format(DATE_FORMATTER);

        // When
        String orderId = OrderIdService.generateOrderId(prefix);

        // Then
        assertTrue(orderId.contains(expectedDate));
    }

    @Test
    @DisplayName("Should generate order ID with current time")
    void shouldGenerateOrderIdWithCurrentTime() {
        // Given
        String prefix = "ORDER";
        LocalDateTime before = LocalDateTime.now();

        // When
        String orderId = OrderIdService.generateOrderId(prefix);

        LocalDateTime after = LocalDateTime.now();

        // Then
        // Extract time part from order ID
        String[] parts = orderId.split("-");
        assertEquals(4, parts.length);
        String timePart = parts[2];

        // Time should be between before and after
        String beforeTime = before.format(TIME_FORMATTER);
        String afterTime = after.format(TIME_FORMATTER);

        assertTrue(timePart.compareTo(beforeTime) >= 0);
        assertTrue(timePart.compareTo(afterTime) <= 0);
    }

    @Test
    @DisplayName("Should generate sequential numbers")
    void shouldGenerateSequentialNumbers() {
        // Given
        String prefix = "SEQ";

        // When
        String orderId1 = OrderIdService.generateOrderId(prefix);
        String orderId2 = OrderIdService.generateOrderId(prefix);
        String orderId3 = OrderIdService.generateOrderId(prefix);

        // Then
        String[] parts1 = orderId1.split("-");
        String[] parts2 = orderId2.split("-");
        String[] parts3 = orderId3.split("-");

        assertEquals("000001", parts1[3]);
        assertEquals("000002", parts2[3]);
        assertEquals("000003", parts3[3]);
    }

    @Test
    @DisplayName("Should format sequence numbers with leading zeros")
    void shouldFormatSequenceNumbersWithLeadingZeros() {
        // Given
        String prefix = "FORMAT";

        // When
        String orderId = OrderIdService.generateOrderId(prefix);

        // Then
        String[] parts = orderId.split("-");
        String sequencePart = parts[3];
        assertEquals(6, sequencePart.length());
        assertTrue(sequencePart.matches("\\d{6}"));
        assertEquals("000001", sequencePart);
    }

    @Test
    @DisplayName("Should reset counter when reaching maximum")
    void shouldResetCounterWhenReachingMaximum() {
        // Given
        String prefix = "MAX";

        // Simulate counter at 999999
        for (int i = 1; i < 999999; i++) {
            OrderIdService.generateOrderId(prefix);
        }

        // When
        String orderIdAtMax = OrderIdService.generateOrderId(prefix);
        String orderIdAfterReset = OrderIdService.generateOrderId(prefix);

        // Then
        String[] partsAtMax = orderIdAtMax.split("-");
        String[] partsAfterReset = orderIdAfterReset.split("-");

        assertEquals("999999", partsAtMax[3]);
        assertEquals("000001", partsAfterReset[3]);
    }

    @Test
    @DisplayName("Should handle empty prefix")
    void shouldHandleEmptyPrefix() {
        // Given
        String prefix = "";

        // When
        String orderId = OrderIdService.generateOrderId(prefix);

        // Then
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("-"));
        assertTrue(orderId.matches("-\\d{8}-\\d{6}-\\d{6}"));
    }

    @Test
    @DisplayName("Should handle null prefix")
    void shouldHandleNullPrefix() {
        // Given
        String prefix = null;

        // When
        String orderId = OrderIdService.generateOrderId(prefix);

        // Then
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("null-"));
    }

    @Test
    @DisplayName("Should reset counter to 1")
    void shouldResetCounterToOne() {
        // Given
        String prefix = "RESET";
        OrderIdService.generateOrderId(prefix); // Generate first ID
        OrderIdService.generateOrderId(prefix); // Generate second ID

        // When
        OrderIdService.resetCounter();
        String orderIdAfterReset = OrderIdService.generateOrderId(prefix);

        // Then
        String[] parts = orderIdAfterReset.split("-");
        assertEquals("000001", parts[3]);
    }

    @Test
    @DisplayName("Should generate unique IDs in concurrent environment")
    void shouldGenerateUniqueIdsInConcurrentEnvironment() throws Exception {
        // Given
        String prefix = "CONCURRENT";
        int numberOfThreads = 10;
        int idsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        Set<String> generatedIds = new HashSet<>();

        try {
            // When
            CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < idsPerThread; j++) {
                        String orderId = OrderIdService.generateOrderId(prefix);
                        synchronized (generatedIds) {
                            generatedIds.add(orderId);
                        }
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).get();

            // Then
            assertEquals(numberOfThreads * idsPerThread, generatedIds.size());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should maintain format consistency across multiple generations")
    void shouldMaintainFormatConsistencyAcrossMultipleGenerations() {
        // Given
        String prefix = "CONSISTENT";
        Pattern expectedPattern = Pattern.compile("CONSISTENT-\\d{8}-\\d{6}-\\d{6}");

        // When & Then
        for (int i = 0; i < 50; i++) {
            String orderId = OrderIdService.generateOrderId(prefix);
            assertTrue(expectedPattern.matcher(orderId).matches(),
                "Order ID " + orderId + " doesn't match expected pattern");
        }
    }
}
