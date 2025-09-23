package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.ReorderRow;
import application.reports.dto.ReorderRow.Sort;

import java.time.LocalDate;

@DisplayName("ReorderRow DTO Tests")
class ReorderRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create ReorderRow with all parameters")
        void shouldCreateReorderRowWithAllParameters() {
            // Given
            String code = "PROD001";
            String name = "Laptop";
            String batchId = "BATCH001";
            String location = "MAIN_STORE";
            int quantity = 15;
            LocalDate expiry = LocalDate.now().plusDays(30);
            String status = "LOW";

            // When
            ReorderRow row = new ReorderRow(code, name, batchId, location, quantity, expiry, status);

            // Then
            assertEquals(code, row.code());
            assertEquals(name, row.name());
            assertEquals(batchId, row.batchId());
            assertEquals(location, row.location());
            assertEquals(quantity, row.quantity());
            assertEquals(expiry, row.expiry());
            assertEquals(status, row.status());
        }

        @Test
        @DisplayName("Should handle different status values")
        void shouldHandleDifferentStatusValues() {
            // Given
            String[] statuses = {"LOW", "CRITICAL", "OUT_OF_STOCK", "URGENT"};

            // When & Then
            for (String status : statuses) {
                ReorderRow row = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10,
                    LocalDate.now().plusDays(30), status);
                assertEquals(status, row.status());
            }
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() {
            // Given
            String[] locations = {"MAIN_STORE", "SHELF", "WEB"};

            // When & Then
            for (String location : locations) {
                ReorderRow row = new ReorderRow("PROD001", "Product", "BATCH001", location, 10,
                    LocalDate.now().plusDays(30), "LOW");
                assertEquals(location, row.location());
            }
        }

        @Test
        @DisplayName("Should handle zero and negative quantities")
        void shouldHandleZeroAndNegativeQuantities() {
            // When
            ReorderRow zeroRow = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 0,
                LocalDate.now().plusDays(30), "OUT_OF_STOCK");
            ReorderRow negativeRow = new ReorderRow("PROD002", "Product", "BATCH002", "SHELF", -5,
                LocalDate.now().plusDays(30), "ERROR");

            // Then
            assertEquals(0, zeroRow.quantity());
            assertEquals(-5, negativeRow.quantity());
        }

        @Test
        @DisplayName("Should handle different expiry dates")
        void shouldHandleDifferentExpiryDates() {
            // Given
            LocalDate nearExpiry = LocalDate.now().plusDays(5);
            LocalDate farExpiry = LocalDate.now().plusDays(365);
            LocalDate pastExpiry = LocalDate.now().minusDays(10);

            // When
            ReorderRow nearRow = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10, nearExpiry, "URGENT");
            ReorderRow farRow = new ReorderRow("PROD002", "Product", "BATCH002", "SHELF", 50, farExpiry, "LOW");
            ReorderRow expiredRow = new ReorderRow("PROD003", "Product", "BATCH003", "SHELF", 20, pastExpiry, "EXPIRED");

            // Then
            assertEquals(nearExpiry, nearRow.expiry());
            assertEquals(farExpiry, farRow.expiry());
            assertEquals(pastExpiry, expiredRow.expiry());
            assertTrue(nearRow.expiry().isBefore(farRow.expiry()));
            assertTrue(expiredRow.expiry().isBefore(LocalDate.now()));
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
            ReorderRow row1 = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10, expiry, "LOW");
            ReorderRow row2 = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10, expiry, "LOW");

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            LocalDate expiry = LocalDate.now().plusDays(30);
            ReorderRow row1 = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10, expiry, "LOW");
            ReorderRow row2 = new ReorderRow("PROD002", "Product", "BATCH001", "SHELF", 10, expiry, "LOW");

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
            assertEquals(Sort.QTY_ASC, Sort.valueOf("QTY_ASC"));
            assertEquals(Sort.LOCATION_ASC, Sort.valueOf("LOCATION_ASC"));
            assertEquals(Sort.NAME_ASC, Sort.valueOf("NAME_ASC"));
            assertEquals(Sort.EXPIRY_ASC, Sort.valueOf("EXPIRY_ASC"));
        }

        @Test
        @DisplayName("Should convert sort enum to string correctly")
        void shouldConvertSortEnumToStringCorrectly() {
            // Then
            assertEquals("QTY_ASC", Sort.QTY_ASC.toString());
            assertEquals("LOCATION_ASC", Sort.LOCATION_ASC.toString());
            assertEquals("NAME_ASC", Sort.NAME_ASC.toString());
            assertEquals("EXPIRY_ASC", Sort.EXPIRY_ASC.toString());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            ReorderRow row = new ReorderRow(null, null, null, null, 10, LocalDate.now(), null);

            // Then
            assertNull(row.code());
            assertNull(row.name());
            assertNull(row.batchId());
            assertNull(row.location());
            assertNull(row.status());
            assertEquals(10, row.quantity());
        }

        @Test
        @DisplayName("Should handle null expiry date")
        void shouldHandleNullExpiryDate() {
            // When
            ReorderRow row = new ReorderRow("PROD001", "Product", "BATCH001", "SHELF", 10, null, "LOW");

            // Then
            assertNull(row.expiry());
            assertEquals("PROD001", row.code());
        }

        @Test
        @DisplayName("Should handle empty string fields")
        void shouldHandleEmptyStringFields() {
            // When
            ReorderRow row = new ReorderRow("", "", "", "", 5, LocalDate.now(), "");

            // Then
            assertEquals("", row.code());
            assertEquals("", row.name());
            assertEquals("", row.batchId());
            assertEquals("", row.location());
            assertEquals("", row.status());
        }
    }
}
