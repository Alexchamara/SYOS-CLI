package application.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for generating unique order IDs
 */
public final class OrderIdService {
    private static final AtomicLong counter = new AtomicLong(1);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * Generate order ID in format: PREFIX-YYYYMMDD-HHMMSS-XXXXXX
     */
    public static String generateOrderId(String prefix) {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_FORMATTER);
        String timePart = now.format(TIME_FORMATTER);

        // Generate 6-digit sequential number
        long currentCount = counter.getAndUpdate(n -> n >= 999999 ? 1 : n + 1);
        String sequencePart = String.format("%06d", currentCount);

        return String.format("%s-%s-%s-%s", prefix, datePart, timePart, sequencePart);
    }

    /**
     * Reset counter for testing purposes
     */
    public static void resetCounter() {
        counter.set(1);
    }
}
