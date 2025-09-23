package domain.billing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import domain.billing.Bill;
import domain.billing.BillLine;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("Bill Domain Entity Tests (aligned with current API)")
class BillTest {

    private BillLine line1;
    private BillLine line2;

    @BeforeEach
    void setUp() {
        line1 = new BillLine(new Code("PROD001"), "Laptop", new Quantity(1), Money.of(new BigDecimal("999.99")));
        line2 = new BillLine(new Code("PROD002"), "Mouse", new Quantity(2), Money.of(new BigDecimal("29.99")));
    }

    @Test
    @DisplayName("Should build Bill with valid parameters and compute totals")
    void shouldBuildBillWithValidParametersAndComputeTotals() {
        // When
        Bill bill = new Bill.Builder()
            .serial("BILL001")
            .addLine(line1)
            .addLine(line2)
            .discount(Money.of(new BigDecimal("0.00")))
            .cash(Money.of(new BigDecimal("2000.00")))
            .build();

        // Then
        assertNotNull(bill);
        assertEquals("BILL001", bill.serial());
        assertTrue(bill.dateTime().isBefore(LocalDateTime.now()) || bill.dateTime().isEqual(LocalDateTime.now()));
        assertEquals(2, bill.lines().size());

        // Subtotal = 999.99 + (2 * 29.99) = 1059.97
        assertEquals(new BigDecimal("1059.97"), bill.subtotal().amount());
        assertEquals(new BigDecimal("0.00"), bill.discount().amount());
        assertEquals(new BigDecimal("1059.97"), bill.total().amount());

        // Cash provided 2000.00 => change 940.03
        assertEquals(new BigDecimal("940.03"), bill.change().amount());
    }

    @Test
    @DisplayName("Should auto-generate serial if none provided")
    void shouldAutoGenerateSerialIfNoneProvided() {
        // When
        Bill bill = new Bill.Builder()
            .addLine(line1)
            .cash(Money.of(new BigDecimal("1000.00")))
            .build();

        // Then
        assertNotNull(bill.serial());
        assertFalse(bill.serial().isBlank());
    }

    @Test
    @DisplayName("Should not allow discount greater than subtotal")
    void shouldNotAllowDiscountGreaterThanSubtotal() {
        // Given subtotal 1059.97
        var builder = new Bill.Builder()
            .serial("BILL002")
            .addLine(line1)
            .addLine(line2)
            .discount(Money.of(new BigDecimal("2000.00")))
            .cash(Money.of(new BigDecimal("3000.00")));

        // When & Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Discount > subtotal", ex.getMessage());
    }

    @Test
    @DisplayName("Should not allow cash less than total")
    void shouldNotAllowCashLessThanTotal() {
        // Given subtotal 1059.97, discount 0 => total 1059.97
        var builder = new Bill.Builder()
            .serial("BILL003")
            .addLine(line1)
            .addLine(line2)
            .discount(Money.of(new BigDecimal("0.00")))
            .cash(Money.of(new BigDecimal("1000.00")));

        // When & Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Cash < total", ex.getMessage());
    }
}
