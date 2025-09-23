package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.ReshelveRow;
import application.reports.dto.ReshelveRow.Sort;

import java.time.LocalDateTime;

@DisplayName("ReshelveRow DTO Tests")
class ReshelveRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create ReshelveRow with all parameters")
        void shouldCreateReshelveRowWithAllParameters() {
            // Given
            long id = 123L;
            LocalDateTime happenedAt = LocalDateTime.now();
            String productCode = "PROD001";
            String productName = "Laptop";
            String fromLocation = "MAIN_STORE";
            String toLocation = "SHELF";
            int quantity = 25;
            String note = "Manual transfer to shelf";

            // When
            ReshelveRow row = new ReshelveRow(id, happenedAt, productCode, productName, fromLocation, toLocation, quantity, note);

            // Then
            assertEquals(id, row.id());
            assertEquals(happenedAt, row.happenedAt());
            assertEquals(productCode, row.productCode());
            assertEquals(productName, row.productName());
            assertEquals(fromLocation, row.fromLocation());
            assertEquals(toLocation, row.toLocation());
            assertEquals(quantity, row.quantity());
            assertEquals(note, row.note());
        }

        @Test
        @DisplayName("Should handle different transfer directions")
        void shouldHandleDifferentTransferDirections() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            ReshelveRow mainToShelf = new ReshelveRow(1L, now, "PROD001", "Product", "MAIN_STORE", "SHELF", 50, "To shelf");
            ReshelveRow shelfToWeb = new ReshelveRow(2L, now, "PROD002", "Product", "SHELF", "WEB", 30, "To web");
            ReshelveRow webToMain = new ReshelveRow(3L, now, "PROD003", "Product", "WEB", "MAIN_STORE", 20, "To main");

            // Then
            assertEquals("MAIN_STORE", mainToShelf.fromLocation());
            assertEquals("SHELF", mainToShelf.toLocation());
            assertEquals("SHELF", shelfToWeb.fromLocation());
            assertEquals("WEB", shelfToWeb.toLocation());
            assertEquals("WEB", webToMain.fromLocation());
            assertEquals("MAIN_STORE", webToMain.toLocation());
        }

        @Test
        @DisplayName("Should handle different quantity values")
        void shouldHandleDifferentQuantityValues() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            ReshelveRow smallTransfer = new ReshelveRow(1L, now, "PROD001", "Product", "MAIN_STORE", "SHELF", 1, "Small transfer");
            ReshelveRow largeTransfer = new ReshelveRow(2L, now, "PROD002", "Product", "MAIN_STORE", "WEB", 1000, "Large transfer");
            ReshelveRow zeroTransfer = new ReshelveRow(3L, now, "PROD003", "Product", "SHELF", "WEB", 0, "Zero transfer");

            // Then
            assertEquals(1, smallTransfer.quantity());
            assertEquals(1000, largeTransfer.quantity());
            assertEquals(0, zeroTransfer.quantity());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            LocalDateTime happenedAt = LocalDateTime.now();
            ReshelveRow row1 = new ReshelveRow(1L, happenedAt, "PROD001", "Product", "MAIN_STORE", "SHELF", 25, "Transfer");
            ReshelveRow row2 = new ReshelveRow(1L, happenedAt, "PROD001", "Product", "MAIN_STORE", "SHELF", 25, "Transfer");

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            LocalDateTime happenedAt = LocalDateTime.now();
            ReshelveRow row1 = new ReshelveRow(1L, happenedAt, "PROD001", "Product", "MAIN_STORE", "SHELF", 25, "Transfer");
            ReshelveRow row2 = new ReshelveRow(2L, happenedAt, "PROD001", "Product", "MAIN_STORE", "SHELF", 25, "Transfer");

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
            assertEquals(4, Sort.values().length);
            assertEquals(Sort.TIME_DESC, Sort.valueOf("TIME_DESC"));
            assertEquals(Sort.TIME_ASC, Sort.valueOf("TIME_ASC"));
            assertEquals(Sort.PRODUCT_ASC, Sort.valueOf("PRODUCT_ASC"));
            assertEquals(Sort.QUANTITY_DESC, Sort.valueOf("QUANTITY_DESC"));
        }

        @Test
        @DisplayName("Should convert sort enum to string correctly")
        void shouldConvertSortEnumToStringCorrectly() {
            // Then
            assertEquals("TIME_DESC", Sort.TIME_DESC.toString());
            assertEquals("TIME_ASC", Sort.TIME_ASC.toString());
            assertEquals("PRODUCT_ASC", Sort.PRODUCT_ASC.toString());
            assertEquals("QUANTITY_DESC", Sort.QUANTITY_DESC.toString());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            ReshelveRow row = new ReshelveRow(1L, LocalDateTime.now(), null, null, null, null, 10, null);

            // Then
            assertNull(row.productCode());
            assertNull(row.productName());
            assertNull(row.fromLocation());
            assertNull(row.toLocation());
            assertNull(row.note());
            assertEquals(10, row.quantity());
        }

        @Test
        @DisplayName("Should handle null datetime")
        void shouldHandleNullDatetime() {
            // When
            ReshelveRow row = new ReshelveRow(1L, null, "PROD001", "Product", "MAIN_STORE", "SHELF", 10, "Transfer");

            // Then
            assertNull(row.happenedAt());
            assertEquals("PROD001", row.productCode());
        }

        @Test
        @DisplayName("Should handle negative IDs and quantities")
        void shouldHandleNegativeIdsAndQuantities() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            ReshelveRow row = new ReshelveRow(-1L, now, "PROD001", "Product", "SHELF", "MAIN_STORE", -5, "Correction");

            // Then
            assertEquals(-1L, row.id());
            assertEquals(-5, row.quantity());
        }

        @Test
        @DisplayName("Should handle very large IDs and quantities")
        void shouldHandleVeryLargeIdsAndQuantities() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            ReshelveRow row = new ReshelveRow(Long.MAX_VALUE, now, "PROD001", "Product",
                "MAIN_STORE", "WEB", Integer.MAX_VALUE, "Massive transfer");

            // Then
            assertEquals(Long.MAX_VALUE, row.id());
            assertEquals(Integer.MAX_VALUE, row.quantity());
        }

        @Test
        @DisplayName("Should handle empty string fields")
        void shouldHandleEmptyStringFields() {
            // When
            ReshelveRow row = new ReshelveRow(1L, LocalDateTime.now(), "", "", "", "", 10, "");

            // Then
            assertEquals("", row.productCode());
            assertEquals("", row.productName());
            assertEquals("", row.fromLocation());
            assertEquals("", row.toLocation());
            assertEquals("", row.note());
        }

        @Test
        @DisplayName("Should handle very long notes")
        void shouldHandleVeryLongNotes() {
            // Given
            String longNote = "This is a very detailed note about the reshelving operation that includes multiple reasons and justifications. ".repeat(10);

            // When
            ReshelveRow row = new ReshelveRow(1L, LocalDateTime.now(), "PROD001", "Product",
                "MAIN_STORE", "SHELF", 25, longNote);

            // Then
            assertEquals(longNote, row.note());
            assertTrue(row.note().length() > 1000);
        }
    }
}
