package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.DailySalesRow;
import application.reports.dto.DailySalesRow.Sort;

import java.math.BigDecimal;

@DisplayName("DailySalesRow DTO Tests")
class DailySalesRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create DailySalesRow with all parameters")
        void shouldCreateDailySalesRowWithAllParameters() {
            // Given
            String code = "PROD001";
            String name = "Laptop";
            String location = "SHELF";
            int qtySold = 5;
            BigDecimal gross = new BigDecimal("500.00");
            BigDecimal discount = new BigDecimal("50.00");
            BigDecimal net = new BigDecimal("450.00");

            // When
            DailySalesRow row = new DailySalesRow(code, name, location, qtySold, gross, discount, net);

            // Then
            assertEquals(code, row.code());
            assertEquals(name, row.name());
            assertEquals(location, row.location());
            assertEquals(qtySold, row.qtySold());
            assertEquals(gross, row.gross());
            assertEquals(discount, row.discount());
            assertEquals(net, row.net());
        }

        @Test
        @DisplayName("Should handle zero quantities and amounts")
        void shouldHandleZeroQuantitiesAndAmounts() {
            // When
            DailySalesRow row = new DailySalesRow("PROD001", "Product", "SHELF", 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            // Then
            assertEquals(0, row.qtySold());
            assertEquals(BigDecimal.ZERO, row.gross());
            assertEquals(BigDecimal.ZERO, row.discount());
            assertEquals(BigDecimal.ZERO, row.net());
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() {
            // Given
            DailySalesRow shelfRow = new DailySalesRow("PROD001", "Product", "SHELF", 5,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
            DailySalesRow webRow = new DailySalesRow("PROD002", "Product", "WEB", 3,
                new BigDecimal("75.00"), BigDecimal.ZERO, new BigDecimal("75.00"));
            DailySalesRow mainStoreRow = new DailySalesRow("PROD003", "Product", "MAIN_STORE", 2,
                new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"));

            // Then
            assertEquals("SHELF", shelfRow.location());
            assertEquals("WEB", webRow.location());
            assertEquals("MAIN_STORE", mainStoreRow.location());
        }

        @Test
        @DisplayName("Should handle large quantities and amounts")
        void shouldHandleLargeQuantitiesAndAmounts() {
            // When
            DailySalesRow row = new DailySalesRow("PROD001", "High Volume Product", "SHELF",
                Integer.MAX_VALUE, new BigDecimal("999999.99"), new BigDecimal("99999.99"), new BigDecimal("900000.00"));

            // Then
            assertEquals(Integer.MAX_VALUE, row.qtySold());
            assertEquals(new BigDecimal("999999.99"), row.gross());
            assertEquals(new BigDecimal("99999.99"), row.discount());
            assertEquals(new BigDecimal("900000.00"), row.net());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            DailySalesRow row1 = new DailySalesRow("PROD001", "Product", "SHELF", 5,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"));
            DailySalesRow row2 = new DailySalesRow("PROD001", "Product", "SHELF", 5,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"));

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            DailySalesRow row1 = new DailySalesRow("PROD001", "Product", "SHELF", 5,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"));
            DailySalesRow row2 = new DailySalesRow("PROD002", "Product", "SHELF", 5,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"));

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
            assertEquals(Sort.QTY_DESC, Sort.valueOf("QTY_DESC"));
            assertEquals(Sort.REVENUE_DESC, Sort.valueOf("REVENUE_DESC"));
            assertEquals(Sort.NAME_ASC, Sort.valueOf("NAME_ASC"));
            assertEquals(Sort.LOCATION_ASC, Sort.valueOf("LOCATION_ASC"));
        }

        @Test
        @DisplayName("Should convert sort enum to string correctly")
        void shouldConvertSortEnumToStringCorrectly() {
            // Then
            assertEquals("QTY_DESC", Sort.QTY_DESC.toString());
            assertEquals("REVENUE_DESC", Sort.REVENUE_DESC.toString());
            assertEquals("NAME_ASC", Sort.NAME_ASC.toString());
            assertEquals("LOCATION_ASC", Sort.LOCATION_ASC.toString());
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should have meaningful toString representation")
        void shouldHaveMeaningfulToStringRepresentation() {
            // Given
            DailySalesRow row = new DailySalesRow("PROD001", "Laptop", "SHELF", 5,
                new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("450.00"));

            // When
            String result = row.toString();

            // Then
            assertNotNull(result);
            assertTrue(result.contains("PROD001"));
            assertTrue(result.contains("Laptop"));
            assertTrue(result.contains("SHELF"));
            assertTrue(result.contains("5"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            DailySalesRow row = new DailySalesRow(null, null, null, 5,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));

            // Then
            assertNull(row.code());
            assertNull(row.name());
            assertNull(row.location());
            assertEquals(5, row.qtySold());
        }

        @Test
        @DisplayName("Should handle null BigDecimal fields")
        void shouldHandleNullBigDecimalFields() {
            // When
            DailySalesRow row = new DailySalesRow("PROD001", "Product", "SHELF", 5, null, null, null);

            // Then
            assertEquals("PROD001", row.code());
            assertNull(row.gross());
            assertNull(row.discount());
            assertNull(row.net());
        }

        @Test
        @DisplayName("Should handle negative quantities")
        void shouldHandleNegativeQuantities() {
            // When
            DailySalesRow row = new DailySalesRow("PROD001", "Product", "SHELF", -5,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));

            // Then
            assertEquals(-5, row.qtySold());
        }

        @Test
        @DisplayName("Should handle very long product names")
        void shouldHandleVeryLongProductNames() {
            // Given
            String longName = "A".repeat(1000);

            // When
            DailySalesRow row = new DailySalesRow("PROD001", longName, "SHELF", 1,
                new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("10.00"));

            // Then
            assertEquals(longName, row.name());
            assertEquals(1000, row.name().length());
        }
    }
}
