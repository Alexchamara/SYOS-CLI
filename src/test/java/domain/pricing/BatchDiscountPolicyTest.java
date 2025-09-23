package domain.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.pricing.BatchDiscountPolicy;
import application.services.DiscountService;
import domain.billing.BillLine;
import domain.shared.Money;
import domain.shared.Code;
import domain.shared.Quantity;
import domain.pricing.Discount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

@DisplayName("BatchDiscountPolicy Tests")
class BatchDiscountPolicyTest {

    @Mock
    private DiscountService discountService;

    @Mock
    private Discount mockDiscount;

    private BatchDiscountPolicy discountPolicy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        discountPolicy = new BatchDiscountPolicy(discountService);
    }

    @Test
    @DisplayName("Should create BatchDiscountPolicy with DiscountService")
    void shouldCreateBatchDiscountPolicyWithDiscountService() {
        // When
        BatchDiscountPolicy policy = new BatchDiscountPolicy(discountService);

        // Then
        assertNotNull(policy);
    }

    @Test
    @DisplayName("Should return zero discount when no active discounts")
    void shouldReturnZeroDiscountWhenNoActiveDiscounts() {
        // Given
        Code productCode = new Code("PROD001");
        Money unitPrice = Money.of(new BigDecimal("100.00"));
        Quantity quantity = new Quantity(2);
        BillLine billLine = new BillLine(productCode, "Product Name", quantity, unitPrice);
        List<BillLine> lines = List.of(billLine);

        when(discountService.getActiveDiscountsForProduct(
            eq("PROD001"), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // When
        Money discount = discountPolicy.discountFor(lines);

        // Then
        assertEquals(Money.of(0), discount);
    }

    @Test
    @DisplayName("Should apply discount when active discounts available")
    void shouldApplyDiscountWhenActiveDiscountsAvailable() {
        // Given
        Code productCode = new Code("PROD001");
        Money unitPrice = Money.of(new BigDecimal("100.00"));
        Quantity quantity = new Quantity(2);
        BillLine billLine = new BillLine(productCode, "Product Name", quantity, unitPrice);
        List<BillLine> lines = List.of(billLine);

        Money discountAmount = Money.of(new BigDecimal("10.00"));
        when(mockDiscount.calculateDiscountAmount(unitPrice)).thenReturn(discountAmount);
        when(discountService.getActiveDiscountsForProduct(
            eq("PROD001"), any(LocalDate.class)))
            .thenReturn(List.of(mockDiscount));

        // When
        Money totalDiscount = discountPolicy.discountFor(lines);

        // Then
        assertEquals(Money.of(new BigDecimal("20.00")), totalDiscount); // 10.00 * 2 quantity
    }

    @Test
    @DisplayName("Should handle multiple bill lines")
    void shouldHandleMultipleBillLines() {
        // Given
        Code productCode1 = new Code("PROD001");
        Code productCode2 = new Code("PROD002");
        Money unitPrice1 = Money.of(new BigDecimal("100.00"));
        Money unitPrice2 = Money.of(new BigDecimal("200.00"));

        BillLine billLine1 = new BillLine(productCode1, "Product 1", new Quantity(1), unitPrice1);
        BillLine billLine2 = new BillLine(productCode2, "Product 2", new Quantity(1), unitPrice2);
        List<BillLine> lines = List.of(billLine1, billLine2);

        when(discountService.getActiveDiscountsForProduct(anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // When
        Money totalDiscount = discountPolicy.discountFor(lines);

        // Then
        assertEquals(Money.of(0), totalDiscount);
        verify(discountService, times(2)).getActiveDiscountsForProduct(anyString(), any(LocalDate.class));
    }
}
