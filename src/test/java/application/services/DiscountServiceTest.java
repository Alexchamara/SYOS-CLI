package application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.DiscountService;
import application.services.DiscountService.PricingResult;
import application.usecase.DiscountManagementUseCase;
import domain.pricing.Discount;
import domain.pricing.Discount.DiscountType;
import domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@DisplayName("DiscountService Tests")
class DiscountServiceTest {

    @Mock
    private DiscountManagementUseCase discountManagementUseCase;

    private DiscountService discountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        discountService = new DiscountService(discountManagementUseCase);
    }

    @Nested
    @DisplayName("Get Best Discount For Batch Tests")
    class GetBestDiscountForBatchTests {

        @Test
        @DisplayName("Should return best discount when multiple discounts available")
        void shouldReturnBestDiscountWhenMultipleDiscountsAvailable() {
            // Given
            long batchId = 100L;
            LocalDate date = LocalDate.now();

            Discount discount1 = new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                date, date.plusDays(30), true, "10% off", 1L);
            Discount discount2 = new Discount(2L, batchId, DiscountType.PERCENTAGE, new BigDecimal("15.00"),
                date, date.plusDays(30), true, "15% off", 1L);
            Discount discount3 = new Discount(3L, batchId, DiscountType.FIXED_AMOUNT, new BigDecimal("8.00"),
                date, date.plusDays(30), true, "8 off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(discount1, discount2, discount3));

            // When
            Optional<Discount> result = discountService.getBestDiscountForBatch(batchId, date);

            // Then
            assertTrue(result.isPresent());
            assertEquals(discount2, result.get()); // 15% off gives the best discount on $100 reference
            verify(discountManagementUseCase).getActiveDiscountsForBatch(batchId, date);
        }

        @Test
        @DisplayName("Should return empty when no discounts available")
        void shouldReturnEmptyWhenNoDiscountsAvailable() {
            // Given
            long batchId = 100L;
            LocalDate date = LocalDate.now();

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of());

            // When
            Optional<Discount> result = discountService.getBestDiscountForBatch(batchId, date);

            // Then
            assertFalse(result.isPresent());
            verify(discountManagementUseCase).getActiveDiscountsForBatch(batchId, date);
        }

        @Test
        @DisplayName("Should return single discount when only one available")
        void shouldReturnSingleDiscountWhenOnlyOneAvailable() {
            // Given
            long batchId = 100L;
            LocalDate date = LocalDate.now();

            Discount singleDiscount = new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                date, date.plusDays(30), true, "20% off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(singleDiscount));

            // When
            Optional<Discount> result = discountService.getBestDiscountForBatch(batchId, date);

            // Then
            assertTrue(result.isPresent());
            assertEquals(singleDiscount, result.get());
        }

        @Test
        @DisplayName("Should prefer higher fixed amount over lower percentage")
        void shouldPreferHigherFixedAmountOverLowerPercentage() {
            // Given
            long batchId = 100L;
            LocalDate date = LocalDate.now();

            Discount percentageDiscount = new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("5.00"),
                date, date.plusDays(30), true, "5% off", 1L);
            Discount fixedDiscount = new Discount(2L, batchId, DiscountType.FIXED_AMOUNT, new BigDecimal("20.00"),
                date, date.plusDays(30), true, "$20 off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(percentageDiscount, fixedDiscount));

            // When
            Optional<Discount> result = discountService.getBestDiscountForBatch(batchId, date);

            // Then
            assertTrue(result.isPresent());
            assertEquals(fixedDiscount, result.get()); // $20 off is better than 5% off $100 = $5
        }
    }

    @Nested
    @DisplayName("Apply Best Discount Tests")
    class ApplyBestDiscountTests {

        @Test
        @DisplayName("Should apply best discount to original price")
        void shouldApplyBestDiscountToOriginalPrice() {
            // Given
            long batchId = 100L;
            Money originalPrice = Money.of(new BigDecimal("50.00"));
            LocalDate date = LocalDate.now();

            Discount discount = new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                date, date.plusDays(30), true, "20% off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(discount));

            // When
            PricingResult result = discountService.applyBestDiscount(batchId, originalPrice, date);

            // Then
            assertNotNull(result);
            assertTrue(result.hasDiscount());
            assertEquals(Money.of(new BigDecimal("40.00")), result.getFinalPrice()); // 50 - 10 = 40
            assertEquals(Money.of(new BigDecimal("10.00")), result.getDiscountAmount()); // 20% of 50 = 10
            assertEquals(discount, result.getAppliedDiscount());
        }

        @Test
        @DisplayName("Should return original price when no discounts available")
        void shouldReturnOriginalPriceWhenNoDiscountsAvailable() {
            // Given
            long batchId = 100L;
            Money originalPrice = Money.of(new BigDecimal("75.00"));
            LocalDate date = LocalDate.now();

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of());

            // When
            PricingResult result = discountService.applyBestDiscount(batchId, originalPrice, date);

            // Then
            assertNotNull(result);
            assertFalse(result.hasDiscount());
            assertEquals(originalPrice, result.getFinalPrice());
            assertEquals(Money.of(0), result.getDiscountAmount());
            assertNull(result.getAppliedDiscount());
        }

        @Test
        @DisplayName("Should handle fixed amount discount")
        void shouldHandleFixedAmountDiscount() {
            // Given
            long batchId = 100L;
            Money originalPrice = Money.of(new BigDecimal("100.00"));
            LocalDate date = LocalDate.now();

            Discount fixedDiscount = new Discount(1L, batchId, DiscountType.FIXED_AMOUNT, new BigDecimal("15.00"),
                date, date.plusDays(30), true, "$15 off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(fixedDiscount));

            // When
            PricingResult result = discountService.applyBestDiscount(batchId, originalPrice, date);

            // Then
            assertEquals(Money.of(new BigDecimal("85.00")), result.getFinalPrice()); // 100 - 15 = 85
            assertEquals(Money.of(new BigDecimal("15.00")), result.getDiscountAmount());
        }

        @Test
        @DisplayName("Should handle discount that exceeds price")
        void shouldHandleDiscountThatExceedsPrice() {
            // Given
            long batchId = 100L;
            Money originalPrice = Money.of(new BigDecimal("10.00"));
            LocalDate date = LocalDate.now();

            Discount largeDiscount = new Discount(1L, batchId, DiscountType.FIXED_AMOUNT, new BigDecimal("20.00"),
                date, date.plusDays(30), true, "$20 off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(largeDiscount));

            // When
            PricingResult result = discountService.applyBestDiscount(batchId, originalPrice, date);

            // Then
            assertEquals(Money.of(0), result.getFinalPrice()); // Should not go below zero
            assertEquals(Money.of(new BigDecimal("20.00")), result.getDiscountAmount());
        }
    }

    @Nested
    @DisplayName("Get Active Discounts For Product Tests")
    class GetActiveDiscountsForProductTests {

        @Test
        @DisplayName("Should return active discounts for product")
        void shouldReturnActiveDiscountsForProduct() {
            // Given
            String productCode = "PROD001";
            LocalDate date = LocalDate.now();

            Discount activeDiscount1 = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                date.minusDays(5), date.plusDays(5), true, "10% off", 1L);
            Discount activeDiscount2 = new Discount(2L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("5.00"),
                date.minusDays(2), date.plusDays(10), true, "$5 off", 1L);
            Discount expiredDiscount = new Discount(3L, 102L, DiscountType.PERCENTAGE, new BigDecimal("15.00"),
                date.minusDays(20), date.minusDays(10), true, "Expired discount", 1L);

            when(discountManagementUseCase.getDiscountsByProductCode(productCode))
                .thenReturn(List.of(activeDiscount1, activeDiscount2, expiredDiscount));

            // When
            List<Discount> result = discountService.getActiveDiscountsForProduct(productCode, date);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.contains(activeDiscount1));
            assertTrue(result.contains(activeDiscount2));
            assertFalse(result.contains(expiredDiscount));
            verify(discountManagementUseCase).getDiscountsByProductCode(productCode);
        }

        @Test
        @DisplayName("Should return empty list when no active discounts")
        void shouldReturnEmptyListWhenNoActiveDiscounts() {
            // Given
            String productCode = "PROD002";
            LocalDate date = LocalDate.now();

            when(discountManagementUseCase.getDiscountsByProductCode(productCode))
                .thenReturn(List.of());

            // When
            List<Discount> result = discountService.getActiveDiscountsForProduct(productCode, date);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should filter out inactive discounts")
        void shouldFilterOutInactiveDiscounts() {
            // Given
            String productCode = "PROD003";
            LocalDate date = LocalDate.now();

            Discount inactiveDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                date.minusDays(5), date.plusDays(5), false, "Inactive discount", 1L);

            when(discountManagementUseCase.getDiscountsByProductCode(productCode))
                .thenReturn(List.of(inactiveDiscount));

            // When
            List<Discount> result = discountService.getActiveDiscountsForProduct(productCode, date);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("PricingResult Tests")
    class PricingResultTests {

        @Test
        @DisplayName("Should create PricingResult with discount")
        void shouldCreatePricingResultWithDiscount() {
            // Given
            Money finalPrice = Money.of(new BigDecimal("80.00"));
            Money discountAmount = Money.of(new BigDecimal("20.00"));
            Discount discount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "20% off", 1L);

            // When
            PricingResult result = new PricingResult(finalPrice, discountAmount, discount);

            // Then
            assertEquals(finalPrice, result.getFinalPrice());
            assertEquals(discountAmount, result.getDiscountAmount());
            assertEquals(discount, result.getAppliedDiscount());
            assertTrue(result.hasDiscount());
            assertEquals("20.0% off (20% off)", result.getDiscountDescription());
        }

        @Test
        @DisplayName("Should create PricingResult without discount")
        void shouldCreatePricingResultWithoutDiscount() {
            // Given
            Money finalPrice = Money.of(new BigDecimal("100.00"));
            Money discountAmount = Money.of(0);

            // When
            PricingResult result = new PricingResult(finalPrice, discountAmount, null);

            // Then
            assertEquals(finalPrice, result.getFinalPrice());
            assertEquals(discountAmount, result.getDiscountAmount());
            assertNull(result.getAppliedDiscount());
            assertFalse(result.hasDiscount());
            assertEquals("No discount", result.getDiscountDescription());
        }

        @Test
        @DisplayName("Should format fixed amount discount description")
        void shouldFormatFixedAmountDiscountDescription() {
            // Given
            Money finalPrice = Money.of(new BigDecimal("85.00"));
            Money discountAmount = Money.of(new BigDecimal("15.00"));
            Discount discount = new Discount(1L, 100L, DiscountType.FIXED_AMOUNT, new BigDecimal("15.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Special offer", 1L);

            // When
            PricingResult result = new PricingResult(finalPrice, discountAmount, discount);

            // Then
            assertEquals("$15.00 off (Special offer)", result.getDiscountDescription());
        }

        @Test
        @DisplayName("Should handle discount without description")
        void shouldHandleDiscountWithoutDescription() {
            // Given
            Money finalPrice = Money.of(new BigDecimal("90.00"));
            Money discountAmount = Money.of(new BigDecimal("10.00"));
            Discount discount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, null, 1L);

            // When
            PricingResult result = new PricingResult(finalPrice, discountAmount, discount);

            // Then
            assertEquals("10.0% off", result.getDiscountDescription());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create DiscountService with required dependencies")
        void shouldCreateDiscountServiceWithRequiredDependencies() {
            // When
            DiscountService service = new DiscountService(discountManagementUseCase);

            // Then
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle zero price discount calculation")
        void shouldHandleZeroPriceDiscountCalculation() {
            // Given
            long batchId = 100L;
            Money zeroPrice = Money.of(0);
            LocalDate date = LocalDate.now();

            Discount discount = new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("50.00"),
                date, date.plusDays(30), true, "50% off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(discount));

            // When
            PricingResult result = discountService.applyBestDiscount(batchId, zeroPrice, date);

            // Then
            assertEquals(Money.of(0), result.getFinalPrice());
            assertEquals(Money.of(0), result.getDiscountAmount());
        }

        @Test
        @DisplayName("Should handle very large discount amounts")
        void shouldHandleVeryLargeDiscountAmounts() {
            // Given
            long batchId = 100L;
            Money highPrice = Money.of(new BigDecimal("10000.00"));
            LocalDate date = LocalDate.now();

            Discount largeDiscount = new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("90.00"),
                date, date.plusDays(30), true, "90% off", 1L);

            when(discountManagementUseCase.getActiveDiscountsForBatch(batchId, date))
                .thenReturn(List.of(largeDiscount));

            // When
            PricingResult result = discountService.applyBestDiscount(batchId, highPrice, date);

            // Then
            assertEquals(Money.of(new BigDecimal("1000.00")), result.getFinalPrice()); // 10% of 10000
            assertEquals(Money.of(new BigDecimal("9000.00")), result.getDiscountAmount()); // 90% of 10000
        }
    }
}
