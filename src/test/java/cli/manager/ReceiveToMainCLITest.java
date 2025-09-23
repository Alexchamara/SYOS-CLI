package cli.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cli.manager.ReceiveToMainCLI;
import application.usecase.ReceiveFromSupplierUseCase;
import application.usecase.BatchManagementUseCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;

@DisplayName("ReceiveToMainCLI Tests")
class ReceiveToMainCLITest {

    @Mock
    private ReceiveFromSupplierUseCase receiveFromSupplierUseCase;

    private ReceiveToMainCLI receiveToMainCLI;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        receiveToMainCLI = new ReceiveToMainCLI(receiveFromSupplierUseCase);

        // Capture System.out and System.in for testing
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Nested
    @DisplayName("Menu Display Tests")
    class MenuDisplayTests {

        @Test
        @DisplayName("Should display receive to main menu")
        void shouldDisplayReceiveToMainMenu() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("RECEIVE TO MAIN STORE"));
            assertTrue(output.contains("1. Receive New Batch"));
            assertTrue(output.contains("2. View Recent Receipts"));
            assertTrue(output.contains("0. Back to Manager Menu"));
        }
    }

    @Nested
    @DisplayName("Receive Batch Tests")
    class ReceiveBatchTests {

        @Test
        @DisplayName("Should receive batch successfully")
        void shouldReceiveBatchSuccessfully() {
            // Given
            when(receiveFromSupplierUseCase.receive("PROD001", 100, LocalDate.of(2025, 12, 31)))
                .thenReturn(123L);

            String input = "1\nPROD001\n100\n2025-12-31\n0\n"; // Receive batch: code, qty, expiry, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            receiveToMainCLI.run();

            // Then
            verify(receiveFromSupplierUseCase).receive("PROD001", 100, LocalDate.of(2025, 12, 31));
            String output = outputStream.toString();
            assertTrue(output.contains("Batch received successfully"));
            assertTrue(output.contains("Batch ID: 123"));
        }

        @Test
        @DisplayName("Should handle empty product code")
        void shouldHandleEmptyProductCode() {
            // Given
            String input = "1\n\nPROD001\n50\n2025-12-31\n0\n"; // Empty then valid code
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(receiveFromSupplierUseCase.receive("PROD001", 50, LocalDate.of(2025, 12, 31)))
                .thenReturn(124L);

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product code cannot be empty"));
        }

        @Test
        @DisplayName("Should handle invalid quantity")
        void shouldHandleInvalidQuantity() {
            // Given
            String input = "1\nPROD001\nabc\n75\n2025-12-31\n0\n"; // Invalid then valid quantity
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(receiveFromSupplierUseCase.receive("PROD001", 75, LocalDate.of(2025, 12, 31)))
                .thenReturn(125L);

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid quantity"));
        }

        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            // Given
            String input = "1\nPROD001\n0\n25\n2025-12-31\n0\n"; // Zero then valid quantity
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(receiveFromSupplierUseCase.receive("PROD001", 25, LocalDate.of(2025, 12, 31)))
                .thenReturn(126L);

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Quantity must be positive"));
        }

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String input = "1\nPROD001\n50\ninvalid-date\n2025-12-31\n0\n"; // Invalid then valid date
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(receiveFromSupplierUseCase.receive("PROD001", 50, LocalDate.of(2025, 12, 31)))
                .thenReturn(127L);

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle use case exceptions")
        void shouldHandleUseCaseExceptions() {
            // Given
            when(receiveFromSupplierUseCase.receive("PROD001", 50, LocalDate.of(2025, 12, 31)))
                .thenThrow(new RuntimeException("Database error"));

            String input = "1\nPROD001\n50\n2025-12-31\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error receiving batch") || output.contains("Database error"));
        }
    }

    @Nested
    @DisplayName("View Recent Receipts Tests")
    class ViewRecentReceiptsTests {

        @Test
        @DisplayName("Should view recent receipts")
        void shouldViewRecentReceipts() {
            // Given
            List<BatchManagementUseCase.BatchInfo> recentBatches = List.of(
                new BatchManagementUseCase.BatchInfo(
                    new domain.inventory.Batch(1L,
                        new domain.shared.Code("PROD001"),
                        domain.inventory.StockLocation.MAIN_STORE,
                        java.time.LocalDateTime.now().minusHours(1),
                        LocalDate.now().plusDays(30),
                        new domain.shared.Quantity(100))
                )
            );

            when(receiveFromSupplierUseCase.getRecentBatches(10)).thenReturn(recentBatches);

            String input = "2\n0\n"; // View recent receipts, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            receiveToMainCLI.run();

            // Then
            verify(receiveFromSupplierUseCase).getRecentBatches(10);
            String output = outputStream.toString();
            assertTrue(output.contains("Recent Receipts:"));
            assertTrue(output.contains("PROD001"));
        }

        @Test
        @DisplayName("Should handle empty recent receipts")
        void shouldHandleEmptyRecentReceipts() {
            // Given
            when(receiveFromSupplierUseCase.getRecentBatches(10)).thenReturn(List.of());

            String input = "2\n0\n"; // View recent receipts, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No recent receipts found"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid menu choices")
        void shouldHandleInvalidMenuChoices() {
            // Given
            String input = "99\nabc\n-1\n0\n"; // Invalid choices, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            receiveToMainCLI.run();

            // Then
            String output = outputStream.toString();
            long invalidMessages = output.lines()
                .filter(line -> line.contains("Invalid option"))
                .count();
            assertTrue(invalidMessages >= 3);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ReceiveToMainCLI with required dependencies")
        void shouldCreateReceiveToMainCLIWithRequiredDependencies() {
            // When
            ReceiveToMainCLI cli = new ReceiveToMainCLI(receiveFromSupplierUseCase);

            // Then
            assertNotNull(cli);
        }
    }
}
