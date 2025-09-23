package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.BillLineRow;

import java.math.BigDecimal;

@DisplayName("BillLineRow DTO Tests")
class BillLineRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create BillLineRow with all parameters")
        void shouldCreateBillLineRowWithAllParameters() {
            // Given
            String productCode = "PROD001";
            String name = "Laptop";
            BigDecimal unitPrice = new BigDecimal("999.99");
            int qty = 2;
            BigDecimal lineTotal = new BigDecimal("1999.98");

            // When
            BillLineRow row = new BillLineRow(productCode, name, unitPrice, qty, lineTotal);

            // Then
            assertEquals(productCode, row.productCode());
            assertEquals(name, row.name());
            assertEquals(unitPrice, row.unitPrice());
            assertEquals(qty, row.qty());
            assertEquals(lineTotal, row.lineTotal());
        }

        @Test
        @DisplayName("Should handle single quantity items")
        void shouldHandleSingleQuantityItems() {
            // When
            BillLineRow row = new BillLineRow("PROD001", "Single Item", new BigDecimal("50.00"), 1, new BigDecimal("50.00"));

            // Then
            assertEquals(1, row.qty());
            assertEquals(row.unitPrice(), row.lineTotal());
        }

        @Test
        @DisplayName("Should handle zero quantity items")
        void shouldHandleZeroQuantityItems() {
            // When
            BillLineRow row = new BillLineRow("PROD001", "Zero Qty Item", new BigDecimal("50.00"), 0, BigDecimal.ZERO);

            // Then
            assertEquals(0, row.qty());
            assertEquals(BigDecimal.ZERO, row.lineTotal());
        }

        @Test
        @DisplayName("Should handle large quantities")
        void shouldHandleLargeQuantities() {
            // Given
            int largeQty = Integer.MAX_VALUE;
            BigDecimal unitPrice = new BigDecimal("1.00");
            BigDecimal lineTotal = new BigDecimal(String.valueOf(largeQty));

            // When
            BillLineRow row = new BillLineRow("PROD001", "Bulk Item", unitPrice, largeQty, lineTotal);

            // Then
            assertEquals(largeQty, row.qty());
            assertEquals(lineTotal, row.lineTotal());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            BillLineRow row1 = new BillLineRow("PROD001", "Product", new BigDecimal("10.00"), 2, new BigDecimal("20.00"));
            BillLineRow row2 = new BillLineRow("PROD001", "Product", new BigDecimal("10.00"), 2, new BigDecimal("20.00"));

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            BillLineRow row1 = new BillLineRow("PROD001", "Product", new BigDecimal("10.00"), 2, new BigDecimal("20.00"));
            BillLineRow row2 = new BillLineRow("PROD002", "Product", new BigDecimal("10.00"), 2, new BigDecimal("20.00"));

            // Then
            assertNotEquals(row1, row2);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            BillLineRow row = new BillLineRow(null, null, new BigDecimal("10.00"), 1, new BigDecimal("10.00"));

            // Then
            assertNull(row.productCode());
            assertNull(row.name());
            assertEquals(new BigDecimal("10.00"), row.unitPrice());
        }

        @Test
        @DisplayName("Should handle null BigDecimal fields")
        void shouldHandleNullBigDecimalFields() {
            // When
            BillLineRow row = new BillLineRow("PROD001", "Product", null, 1, null);

            // Then
            assertEquals("PROD001", row.productCode());
            assertNull(row.unitPrice());
            assertNull(row.lineTotal());
        }

        @Test
        @DisplayName("Should handle negative quantities")
        void shouldHandleNegativeQuantities() {
            // When
            BillLineRow row = new BillLineRow("PROD001", "Return Item", new BigDecimal("50.00"), -2, new BigDecimal("-100.00"));

            // Then
            assertEquals(-2, row.qty());
            assertEquals(new BigDecimal("-100.00"), row.lineTotal());
        }

        @Test
        @DisplayName("Should handle very large prices")
        void shouldHandleVeryLargePrices() {
            // Given
            BigDecimal largePrice = new BigDecimal("999999.99");
            BigDecimal largeTotal = new BigDecimal("1999999.98");

            // When
            BillLineRow row = new BillLineRow("PROD001", "Expensive Item", largePrice, 2, largeTotal);

            // Then
            assertEquals(largePrice, row.unitPrice());
            assertEquals(largeTotal, row.lineTotal());
        }
    }
}
