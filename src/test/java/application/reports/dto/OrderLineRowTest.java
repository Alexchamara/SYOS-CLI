package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.OrderLineRow;

import java.math.BigDecimal;

@DisplayName("OrderLineRow DTO Tests")
class OrderLineRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create OrderLineRow with all parameters")
        void shouldCreateOrderLineRowWithAllParameters() {
            // Given
            String productCode = "PROD001";
            String name = "Gaming Laptop";
            BigDecimal unitPrice = new BigDecimal("1299.99");
            int qty = 1;
            BigDecimal lineTotal = new BigDecimal("1299.99");

            // When
            OrderLineRow row = new OrderLineRow(productCode, name, unitPrice, qty, lineTotal);

            // Then
            assertEquals(productCode, row.productCode());
            assertEquals(name, row.name());
            assertEquals(unitPrice, row.unitPrice());
            assertEquals(qty, row.qty());
            assertEquals(lineTotal, row.lineTotal());
        }

        @Test
        @DisplayName("Should handle multiple quantity orders")
        void shouldHandleMultipleQuantityOrders() {
            // When
            OrderLineRow row = new OrderLineRow("PROD002", "Wireless Mouse", new BigDecimal("29.99"), 3, new BigDecimal("89.97"));

            // Then
            assertEquals(3, row.qty());
            assertEquals(new BigDecimal("89.97"), row.lineTotal());
        }

        @Test
        @DisplayName("Should handle high-value items")
        void shouldHandleHighValueItems() {
            // When
            OrderLineRow row = new OrderLineRow("PROD003", "Professional Workstation",
                new BigDecimal("4999.99"), 2, new BigDecimal("9999.98"));

            // Then
            assertEquals(new BigDecimal("4999.99"), row.unitPrice());
            assertEquals(new BigDecimal("9999.98"), row.lineTotal());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            OrderLineRow row1 = new OrderLineRow("PROD001", "Product", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));
            OrderLineRow row2 = new OrderLineRow("PROD001", "Product", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when product codes differ")
        void shouldNotBeEqualWhenProductCodesDiffer() {
            // Given
            OrderLineRow row1 = new OrderLineRow("PROD001", "Product", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));
            OrderLineRow row2 = new OrderLineRow("PROD002", "Product", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));

            // Then
            assertNotEquals(row1, row2);
        }

        @Test
        @DisplayName("Should not be equal when quantities differ")
        void shouldNotBeEqualWhenQuantitiesDiffer() {
            // Given
            OrderLineRow row1 = new OrderLineRow("PROD001", "Product", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));
            OrderLineRow row2 = new OrderLineRow("PROD001", "Product", new BigDecimal("25.00"), 3, new BigDecimal("75.00"));

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
            OrderLineRow row = new OrderLineRow(null, null, new BigDecimal("10.00"), 1, new BigDecimal("10.00"));

            // Then
            assertNull(row.productCode());
            assertNull(row.name());
            assertEquals(new BigDecimal("10.00"), row.unitPrice());
        }

        @Test
        @DisplayName("Should handle null BigDecimal fields")
        void shouldHandleNullBigDecimalFields() {
            // When
            OrderLineRow row = new OrderLineRow("PROD001", "Product", null, 1, null);

            // Then
            assertEquals("PROD001", row.productCode());
            assertNull(row.unitPrice());
            assertNull(row.lineTotal());
        }

        @Test
        @DisplayName("Should handle zero quantity orders")
        void shouldHandleZeroQuantityOrders() {
            // When
            OrderLineRow row = new OrderLineRow("PROD001", "Cancelled Item", new BigDecimal("50.00"), 0, BigDecimal.ZERO);

            // Then
            assertEquals(0, row.qty());
            assertEquals(BigDecimal.ZERO, row.lineTotal());
        }

        @Test
        @DisplayName("Should handle negative quantities for returns")
        void shouldHandleNegativeQuantitiesForReturns() {
            // When
            OrderLineRow row = new OrderLineRow("PROD001", "Returned Item", new BigDecimal("25.00"), -2, new BigDecimal("-50.00"));

            // Then
            assertEquals(-2, row.qty());
            assertEquals(new BigDecimal("-50.00"), row.lineTotal());
        }

        @Test
        @DisplayName("Should handle fractional prices")
        void shouldHandleFractionalPrices() {
            // When
            OrderLineRow row = new OrderLineRow("PROD001", "Fractional Price Item",
                new BigDecimal("10.333"), 3, new BigDecimal("30.999"));

            // Then
            assertEquals(new BigDecimal("10.333"), row.unitPrice());
            assertEquals(new BigDecimal("30.999"), row.lineTotal());
        }

        @Test
        @DisplayName("Should handle very long product names")
        void shouldHandleVeryLongProductNames() {
            // Given
            String longName = "Ultra Premium Professional Gaming Laptop with Advanced Graphics Card and Extended Warranty Coverage Plus Additional Accessories Bundle".repeat(5);

            // When
            OrderLineRow row = new OrderLineRow("PROD001", longName, new BigDecimal("2999.99"), 1, new BigDecimal("2999.99"));

            // Then
            assertEquals(longName, row.name());
            assertTrue(row.name().length() > 500);
        }
    }
}
