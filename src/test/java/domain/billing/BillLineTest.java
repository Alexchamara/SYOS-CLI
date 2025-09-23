package domain.billing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import domain.billing.BillLine;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;
import java.math.BigDecimal;

@DisplayName("BillLine Domain Entity Tests")
class BillLineTest {

    private Code productCode;
    private String productName;
    private Money unitPrice;
    private Quantity quantity;

    @BeforeEach
    void setUp() {
        productCode = new Code("PROD001");
        productName = "Laptop";
        unitPrice = Money.of(new BigDecimal("50.00"));
        quantity = new Quantity(2);
    }

    @Test
    @DisplayName("Should create BillLine with valid parameters")
    void shouldCreateBillLineWithValidParameters() {
        // When
        BillLine billLine = new BillLine(productCode, productName, quantity, unitPrice);

        // Then
        assertEquals(productCode, billLine.productCode());
        assertEquals(productName, billLine.name());
        assertEquals(quantity, billLine.qty());
        assertEquals(unitPrice, billLine.unitPrice());
    }

    @Test
    @DisplayName("Should calculate line total correctly")
    void shouldCalculateLineTotalCorrectly() {
        // Given
        BillLine billLine = new BillLine(productCode, productName, quantity, unitPrice);

        // When
        Money lineTotal = billLine.lineTotal();

        // Then
        assertEquals(Money.of(new BigDecimal("100.00")), lineTotal); // 50.00 * 2 = 100.00
    }

    @Test
    @DisplayName("Should handle single quantity")
    void shouldHandleSingleQuantity() {
        // Given
        Quantity singleQuantity = new Quantity(1);
        BillLine billLine = new BillLine(productCode, productName, singleQuantity, unitPrice);

        // When
        Money lineTotal = billLine.lineTotal();

        // Then
        assertEquals(unitPrice, lineTotal);
    }

    @Test
    @DisplayName("Should handle zero quantity")
    void shouldHandleZeroQuantity() {
        // Given
        Quantity zeroQuantity = new Quantity(0);
        BillLine billLine = new BillLine(productCode, productName, zeroQuantity, unitPrice);

        // When
        Money lineTotal = billLine.lineTotal();

        // Then
        assertEquals(Money.of(0L), lineTotal);
    }

    @Test
    @DisplayName("Should handle different product codes")
    void shouldHandleDifferentProductCodes() {
        // Given
        Code code1 = new Code("PROD001");
        Code code2 = new Code("PROD002");

        // When
        BillLine billLine1 = new BillLine(code1, "Laptop", quantity, unitPrice);
        BillLine billLine2 = new BillLine(code2, "Mouse", quantity, unitPrice);

        // Then
        assertEquals(code1, billLine1.productCode());
        assertEquals(code2, billLine2.productCode());
        assertNotEquals(billLine1.productCode(), billLine2.productCode());
    }

    @Test
    @DisplayName("Should handle different product names")
    void shouldHandleDifferentProductNames() {
        // Given
        String name1 = "Laptop";
        String name2 = "Desktop";

        // When
        BillLine billLine1 = new BillLine(productCode, name1, quantity, unitPrice);
        BillLine billLine2 = new BillLine(productCode, name2, quantity, unitPrice);

        // Then
        assertEquals(name1, billLine1.name());
        assertEquals(name2, billLine2.name());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10, 100})
    @DisplayName("Should calculate correct line totals for different quantities")
    void shouldCalculateCorrectLineTotalsForDifferentQuantities(int qty) {
        // Given
        Quantity testQuantity = new Quantity(qty);
        BillLine billLine = new BillLine(productCode, productName, testQuantity, unitPrice);

        // When
        Money lineTotal = billLine.lineTotal();

        // Then
        Money expectedTotal = unitPrice.times(qty);
        assertEquals(expectedTotal, lineTotal);
    }

    @Test
    @DisplayName("Should handle different unit prices")
    void shouldHandleDifferentUnitPrices() {
        // Given
        Money price1 = Money.of(new BigDecimal("10.00"));
        Money price2 = Money.of(new BigDecimal("20.00"));

        // When
        BillLine billLine1 = new BillLine(productCode, productName, quantity, price1);
        BillLine billLine2 = new BillLine(productCode, productName, quantity, price2);

        // Then
        assertEquals(Money.of(new BigDecimal("20.00")), billLine1.lineTotal()); // 10.00 * 2
        assertEquals(Money.of(new BigDecimal("40.00")), billLine2.lineTotal()); // 20.00 * 2
    }
}
