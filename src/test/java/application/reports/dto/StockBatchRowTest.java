package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.StockBatchRow;
import application.reports.dto.StockBatchRow.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;

@DisplayName("StockBatchRow DTO Tests")
class StockBatchRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create StockBatchRow with all parameters")
        void shouldCreateStockBatchRowWithAllParameters() {
            // Given
            String code = "PROD001";
            String name = "Gaming Laptop";
            String batchId = "BATCH001";
            LocalDate expiry = LocalDate.now().plusDays(30);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(5);
            int qty = 100;
            String location = "MAIN_STORE";

            // When
            StockBatchRow row = new StockBatchRow(code, name, batchId, expiry, receivedAt, qty, location);

            // Then
            assertEquals(code, row.code());
            assertEquals(name, row.name());
            assertEquals(batchId, row.batchId());
            assertEquals(expiry, row.expiry());
            assertEquals(receivedAt, row.receivedAt());
            assertEquals(qty, row.qty());
            assertEquals(location, row.location());
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(45);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(3);

            // When
            StockBatchRow mainStoreRow = new StockBatchRow("PROD001", "Product", "BATCH001", expiry, receivedAt, 50, "MAIN_STORE");
            StockBatchRow shelfRow = new StockBatchRow("PROD002", "Product", "BATCH002", expiry, receivedAt, 30, "SHELF");
            StockBatchRow webRow = new StockBatchRow("PROD003", "Product", "BATCH003", expiry, receivedAt, 20, "WEB");

            // Then
            assertEquals("MAIN_STORE", mainStoreRow.location());
            assertEquals("SHELF", shelfRow.location());
            assertEquals("WEB", webRow.location());
        }

        @Test
        @DisplayName("Should handle different batch quantities")
        void shouldHandleDifferentBatchQuantities() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(60);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(2);

            // When
            StockBatchRow smallBatch = new StockBatchRow("PROD001", "Product", "BATCH001", expiry, receivedAt, 1, "SHELF");
            StockBatchRow mediumBatch = new StockBatchRow("PROD002", "Product", "BATCH002", expiry, receivedAt, 500, "MAIN_STORE");
            StockBatchRow largeBatch = new StockBatchRow("PROD003", "Product", "BATCH003", expiry, receivedAt, 10000, "WEB");

            // Then
            assertEquals(1, smallBatch.qty());
            assertEquals(500, mediumBatch.qty());
            assertEquals(10000, largeBatch.qty());
        }

        @Test
        @DisplayName("Should handle different expiry and received dates")
        void shouldHandleDifferentExpiryAndReceivedDates() {
            // Given
            LocalDate nearExpiry = LocalDate.now().plusDays(7);
            LocalDate farExpiry = LocalDate.now().plusDays(365);
            LocalDateTime recentlyReceived = LocalDateTime.now().minusHours(2);
            LocalDateTime oldReceived = LocalDateTime.now().minusDays(30);

            // When
            StockBatchRow urgentBatch = new StockBatchRow("PROD001", "Urgent", "BATCH001", nearExpiry, recentlyReceived, 25, "SHELF");
            StockBatchRow stableBatch = new StockBatchRow("PROD002", "Stable", "BATCH002", farExpiry, oldReceived, 200, "MAIN_STORE");

            // Then
            assertTrue(urgentBatch.expiry().isBefore(stableBatch.expiry()));
            assertTrue(urgentBatch.receivedAt().isAfter(stableBatch.receivedAt()));
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(30);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(5);
            StockBatchRow row1 = new StockBatchRow("PROD001", "Product", "BATCH001", expiry, receivedAt, 100, "MAIN_STORE");
            StockBatchRow row2 = new StockBatchRow("PROD001", "Product", "BATCH001", expiry, receivedAt, 100, "MAIN_STORE");

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(30);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(5);
            StockBatchRow row1 = new StockBatchRow("PROD001", "Product", "BATCH001", expiry, receivedAt, 100, "MAIN_STORE");
            StockBatchRow row2 = new StockBatchRow("PROD001", "Product", "BATCH002", expiry, receivedAt, 100, "MAIN_STORE");

            // Then
            assertNotEquals(row1, row2);
        }
    }

    @Nested
    @DisplayName("Sort Enum Tests")
    class SortEnumTests {

        @Test
        @DisplayName("Should have all expected sort options")
        void shouldHaveAllExpectedSortOptions() {
            // Then
            assertEquals(3, Sort.values().length);
            assertEquals(Sort.EXPIRY_ASC_THEN_RECEIVED_ASC, Sort.valueOf("EXPIRY_ASC_THEN_RECEIVED_ASC"));
            assertEquals(Sort.RECEIVED_ASC, Sort.valueOf("RECEIVED_ASC"));
            assertEquals(Sort.QTY_DESC, Sort.valueOf("QTY_DESC"));
        }

        @Test
        @DisplayName("Should convert sort enum to string correctly")
        void shouldConvertSortEnumToStringCorrectly() {
            // Then
            assertEquals("EXPIRY_ASC_THEN_RECEIVED_ASC", Sort.EXPIRY_ASC_THEN_RECEIVED_ASC.toString());
            assertEquals("RECEIVED_ASC", Sort.RECEIVED_ASC.toString());
            assertEquals("QTY_DESC", Sort.QTY_DESC.toString());
        }

        @Test
        @DisplayName("Should support ordinal access")
        void shouldSupportOrdinalAccess() {
            // Then
            assertEquals(0, Sort.EXPIRY_ASC_THEN_RECEIVED_ASC.ordinal());
            assertEquals(1, Sort.RECEIVED_ASC.ordinal());
            assertEquals(2, Sort.QTY_DESC.ordinal());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            StockBatchRow row = new StockBatchRow(null, null, null, LocalDate.now(), LocalDateTime.now(), 50, null);

            // Then
            assertNull(row.code());
            assertNull(row.name());
            assertNull(row.batchId());
            assertNull(row.location());
            assertEquals(50, row.qty());
        }

        @Test
        @DisplayName("Should handle null date fields")
        void shouldHandleNullDateFields() {
            // When
            StockBatchRow row = new StockBatchRow("PROD001", "Product", "BATCH001", null, null, 75, "SHELF");

            // Then
            assertNull(row.expiry());
            assertNull(row.receivedAt());
            assertEquals("PROD001", row.code());
            assertEquals(75, row.qty());
        }

        @Test
        @DisplayName("Should handle zero and negative quantities")
        void shouldHandleZeroAndNegativeQuantities() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(30);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(1);

            // When
            StockBatchRow zeroBatch = new StockBatchRow("PROD001", "Empty Batch", "BATCH001", expiry, receivedAt, 0, "SHELF");
            StockBatchRow negativeBatch = new StockBatchRow("PROD002", "Correction", "BATCH002", expiry, receivedAt, -10, "MAIN_STORE");

            // Then
            assertEquals(0, zeroBatch.qty());
            assertEquals(-10, negativeBatch.qty());
        }

        @Test
        @DisplayName("Should handle past expiry dates")
        void shouldHandlePastExpiryDates() {
            // Given
            LocalDate pastExpiry = LocalDate.now().minusDays(10);
            LocalDateTime receivedAt = LocalDateTime.now().minusDays(20);

            // When
            StockBatchRow expiredBatch = new StockBatchRow("PROD001", "Expired Product", "BATCH001",
                pastExpiry, receivedAt, 15, "SHELF");

            // Then
            assertTrue(expiredBatch.expiry().isBefore(LocalDate.now()));
            assertEquals(15, expiredBatch.qty());
        }

        @Test
        @DisplayName("Should handle future received dates")
        void shouldHandleFutureReceivedDates() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(60);
            LocalDateTime futureReceived = LocalDateTime.now().plusDays(1); // Future date (edge case)

            // When
            StockBatchRow futureBatch = new StockBatchRow("PROD001", "Future Batch", "BATCH001",
                expiry, futureReceived, 50, "MAIN_STORE");

            // Then
            assertTrue(futureBatch.receivedAt().isAfter(LocalDateTime.now()));
            assertEquals(50, futureBatch.qty());
        }
    }
}
