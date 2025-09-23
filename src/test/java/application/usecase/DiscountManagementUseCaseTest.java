package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.DiscountManagementUseCase;
import domain.pricing.Discount;
import domain.pricing.Discount.DiscountType;
import domain.repository.DiscountRepository;
import domain.user.User;
import domain.user.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@DisplayName("DiscountManagementUseCase Tests")
class DiscountManagementUseCaseTest {

    @Mock
    private DiscountRepository discountRepository;

    private DiscountManagementUseCase discountManagementUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        discountManagementUseCase = new DiscountManagementUseCase(discountRepository);
    }

    @Nested
    @DisplayName("Create Discount Tests")
    class CreateDiscountTests {

        @Test
        @DisplayName("Should create discount successfully when user is manager")
        void shouldCreateDiscountSuccessfullyWhenUserIsManager() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            DiscountManagementUseCase.CreateDiscountRequest request = createValidDiscountRequest();
            Discount savedDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), LocalDate.now(), LocalDate.now().plusDays(30),
                true, "10% off", 1L);

            when(discountRepository.save(any(Discount.class))).thenReturn(savedDiscount);

            // When
            DiscountManagementUseCase.CreateDiscountResult result =
                discountManagementUseCase.createDiscount(request, manager);

            // Then
            assertTrue(result.isSuccess());
            assertEquals(savedDiscount, result.getDiscount());
            verify(discountRepository).save(any(Discount.class));
        }

        @Test
        @DisplayName("Should fail to create discount when user is not manager")
        void shouldFailToCreateDiscountWhenUserIsNotManager() {
            // Given
            User cashier = new User(1L, "cashier", "hash", "cashier@store.com", Role.CASHIER);
            DiscountManagementUseCase.CreateDiscountRequest request = createValidDiscountRequest();

            // When
            DiscountManagementUseCase.CreateDiscountResult result =
                discountManagementUseCase.createDiscount(request, cashier);

            // Then
            assertFalse(result.isSuccess());
            assertEquals("Only managers can create discounts", result.getErrorMessage());
            verifyNoInteractions(discountRepository);
        }

        @Test
        @DisplayName("Should fail when discount value is invalid")
        void shouldFailWhenDiscountValueIsInvalid() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            DiscountManagementUseCase.CreateDiscountRequest request =
                new DiscountManagementUseCase.CreateDiscountRequest(
                    100L, DiscountType.PERCENTAGE, new BigDecimal("-5.00"), // Invalid negative value
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "Invalid discount"
                );

            // When
            DiscountManagementUseCase.CreateDiscountResult result =
                discountManagementUseCase.createDiscount(request, manager);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("Discount value must be positive"));
            verifyNoInteractions(discountRepository);
        }

        @Test
        @DisplayName("Should handle repository exception during create")
        void shouldHandleRepositoryExceptionDuringCreate() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            DiscountManagementUseCase.CreateDiscountRequest request = createValidDiscountRequest();

            when(discountRepository.save(any(Discount.class))).thenThrow(new RuntimeException("Database error"));

            // When
            DiscountManagementUseCase.CreateDiscountResult result =
                discountManagementUseCase.createDiscount(request, manager);

            // Then
            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("Failed to create discount"));
        }
    }

    @Nested
    @DisplayName("Update Discount Tests")
    class UpdateDiscountTests {

        @Test
        @DisplayName("Should update discount successfully when user is manager")
        void shouldUpdateDiscountSuccessfullyWhenUserIsManager() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            DiscountManagementUseCase.UpdateDiscountRequest request = createValidUpdateRequest();
            Discount existingDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), LocalDate.now(), LocalDate.now().plusDays(30),
                true, "Old description", 1L);
            Discount updatedDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE,
                new BigDecimal("15.00"), LocalDate.now(), LocalDate.now().plusDays(30),
                true, "Updated description", 1L);

            when(discountRepository.findById(1L)).thenReturn(Optional.of(existingDiscount));
            when(discountRepository.update(any(Discount.class))).thenReturn(updatedDiscount);

            // When
            DiscountManagementUseCase.UpdateDiscountResult result =
                discountManagementUseCase.updateDiscount(request, manager);

            // Then
            assertTrue(result.isSuccess());
            assertEquals(updatedDiscount, result.getDiscount());
            verify(discountRepository).findById(1L);
            verify(discountRepository).update(any(Discount.class));
        }

        @Test
        @DisplayName("Should fail to update discount when user is not manager")
        void shouldFailToUpdateDiscountWhenUserIsNotManager() {
            // Given
            User cashier = new User(1L, "cashier", "hash", "cashier@store.com", Role.CASHIER);
            DiscountManagementUseCase.UpdateDiscountRequest request = createValidUpdateRequest();

            // When
            DiscountManagementUseCase.UpdateDiscountResult result =
                discountManagementUseCase.updateDiscount(request, cashier);

            // Then
            assertFalse(result.isSuccess());
            assertEquals("Only managers can update discounts", result.getErrorMessage());
            verifyNoInteractions(discountRepository);
        }

        @Test
        @DisplayName("Should fail when discount not found")
        void shouldFailWhenDiscountNotFound() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            DiscountManagementUseCase.UpdateDiscountRequest request = createValidUpdateRequest();

            when(discountRepository.findById(1L)).thenReturn(Optional.empty());

            // When
            DiscountManagementUseCase.UpdateDiscountResult result =
                discountManagementUseCase.updateDiscount(request, manager);

            // Then
            assertFalse(result.isSuccess());
            assertEquals("Discount not found", result.getErrorMessage());
            verify(discountRepository).findById(1L);
            verify(discountRepository, never()).update(any(Discount.class));
        }
    }

    @Nested
    @DisplayName("Delete Discount Tests")
    class DeleteDiscountTests {

        @Test
        @DisplayName("Should delete discount successfully when user is manager")
        void shouldDeleteDiscountSuccessfullyWhenUserIsManager() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            long discountId = 1L;

            when(discountRepository.findById(discountId)).thenReturn(Optional.of(
                new Discount(discountId, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "10% off", 1L)
            ));

            // When
            DiscountManagementUseCase.DeleteDiscountResult result =
                discountManagementUseCase.deleteDiscount(discountId, manager);

            // Then
            assertTrue(result.isSuccess());
            verify(discountRepository).delete(discountId);
        }

        @Test
        @DisplayName("Should fail to delete discount when user is not manager")
        void shouldFailToDeleteDiscountWhenUserIsNotManager() {
            // Given
            User cashier = new User(1L, "cashier", "hash", "cashier@store.com", Role.CASHIER);
            long discountId = 1L;

            // When
            DiscountManagementUseCase.DeleteDiscountResult result =
                discountManagementUseCase.deleteDiscount(discountId, cashier);

            // Then
            assertFalse(result.isSuccess());
            assertEquals("Only managers can delete discounts", result.getErrorMessage());
            verifyNoInteractions(discountRepository);
        }
    }

    @Nested
    @DisplayName("Get Discounts Tests")
    class GetDiscountsTests {

        @Test
        @DisplayName("Should get all discounts")
        void shouldGetAllDiscounts() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            List<Discount> discounts = List.of(
                new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "10% off", 1L),
                new Discount(2L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("5.00"),
                    LocalDate.now(), LocalDate.now().plusDays(15), true, "$5 off", 1L)
            );

            when(discountRepository.findAll()).thenReturn(discounts);

            // When
            List<Discount> result = discountManagementUseCase.getAllDiscounts(manager);

            // Then
            assertEquals(2, result.size());
            assertEquals(discounts, result);
            verify(discountRepository).findAll();
        }

        @Test
        @DisplayName("Should get active discounts")
        void shouldGetActiveDiscounts() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            List<Discount> activeDiscounts = List.of(
                new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "10% off", 1L)
            );

            when(discountRepository.findAll()).thenReturn(activeDiscounts);

            // When
            List<Discount> result = discountManagementUseCase.getAllDiscounts(manager);

            // Then
            assertEquals(1, result.size());
            assertEquals(activeDiscounts, result);
            verify(discountRepository).findAll();
        }

        @Test
        @DisplayName("Should get discounts by batch ID")
        void shouldGetDiscountsByBatchId() {
            // Given
            User manager = new User(1L, "manager", "hash", "manager@store.com", Role.MANAGER);
            Long batchId = 100L;
            List<Discount> batchDiscounts = List.of(
                new Discount(1L, batchId, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "10% off", 1L)
            );

            when(discountRepository.findByBatchId(batchId)).thenReturn(batchDiscounts);

            // When
            List<Discount> result = discountManagementUseCase.getDiscountsForBatch(batchId, manager);

            // Then
            assertEquals(1, result.size());
            assertEquals(batchDiscounts, result);
            verify(discountRepository).findByBatchId(batchId);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate discount request correctly")
        void shouldValidateDiscountRequestCorrectly() {
            // Given
            DiscountManagementUseCase.CreateDiscountRequest validRequest = createValidDiscountRequest();
            DiscountManagementUseCase.CreateDiscountRequest invalidRequest =
                new DiscountManagementUseCase.CreateDiscountRequest(
                    100L, DiscountType.PERCENTAGE, new BigDecimal("150.00"), // Invalid: > 100%
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "Invalid discount"
                );

            // When
            DiscountManagementUseCase.ValidationResult validResult =
                discountManagementUseCase.validateDiscountRequest(validRequest);
            DiscountManagementUseCase.ValidationResult invalidResult =
                discountManagementUseCase.validateDiscountRequest(invalidRequest);

            // Then
            assertTrue(validResult.isValid());
            assertFalse(invalidResult.isValid());
            assertTrue(invalidResult.getErrorMessage().contains("percentage cannot exceed 100"));
        }

        @Test
        @DisplayName("Should validate end date is after start date")
        void shouldValidateEndDateIsAfterStartDate() {
            // Given
            DiscountManagementUseCase.CreateDiscountRequest request =
                new DiscountManagementUseCase.CreateDiscountRequest(
                    100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now().plusDays(30), LocalDate.now(), // End date before start date
                    true, "Invalid date range"
                );

            // When
            DiscountManagementUseCase.ValidationResult result =
                discountManagementUseCase.validateDiscountRequest(request);

            // Then
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("End date must be after start date"));
        }
    }

    // Helper methods
    private DiscountManagementUseCase.CreateDiscountRequest createValidDiscountRequest() {
        return new DiscountManagementUseCase.CreateDiscountRequest(
            100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
            LocalDate.now(), LocalDate.now().plusDays(30), true, "10% off promotion"
        );
    }

    private DiscountManagementUseCase.UpdateDiscountRequest createValidUpdateRequest() {
        return new DiscountManagementUseCase.UpdateDiscountRequest(
            1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("15.00"),
            LocalDate.now(), LocalDate.now().plusDays(30), true, "Updated 15% off promotion"
        );
    }
}
