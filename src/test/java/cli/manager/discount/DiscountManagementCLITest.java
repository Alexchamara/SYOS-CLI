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

import cli.manager.discount.DiscountManagementCLI;
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
            assertTrue(output.contains("1. Create New Discount") || output.contains("1) Create New Discount"));
            assertTrue(output.contains("2. View All Discounts") || output.contains("4) View All Discounts"));
            assertTrue(output.contains("3. Update Discount") || output.contains("2) Update Discount"));
            assertTrue(output.contains("4. Delete Discount") || output.contains("3) Delete Discount"));
            assertTrue(output.contains("5. View Active Discounts") || output.contains("6) View Active Discounts"));
            assertTrue(output.contains("0. Back to Manager Menu") || output.contains("0) Back to Manager Menu"));
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
            assertTrue(output.contains("Invalid discount type"));
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

            String input = "2\n0\n"; // View all discounts, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).getAllDiscounts(currentUser);
            String output = outputStream.toString();
            assertTrue(output.contains("All Discounts:") || output.contains("ALL DISCOUNTS"));
            assertTrue(output.contains("ID: 1") || output.contains("ID"));
            assertTrue(output.contains("ID: 2") || output.contains("ID"));
            assertTrue(output.contains("PERCENTAGE"));
            assertTrue(output.contains("FIXED_AMOUNT"));
            assertTrue(output.contains("10% off") || output.contains("10%"));
            assertTrue(output.contains("$5 off") || output.contains("$5"));
        }

        @Test
        @DisplayName("Should handle empty discount list")
        void shouldHandleEmptyDiscountList() {
            // Given
            when(discountManagementUseCase.getAllDiscounts(currentUser)).thenReturn(List.of());

            String input = "2\n0\n"; // View all discounts, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No discounts found") || output.contains("No discounts found."));
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

            String input = "4\n1\ny\n0\n"; // Delete, ID, confirm, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase).deleteDiscount(1L, currentUser);
            String output = outputStream.toString();
            assertTrue(output.contains("Discount deleted successfully") || output.contains("Discount deleted successfully!"));
        }

        @Test
        @DisplayName("Should handle delete confirmation cancellation")
        void shouldHandleDeleteConfirmationCancellation() {
            // Given
            String input = "4\n1\nn\n0\n"; // Delete, ID, cancel, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            verify(discountManagementUseCase, never()).deleteDiscount(anyLong(), any(User.class));
            String output = outputStream.toString();
            assertTrue(output.contains("Delete cancelled") || output.contains("Deletion cancelled."));
        }

        @Test
        @DisplayName("Should handle delete failure")
        void shouldHandleDeleteFailure() {
            // Given
            DiscountManagementUseCase.DeleteDiscountResult failureResult =
                DiscountManagementUseCase.DeleteDiscountResult.failure("Discount not found");
            when(discountManagementUseCase.deleteDiscount(999L, currentUser)).thenReturn(failureResult);

            String input = "4\n999\ny\n0\n"; // Delete non-existent
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            discountManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Failed to delete discount") || output.contains("Discount not found"));
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
}
