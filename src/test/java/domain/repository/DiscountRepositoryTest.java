package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.DiscountRepository;
import domain.pricing.Discount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@DisplayName("DiscountRepository Domain Interface Tests")
class DiscountRepositoryTest {

    private DiscountRepository discountRepository;
    private Discount discount;
    private long discountId;
    private long batchId;

    @BeforeEach
    void setUp() {
        discountRepository = mock(DiscountRepository.class);
        discount = mock(Discount.class);
        discountId = 1L;
        batchId = 100L;
    }

    @Test
    @DisplayName("Should define contract for saving discounts")
    void shouldDefineContractForSavingDiscounts() {
        // Given
        when(discountRepository.save(discount)).thenReturn(discount);

        // When
        Discount savedDiscount = discountRepository.save(discount);

        // Then
        assertEquals(discount, savedDiscount);
        verify(discountRepository).save(discount);
    }

    @Test
    @DisplayName("Should define contract for updating discounts")
    void shouldDefineContractForUpdatingDiscounts() {
        // Given
        when(discountRepository.update(discount)).thenReturn(discount);

        // When
        Discount updated = discountRepository.update(discount);

        // Then
        assertEquals(discount, updated);
        verify(discountRepository).update(discount);
    }

    @Test
    @DisplayName("Should define contract for finding discount by id")
    void shouldDefineContractForFindingDiscountById() {
        // Given
        when(discountRepository.findById(discountId)).thenReturn(Optional.of(discount));

        // When
        Optional<Discount> foundDiscount = discountRepository.findById(discountId);

        // Then
        assertTrue(foundDiscount.isPresent());
        assertEquals(discount, foundDiscount.get());
        verify(discountRepository).findById(discountId);
    }

    @Test
    @DisplayName("Should define contract for finding discounts by batch")
    void shouldDefineContractForFindingDiscountsByBatch() {
        // Given
        List<Discount> expectedDiscounts = List.of(discount);
        when(discountRepository.findByBatchId(batchId)).thenReturn(expectedDiscounts);

        // When
        List<Discount> discounts = discountRepository.findByBatchId(batchId);

        // Then
        assertEquals(expectedDiscounts, discounts);
        verify(discountRepository).findByBatchId(batchId);
    }

    @Test
    @DisplayName("Should define contract for finding active discounts for batch on date")
    void shouldDefineContractForFindingActiveDiscountsForBatchOnDate() {
        // Given
        LocalDate date = LocalDate.now();
        List<Discount> expectedDiscounts = List.of(discount);
        when(discountRepository.findActiveDiscountsForBatch(batchId, date)).thenReturn(expectedDiscounts);

        // When
        List<Discount> discounts = discountRepository.findActiveDiscountsForBatch(batchId, date);

        // Then
        assertEquals(expectedDiscounts, discounts);
        verify(discountRepository).findActiveDiscountsForBatch(batchId, date);
    }

    @Test
    @DisplayName("Should define contract for finding active discounts in date range")
    void shouldDefineContractForFindingActiveDiscountsInDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Discount> expectedDiscounts = List.of(discount);
        when(discountRepository.findActiveDiscountsInDateRange(startDate, endDate)).thenReturn(expectedDiscounts);

        // When
        List<Discount> discounts = discountRepository.findActiveDiscountsInDateRange(startDate, endDate);

        // Then
        assertEquals(expectedDiscounts, discounts);
        verify(discountRepository).findActiveDiscountsInDateRange(startDate, endDate);
    }

    @Test
    @DisplayName("Should define contract for finding all discounts")
    void shouldDefineContractForFindingAllDiscounts() {
        // Given
        List<Discount> expected = List.of(discount);
        when(discountRepository.findAll()).thenReturn(expected);

        // When
        List<Discount> result = discountRepository.findAll();

        // Then
        assertEquals(expected, result);
        verify(discountRepository).findAll();
    }

    @Test
    @DisplayName("Should define contract for finding discounts by product code")
    void shouldDefineContractForFindingDiscountsByProductCode() {
        // Given
        String productCode = "PROD001";
        List<Discount> expected = List.of(discount);
        when(discountRepository.findByProductCode(productCode)).thenReturn(expected);

        // When
        List<Discount> result = discountRepository.findByProductCode(productCode);

        // Then
        assertEquals(expected, result);
        verify(discountRepository).findByProductCode(productCode);
    }

    @Test
    @DisplayName("Should define contract for deleting discount")
    void shouldDefineContractForDeletingDiscount() {
        // Given
        when(discountRepository.delete(discountId)).thenReturn(true);

        // When
        boolean deleted = discountRepository.delete(discountId);

        // Then
        assertTrue(deleted);
        verify(discountRepository).delete(discountId);
    }
}
