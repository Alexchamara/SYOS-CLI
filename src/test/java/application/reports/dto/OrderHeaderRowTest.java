package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.OrderHeaderRow;
import application.reports.dto.OrderHeaderRow.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("OrderHeaderRow DTO Tests")
class OrderHeaderRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create OrderHeaderRow with all parameters")
        void shouldCreateOrderHeaderRowWithAllParameters() {
            // Given
            int rowNo = 1;
            long orderId = 201L;
            long serial = 1001L;
            String type = "WEB";
            String store = "Online Store";
            LocalDateTime createdAt = LocalDateTime.now();
            BigDecimal netTotal = new BigDecimal("299.99");
            String paymentSummary = "Card: ****1234, Approved";

            // When
            OrderHeaderRow row = new OrderHeaderRow(rowNo, orderId, serial, type, store, createdAt, netTotal, paymentSummary);

            // Then
            assertEquals(rowNo, row.rowNo());
            assertEquals(orderId, row.orderId());
            assertEquals(serial, row.serial());
            assertEquals(type, row.type());
            assertEquals(store, row.store());
            assertEquals(createdAt, row.createdAt());
            assertEquals(netTotal, row.netTotal());
            assertEquals(paymentSummary, row.paymentSummary());
        }

        @Test
        @DisplayName("Should handle different order types")
        void shouldHandleDifferentOrderTypes() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            OrderHeaderRow webOrder = new OrderHeaderRow(1, 201L, 1001L, "WEB", "Online Store",
                now, new BigDecimal("150.00"), "Card: ****1234");
            OrderHeaderRow mobileOrder = new OrderHeaderRow(2, 202L, 1002L, "MOBILE", "Mobile App",
                now, new BigDecimal("75.00"), "Digital Wallet");
            OrderHeaderRow phoneOrder = new OrderHeaderRow(3, 203L, 1003L, "PHONE", "Phone Order",
                now, new BigDecimal("200.00"), "Card: ****5678");

            // Then
            assertEquals("WEB", webOrder.type());
            assertEquals("MOBILE", mobileOrder.type());
            assertEquals("PHONE", phoneOrder.type());
        }

        @Test
        @DisplayName("Should handle different serial numbers")
        void shouldHandleDifferentSerialNumbers() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            OrderHeaderRow lowSerial = new OrderHeaderRow(1, 201L, 1L, "WEB", "Store",
                now, new BigDecimal("100.00"), "Payment");
            OrderHeaderRow highSerial = new OrderHeaderRow(2, 202L, Long.MAX_VALUE, "WEB", "Store",
                now, new BigDecimal("200.00"), "Payment");

            // Then
            assertEquals(1L, lowSerial.serial());
            assertEquals(Long.MAX_VALUE, highSerial.serial());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            OrderHeaderRow row1 = new OrderHeaderRow(1, 201L, 1001L, "WEB", "Store",
                createdAt, new BigDecimal("100.00"), "Card: ****1234");
            OrderHeaderRow row2 = new OrderHeaderRow(1, 201L, 1001L, "WEB", "Store",
                createdAt, new BigDecimal("100.00"), "Card: ****1234");

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            OrderHeaderRow row1 = new OrderHeaderRow(1, 201L, 1001L, "WEB", "Store",
                createdAt, new BigDecimal("100.00"), "Card: ****1234");
            OrderHeaderRow row2 = new OrderHeaderRow(1, 202L, 1001L, "WEB", "Store",
                createdAt, new BigDecimal("100.00"), "Card: ****1234");

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
            assertEquals(5, Sort.values().length);
            assertEquals(Sort.SERIAL_ASC, Sort.valueOf("SERIAL_ASC"));
            assertEquals(Sort.SERIAL_DESC, Sort.valueOf("SERIAL_DESC"));
            assertEquals(Sort.DATE_ASC, Sort.valueOf("DATE_ASC"));
            assertEquals(Sort.DATE_DESC, Sort.valueOf("DATE_DESC"));
            assertEquals(Sort.NET_DESC, Sort.valueOf("NET_DESC"));
        }

        @Test
        @DisplayName("Should support ordinal access")
        void shouldSupportOrdinalAccess() {
            // Then
            assertEquals(0, Sort.SERIAL_ASC.ordinal());
            assertEquals(1, Sort.SERIAL_DESC.ordinal());
            assertEquals(2, Sort.DATE_ASC.ordinal());
            assertEquals(3, Sort.DATE_DESC.ordinal());
            assertEquals(4, Sort.NET_DESC.ordinal());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            OrderHeaderRow row = new OrderHeaderRow(1, 201L, 1001L, null, null,
                LocalDateTime.now(), new BigDecimal("100.00"), null);

            // Then
            assertNull(row.type());
            assertNull(row.store());
            assertNull(row.paymentSummary());
            assertEquals(201L, row.orderId());
        }

        @Test
        @DisplayName("Should handle zero and negative values")
        void shouldHandleZeroAndNegativeValues() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            OrderHeaderRow zeroRow = new OrderHeaderRow(0, 0L, 0L, "WEB", "Store",
                now, BigDecimal.ZERO, "Free order");
            OrderHeaderRow negativeRow = new OrderHeaderRow(-1, -1L, -1L, "WEB", "Store",
                now, new BigDecimal("-50.00"), "Refund");

            // Then
            assertEquals(0, zeroRow.rowNo());
            assertEquals(0L, zeroRow.orderId());
            assertEquals(0L, zeroRow.serial());
            assertEquals(BigDecimal.ZERO, zeroRow.netTotal());

            assertEquals(-1, negativeRow.rowNo());
            assertEquals(-1L, negativeRow.orderId());
            assertEquals(-1L, negativeRow.serial());
            assertEquals(new BigDecimal("-50.00"), negativeRow.netTotal());
        }

        @Test
        @DisplayName("Should handle very large values")
        void shouldHandleVeryLargeValues() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            BigDecimal largeAmount = new BigDecimal("999999999.99");

            // When
            OrderHeaderRow row = new OrderHeaderRow(Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                "WEB", "Store", now, largeAmount, "Large transaction");

            // Then
            assertEquals(Integer.MAX_VALUE, row.rowNo());
            assertEquals(Long.MAX_VALUE, row.orderId());
            assertEquals(Long.MAX_VALUE, row.serial());
            assertEquals(largeAmount, row.netTotal());
        }
    }
}
