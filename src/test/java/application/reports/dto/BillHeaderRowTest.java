package application.reports.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import application.reports.dto.BillHeaderRow;
import application.reports.dto.BillHeaderRow.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("BillHeaderRow DTO Tests")
class BillHeaderRowTest {

    @Nested
    @DisplayName("Record Creation Tests")
    class RecordCreationTests {

        @Test
        @DisplayName("Should create BillHeaderRow with all parameters")
        void shouldCreateBillHeaderRowWithAllParameters() {
            // Given
            int rowNo = 1;
            long orderId = 101L;
            String serial = "C-000001";
            String type = "COUNTER";
            String store = "Main Store";
            LocalDateTime createdAt = LocalDateTime.now();
            BigDecimal netTotal = new BigDecimal("150.75");
            String paymentSummary = "Cash: $150.75";

            // When
            BillHeaderRow row = new BillHeaderRow(rowNo, orderId, serial, type, store, createdAt, netTotal, paymentSummary);

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
        @DisplayName("Should handle different bill types")
        void shouldHandleDifferentBillTypes() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            BillHeaderRow counterBill = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Main Store",
                now, new BigDecimal("100.00"), "Cash: $100.00");
            BillHeaderRow webBill = new BillHeaderRow(2, 102L, "W-000001", "WEB", "Online Store",
                now, new BigDecimal("200.00"), "Card: ****1234");

            // Then
            assertEquals("COUNTER", counterBill.type());
            assertEquals("WEB", webBill.type());
            assertTrue(counterBill.serial().startsWith("C-"));
            assertTrue(webBill.serial().startsWith("W-"));
        }

        @Test
        @DisplayName("Should handle different payment summaries")
        void shouldHandleDifferentPaymentSummaries() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            BillHeaderRow cashBill = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store",
                now, new BigDecimal("50.00"), "Cash: $50.00");
            BillHeaderRow cardBill = new BillHeaderRow(2, 102L, "W-000001", "WEB", "Store",
                now, new BigDecimal("75.00"), "Card: ****5678, Approved");
            BillHeaderRow mixedBill = new BillHeaderRow(3, 103L, "C-000002", "COUNTER", "Store",
                now, new BigDecimal("125.00"), "Cash: $100.00, Card: $25.00");

            // Then
            assertTrue(cashBill.paymentSummary().contains("Cash"));
            assertTrue(cardBill.paymentSummary().contains("Card"));
            assertTrue(cardBill.paymentSummary().contains("****"));
            assertTrue(mixedBill.paymentSummary().contains("Cash") && mixedBill.paymentSummary().contains("Card"));
        }

        @Test
        @DisplayName("Should handle different row numbers")
        void shouldHandleDifferentRowNumbers() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            BillHeaderRow row1 = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store",
                now, new BigDecimal("100.00"), "Cash: $100.00");
            BillHeaderRow row100 = new BillHeaderRow(100, 200L, "C-000100", "COUNTER", "Store",
                now, new BigDecimal("200.00"), "Cash: $200.00");

            // Then
            assertEquals(1, row1.rowNo());
            assertEquals(100, row100.rowNo());
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
            BillHeaderRow row1 = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store",
                createdAt, new BigDecimal("100.00"), "Cash: $100.00");
            BillHeaderRow row2 = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store",
                createdAt, new BigDecimal("100.00"), "Cash: $100.00");

            // Then
            assertEquals(row1, row2);
            assertEquals(row1.hashCode(), row2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            BillHeaderRow row1 = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store",
                createdAt, new BigDecimal("100.00"), "Cash: $100.00");
            BillHeaderRow row2 = new BillHeaderRow(2, 101L, "C-000001", "COUNTER", "Store",
                createdAt, new BigDecimal("100.00"), "Cash: $100.00");

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
        @DisplayName("Should convert sort enum to string correctly")
        void shouldConvertSortEnumToStringCorrectly() {
            // Then
            assertEquals("SERIAL_ASC", Sort.SERIAL_ASC.toString());
            assertEquals("SERIAL_DESC", Sort.SERIAL_DESC.toString());
            assertEquals("DATE_ASC", Sort.DATE_ASC.toString());
            assertEquals("DATE_DESC", Sort.DATE_DESC.toString());
            assertEquals("NET_DESC", Sort.NET_DESC.toString());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null string fields")
        void shouldHandleNullStringFields() {
            // When
            BillHeaderRow row = new BillHeaderRow(1, 101L, null, null, null,
                LocalDateTime.now(), new BigDecimal("100.00"), null);

            // Then
            assertNull(row.serial());
            assertNull(row.type());
            assertNull(row.store());
            assertNull(row.paymentSummary());
            assertEquals(101L, row.orderId());
        }

        @Test
        @DisplayName("Should handle null datetime and decimal fields")
        void shouldHandleNullDatetimeAndDecimalFields() {
            // When
            BillHeaderRow row = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store", null, null, "Cash");

            // Then
            assertNull(row.createdAt());
            assertNull(row.netTotal());
            assertEquals("Cash", row.paymentSummary());
        }

        @Test
        @DisplayName("Should handle negative order IDs")
        void shouldHandleNegativeOrderIds() {
            // When
            BillHeaderRow row = new BillHeaderRow(1, -1L, "C-000001", "COUNTER", "Store",
                LocalDateTime.now(), new BigDecimal("100.00"), "Cash: $100.00");

            // Then
            assertEquals(-1L, row.orderId());
        }

        @Test
        @DisplayName("Should handle zero and negative net totals")
        void shouldHandleZeroAndNegativeNetTotals() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            BillHeaderRow zeroRow = new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Store",
                now, BigDecimal.ZERO, "Free items");
            BillHeaderRow negativeRow = new BillHeaderRow(2, 102L, "C-000002", "COUNTER", "Store",
                now, new BigDecimal("-50.00"), "Refund: $50.00");

            // Then
            assertEquals(BigDecimal.ZERO, zeroRow.netTotal());
            assertEquals(new BigDecimal("-50.00"), negativeRow.netTotal());
        }

        @Test
        @DisplayName("Should handle very long serial numbers")
        void shouldHandleVeryLongSerialNumbers() {
            // Given
            String longSerial = "VERY-LONG-SERIAL-NUMBER-" + "X".repeat(100);

            // When
            BillHeaderRow row = new BillHeaderRow(1, 101L, longSerial, "COUNTER", "Store",
                LocalDateTime.now(), new BigDecimal("100.00"), "Cash: $100.00");

            // Then
            assertEquals(longSerial, row.serial());
            assertTrue(row.serial().length() > 100);
        }
    }
}
