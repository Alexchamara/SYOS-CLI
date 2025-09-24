package cli.manager.discount;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.DiscountManagementUseCase;
import domain.pricing.Discount;
import domain.pricing.Discount.DiscountType;
import domain.user.User;
import domain.user.Role;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@DisplayName("DiscountManagementCLI Tests")
class DiscountManagementCLITest {

    @Mock
    private DiscountManagementUseCase discountManagementUseCase;

    @Mock
    private User currentUser;

    private DiscountManagementCLI discountManagementCLI;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(currentUser.role()).thenReturn(Role.MANAGER);
        when(currentUser.username()).thenReturn("manager");
        discountManagementCLI = new DiscountManagementCLI(currentUser, discountManagementUseCase);

        // Capture System.out and System.in for testing
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setIn(originalIn);
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Menu Display Tests")
    class MenuDisplayTests {

        @Test
        @DisplayName("Should display discount management menu")
        void shouldDisplayDiscountManagementMenu() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("DISCOUNT MANAGEMENT"));
            assertTrue(output.contains("1) Create New Discount"));
            assertTrue(output.contains("2) Update Discount"));
            assertTrue(output.contains("3) Delete Discount"));
            assertTrue(output.contains("4) View All Discounts"));
            assertTrue(output.contains("5) View Discounts by Batch"));
            assertTrue(output.contains("6) View Active Discounts"));
            assertTrue(output.contains("0) Back to Manager Menu"));
        }

        @Test
        @DisplayName("Should handle invalid menu option")
        void shouldHandleInvalidMenuOption() {
            // Given
            String input = "99\n0\n"; // Invalid option then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid option. Please try again."));
        }
    }

    @Nested
    @DisplayName("Create Discount Tests")
    class CreateDiscountTests {

        @Test
        @DisplayName("Should create percentage discount successfully")
        void shouldCreatePercentageDiscountSuccessfully() {
            // Given
            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(
                    new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("15.00"),
                        LocalDate.now(), LocalDate.now().plusDays(30), true, "15% off", 1L)
                );
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            String input = "1\n100\n1\n15.00\n2025-12-31\n2026-01-31\ntrue\n15% off promotion\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).createDiscount(argThat(request ->
                request.getBatchId() == 100L &&
                request.getDiscountType() == DiscountType.PERCENTAGE &&
                request.getDiscountValue().equals(new BigDecimal("15.00")) &&
                request.isActive() &&
                "15% off promotion".equals(request.getDescription())
            ), eq(currentUser));

            String output = outputStream.toString();
            assertTrue(output.contains("Discount created successfully") || output.contains("Discount created successfully!"));
        }

        @Test
        @DisplayName("Should create fixed amount discount successfully")
        void shouldCreateFixedAmountDiscountSuccessfully() {
            // Given
            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(
                    new Discount(2L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("25.00"),
                        LocalDate.now(), LocalDate.now().plusDays(15), true, "$25 off", 1L)
                );
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            String input = "1\n101\n2\n25.00\n2025-12-31\n2026-01-15\ntrue\n$25 off\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).createDiscount(argThat(request ->
                request.getBatchId() == 101L &&
                request.getDiscountType() == DiscountType.FIXED_AMOUNT &&
                request.getDiscountValue().equals(new BigDecimal("25.00"))
            ), eq(currentUser));
        }

        @Test
        @DisplayName("Should handle invalid discount type")
        void shouldHandleInvalidDiscountType() {
            // Given
            String input = "1\n100\n99\n1\n10.00\n2025-12-31\n2026-01-31\ntrue\nDiscount\n0\n"; // Invalid then valid type
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter 1 or 2."));
        }

        @Test
        @DisplayName("Should handle creation failure")
        void shouldHandleCreationFailure() {
            // Given
            DiscountManagementUseCase.CreateDiscountResult failureResult =
                DiscountManagementUseCase.CreateDiscountResult.failure("Validation error");
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(failureResult);

            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\ntrue\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Failed to create discount") || output.contains("Validation error"));
        }

        @Test
        @DisplayName("Should handle invalid batch ID input")
        void shouldHandleInvalidBatchIdInput() {
            // Given
            String input = "1\ninvalid\n100\n1\n10.00\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter a valid number."));
        }

        @Test
        @DisplayName("Should handle invalid discount value input")
        void shouldHandleInvalidDiscountValueInput() {
            // Given
            String input = "1\n100\n1\ninvalid\n10.00\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter a valid number."));
        }

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String input = "1\n100\n1\n10.00\ninvalid-date\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter date in format yyyy-MM-dd"));
        }

        @Test
        @DisplayName("Should handle percentage over 100")
        void shouldHandlePercentageOver100() {
            // Given
            String input = "1\n100\n1\n150\n10.00\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Percentage cannot exceed 100%"));
        }

        @Test
        @DisplayName("Should handle negative discount value")
        void shouldHandleNegativeDiscountValue() {
            // Given
            String input = "1\n100\n1\n-10\n10.00\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Value must be positive"));
        }

        @Test
        @DisplayName("Should handle empty description")
        void shouldHandleEmptyDescription() {
            // Given
            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\ny\n\n0\n"; // Empty description
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).createDiscount(argThat(request ->
                request.getDescription() == null
            ), eq(currentUser));
        }

        @Test
        @DisplayName("Should handle boolean input variations")
        void shouldHandleBooleanInputVariations() {
            // Given - Test different boolean inputs
            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\nyes\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).createDiscount(argThat(request ->
                request.isActive()
            ), eq(currentUser));
        }
    }

    @Nested
    @DisplayName("View Discounts Tests")
    class ViewDiscountsTests {

        @Test
        @DisplayName("Should view all discounts")
        void shouldViewAllDiscounts() {
            // Given
            List<Discount> discounts = List.of(
                new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "10% off", 1L),
                new Discount(2L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("5.00"),
                    LocalDate.now(), LocalDate.now().plusDays(15), false, "$5 off", 1L)
            );

            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(discounts);

            String input = "4\n0\n"; // View all discounts (option 4), then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).getAllDiscounts(currentUser);
            String output = outputStream.toString();
            assertTrue(output.contains("ALL DISCOUNTS"));
            assertTrue(output.contains("1") && output.contains("100")); // ID and Batch ID
            assertTrue(output.contains("2") && output.contains("101"));
            assertTrue(output.contains("PERCENTAGE"));
            assertTrue(output.contains("FIXED_AMOUNT"));
        }

        @Test
        @DisplayName("Should handle empty discount list")
        void shouldHandleEmptyDiscountList() {
            // Given
            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of());

            String input = "4\n0\n"; // View all discounts (option 4), then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No discounts found."));
        }

        @Test
        @DisplayName("Should view discounts by batch")
        void shouldViewDiscountsByBatch() {
            // Given
            List<Discount> batchDiscounts = List.of(
                new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                    LocalDate.now(), LocalDate.now().plusDays(30), true, "Batch discount", 1L)
            );

            when(discountManagementUseCase.getDiscountsForBatch(100L, currentUser)).thenReturn(batchDiscounts);

            String input = "5\n100\n0\n"; // View by batch (option 5), batch ID 100, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).getDiscountsForBatch(100L, currentUser);
            String output = outputStream.toString();
            assertTrue(output.contains("DISCOUNTS BY BATCH"));
            assertTrue(output.contains("100"));
        }

        @Test
        @DisplayName("Should view active discounts")
        void shouldViewActiveDiscounts() {
            // Given
            List<Discount> allDiscounts = List.of(
                new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("15.00"),
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), true, "Active discount", 1L),
                new Discount(2L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("5.00"),
                    LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), true, "Expired discount", 1L)
            );

            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(allDiscounts);

            String input = "6\n0\n"; // View active discounts (option 6), then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).getAllDiscounts(currentUser);
            String output = outputStream.toString();
            assertTrue(output.contains("ACTIVE DISCOUNTS"));
        }

        @Test
        @DisplayName("Should handle empty batch discount list")
        void shouldHandleEmptyBatchDiscountList() {
            // Given
            when(discountManagementUseCase.getDiscountsForBatch(999L, currentUser)).thenReturn(List.of());

            String input = "5\n999\n0\n"; // View by batch, non-existent batch, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No discounts found for batch 999"));
        }
    }

    @Nested
    @DisplayName("Delete Discount Tests")
    class DeleteDiscountTests {

        @Test
        @DisplayName("Should delete discount successfully")
        void shouldDeleteDiscountSuccessfully() {
            // Given
            DiscountManagementUseCase.DeleteDiscountResult successResult =
                DiscountManagementUseCase.DeleteDiscountResult.success();
            when(discountManagementUseCase.deleteDiscount(1L, currentUser)).thenReturn(successResult);

            String input = "3\n1\ny\n0\n"; // Delete (option 3), ID, confirm, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).deleteDiscount(1L, currentUser);
            String output = outputStream.toString();
            assertTrue(output.contains("Discount deleted successfully!"));
        }

        @Test
        @DisplayName("Should handle delete confirmation cancellation")
        void shouldHandleDeleteConfirmationCancellation() {
            // Given
            String input = "3\n1\nn\n0\n"; // Delete (option 3), ID, cancel, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase, never()).deleteDiscount(anyLong(), any(User.class));
            String output = outputStream.toString();
            assertTrue(output.contains("Deletion cancelled."));
        }

        @Test
        @DisplayName("Should handle delete failure")
        void shouldHandleDeleteFailure() {
            // Given
            DiscountManagementUseCase.DeleteDiscountResult failureResult =
                DiscountManagementUseCase.DeleteDiscountResult.failure("Discount not found");
            when(discountManagementUseCase.deleteDiscount(999L, currentUser)).thenReturn(failureResult);

            String input = "3\n999\ny\n0\n"; // Delete (option 3) non-existent, confirm, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Failed to delete discount: Discount not found"));
        }

        @Test
        @DisplayName("Should handle invalid discount ID input")
        void shouldHandleInvalidDiscountIdInput() {
            // Given
            String input = "3\ninvalid\n1\ny\n0\n"; // Delete, invalid ID, valid ID, confirm, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.DeleteDiscountResult successResult =
                DiscountManagementUseCase.DeleteDiscountResult.success();
            when(discountManagementUseCase.deleteDiscount(1L, currentUser)).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter a valid number."));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should work with manager user")
        void shouldWorkWithManagerUser() {
            // Given
            when(currentUser.role()).thenReturn(Role.MANAGER);
            String input = "0\n"; // Exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When & Then
            assertDoesNotThrow(() -> discountManagementCLI.run());
        }

        @Test
        @DisplayName("Should handle non-manager user gracefully")
        void shouldHandleNonManagerUserGracefully() {
            // Given
            User cashierUser = mock(User.class);
            when(cashierUser.role()).thenReturn(Role.CASHIER);
            DiscountManagementCLI cashierCLI = new DiscountManagementCLI(cashierUser, discountManagementUseCase);

            DiscountManagementUseCase.CreateDiscountResult failureResult =
                DiscountManagementUseCase.CreateDiscountResult.failure("Only managers can create discounts");
            when(discountManagementUseCase.createDiscount(any(), eq(cashierUser))).thenReturn(failureResult);

            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\ntrue\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            cashierCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Only managers can create discounts") || output.contains("Failed to create discount"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create DiscountManagementCLI with required dependencies")
        void shouldCreateDiscountManagementCLIWithRequiredDependencies() {
            // When
            DiscountManagementCLI cli = new DiscountManagementCLI(currentUser, discountManagementUseCase);

            // Then
            assertNotNull(cli);
        }
    }

    @Nested
    @DisplayName("Update Discount Tests")
    class UpdateDiscountTests {

        @Test
        @DisplayName("Should update discount successfully")
        void shouldUpdateDiscountSuccessfully() {
            // Given
            Discount existingDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Old description", 1L);

            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of(existingDiscount));

            DiscountManagementUseCase.UpdateDiscountResult successResult =
                DiscountManagementUseCase.UpdateDiscountResult.success(
                    new Discount(1L, 101L, DiscountType.FIXED_AMOUNT, new BigDecimal("25.00"),
                        LocalDate.now(), LocalDate.now().plusDays(15), false, "Updated description", 1L)
                );
            when(discountManagementUseCase.updateDiscount(any(), eq(currentUser))).thenReturn(successResult);

            String input = "2\n1\n101\n2\n25.00\n2025-12-31\n2026-01-15\nn\nUpdated description\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).updateDiscount(argThat(request ->
                request.getDiscountId() == 1L &&
                request.getBatchId() == 101L &&
                request.getDiscountType() == DiscountType.FIXED_AMOUNT &&
                request.getDiscountValue().equals(new BigDecimal("25.00")) &&
                !request.isActive() &&
                "Updated description".equals(request.getDescription())
            ), eq(currentUser));

            String output = outputStream.toString();
            assertTrue(output.contains("Discount updated successfully!"));
        }

        @Test
        @DisplayName("Should handle update with default values")
        void shouldHandleUpdateWithDefaultValues() {
            // Given
            Discount existingDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Original description", 1L);

            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of(existingDiscount));

            DiscountManagementUseCase.UpdateDiscountResult successResult =
                DiscountManagementUseCase.UpdateDiscountResult.success(existingDiscount);
            when(discountManagementUseCase.updateDiscount(any(), eq(currentUser))).thenReturn(successResult);

            String input = "2\n1\n\n\n\n\n\n\n\n0\n"; // All empty inputs (use defaults)
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).updateDiscount(argThat(request ->
                request.getDiscountId() == 1L &&
                request.getBatchId() == 100L &&
                request.getDiscountType() == DiscountType.PERCENTAGE &&
                request.getDiscountValue().equals(new BigDecimal("10.00")) &&
                request.isActive() &&
                "Original description".equals(request.getDescription())
            ), eq(currentUser));
        }

        @Test
        @DisplayName("Should handle discount not found for update")
        void shouldHandleDiscountNotFoundForUpdate() {
            // Given
            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of());

            String input = "2\n999\n0\n"; // Try to update non-existent discount
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Discount not found!"));
            verify(discountManagementUseCase, never()).updateDiscount(any(), eq(currentUser));
        }

        @Test
        @DisplayName("Should handle update failure")
        void shouldHandleUpdateFailure() {
            // Given
            Discount existingDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Description", 1L);

            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of(existingDiscount));

            DiscountManagementUseCase.UpdateDiscountResult failureResult =
                DiscountManagementUseCase.UpdateDiscountResult.failure("Update validation failed");
            when(discountManagementUseCase.updateDiscount(any(), eq(currentUser))).thenReturn(failureResult);

            String input = "2\n1\n\n\n\n\n\n\n\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Failed to update discount: Update validation failed"));
        }

        @Test
        @DisplayName("Should handle invalid discount ID for update")
        void shouldHandleInvalidDiscountIdForUpdate() {
            // Given
            String input = "2\ninvalid\n1\n\n\n\n\n\n\n\n0\n"; // Invalid then valid ID
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            Discount existingDiscount = new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), true, "Description", 1L);
            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of(existingDiscount));

            DiscountManagementUseCase.UpdateDiscountResult successResult =
                DiscountManagementUseCase.UpdateDiscountResult.success(existingDiscount);
            when(discountManagementUseCase.updateDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter a valid number."));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid boolean input")
        void shouldHandleInvalidBooleanInput() {
            // Given
            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\ninvalid\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter y/n or yes/no."));
        }

        @Test
        @DisplayName("Should handle invalid batch ID for view by batch")
        void shouldHandleInvalidBatchIdForViewByBatch() {
            // Given
            String input = "5\ninvalid\n100\n0\n"; // Invalid then valid batch ID
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(discountManagementUseCase.getDiscountsForBatch(100L, currentUser)).thenReturn(List.of());

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Please enter a valid number."));
        }

        @Test
        @DisplayName("Should handle zero discount value")
        void shouldHandleZeroDiscountValue() {
            // Given
            String input = "1\n100\n1\n0\n10.00\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Value must be positive"));
        }

        @Test
        @DisplayName("Should handle large discount values")
        void shouldHandleLargeDiscountValues() {
            // Given
            String input = "1\n100\n2\n99999.99\n2025-12-31\n2026-01-31\ny\nLarge discount\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).createDiscount(argThat(request ->
                request.getDiscountValue().equals(new BigDecimal("99999.99"))
            ), eq(currentUser));
        }

        @Test
        @DisplayName("Should handle different boolean input formats")
        void shouldHandleDifferentBooleanInputFormats() {
            // Given - Test 'false' input
            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\nfalse\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            DiscountManagementUseCase.CreateDiscountResult successResult =
                DiscountManagementUseCase.CreateDiscountResult.success(mock(Discount.class));
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser))).thenReturn(successResult);

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).createDiscount(argThat(request ->
                !request.isActive()
            ), eq(currentUser));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exception during create discount")
        void shouldHandleExceptionDuringCreateDiscount() {
            // Given
            when(discountManagementUseCase.createDiscount(any(), eq(currentUser)))
                .thenThrow(new RuntimeException("Database connection failed"));

            String input = "1\n100\n1\n10.00\n2025-12-31\n2026-01-31\ny\nTest\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error: Database connection failed"));
        }

        @Test
        @DisplayName("Should handle exception during view all discounts")
        void shouldHandleExceptionDuringViewAllDiscounts() {
            // Given
            when(discountManagementUseCase.getAllDiscounts(currentUser))
                .thenThrow(new RuntimeException("Service unavailable"));

            String input = "4\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error: Service unavailable"));
        }

        @Test
        @DisplayName("Should handle exception during view by batch")
        void shouldHandleExceptionDuringViewByBatch() {
            // Given
            when(discountManagementUseCase.getDiscountsForBatch(100L, currentUser))
                .thenThrow(new RuntimeException("Batch service error"));

            String input = "5\n100\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error: Batch service error"));
        }

        @Test
        @DisplayName("Should handle exception during delete discount")
        void shouldHandleExceptionDuringDeleteDiscount() {
            // Given
            when(discountManagementUseCase.deleteDiscount(1L, currentUser))
                .thenThrow(new RuntimeException("Delete operation failed"));

            String input = "3\n1\ny\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error: Delete operation failed"));
        }

        @Test
        @DisplayName("Should handle multiple menu iterations")
        void shouldHandleMultipleMenuIterations() {
            // Given
            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of());

            String input = "4\n6\n99\n0\n"; // View all, view active, invalid option, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("ALL DISCOUNTS"));
            assertTrue(output.contains("ACTIVE DISCOUNTS"));
            assertTrue(output.contains("Invalid option. Please try again."));
            verify(discountManagementUseCase, times(2)).getAllDiscounts(currentUser);
        }

        @Test
        @DisplayName("Should handle view active discounts with no active ones")
        void shouldHandleViewActiveDiscountsWithNoActiveOnes() {
            // Given
            List<Discount> expiredDiscounts = List.of(
                new Discount(1L, 100L, DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                    LocalDate.now().minusDays(60), LocalDate.now().minusDays(30), true, "Expired", 1L)
            );

            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(expiredDiscounts);

            String input = "6\n0\n"; // View active discounts, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("ACTIVE DISCOUNTS"));
            assertTrue(output.contains("No active discounts found."));
        }
    }
}
