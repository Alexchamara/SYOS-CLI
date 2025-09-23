package cli.manager.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cli.manager.batch.BatchManagementCLI;
import application.usecase.BatchManagementUseCase;
import domain.inventory.StockLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;

@DisplayName("BatchManagementCLI Tests")
class BatchManagementCLITest {

    @Mock
    private BatchManagementUseCase batchManagementUseCase;

    private BatchManagementCLI batchManagementCLI;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        batchManagementCLI = new BatchManagementCLI(batchManagementUseCase);

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
        @DisplayName("Should display batch management menu")
        void shouldDisplayBatchManagementMenu() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("BATCH MANAGEMENT"));
            assertTrue(output.contains("1. Add New Batch"));
            assertTrue(output.contains("2. Update Batch"));
            assertTrue(output.contains("3. View Batch Details"));
            assertTrue(output.contains("4. View All Batches"));
            assertTrue(output.contains("5. View Batches by Product"));
            assertTrue(output.contains("6. View Batches by Location"));
            assertTrue(output.contains("7. Delete Batch"));
            assertTrue(output.contains("0. Back to Manager Menu"));
        }

        @Test
        @DisplayName("Should handle menu navigation and exit")
        void shouldHandleMenuNavigationAndExit() {
            // Given
            String input = "0\n"; // Exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("BATCH MANAGEMENT"));
        }
    }

    @Nested
    @DisplayName("Add Batch Tests")
    class AddBatchTests {

        @Test
        @DisplayName("Should add batch successfully")
        void shouldAddBatchSuccessfully() {
            // Given
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            String input = "1\nPROD001\n1\n2025-12-31\n100\n0\n"; // Add batch: code, location, expiry, qty, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).createBatch(argThat(request ->
                request.productCode().equals("PROD001") &&
                request.location().equals(StockLocation.MAIN_STORE) &&
                request.expiry().equals(LocalDate.of(2025, 12, 31)) &&
                request.quantity() == 100
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Batch created successfully"));
        }

        @Test
        @DisplayName("Should handle invalid stock location")
        void shouldHandleInvalidStockLocation() {
            // Given
            String input = "1\nPROD001\n99\n1\n2025-12-31\n100\n0\n"; // Invalid location, then valid
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid location"));
        }

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String input = "1\nPROD001\n1\ninvalid-date\n2025-12-31\n100\n0\n"; // Invalid then valid date
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle invalid quantity")
        void shouldHandleInvalidQuantity() {
            // Given
            String input = "1\nPROD001\n1\n2025-12-31\nabc\n50\n0\n"; // Invalid then valid quantity
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid quantity"));
        }

        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            // Given
            String input = "1\nPROD001\n1\n2025-12-31\n0\n25\n0\n"; // Zero then valid quantity
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Quantity must be positive"));
        }
    }

    @Nested
    @DisplayName("View Batch Tests")
    class ViewBatchTests {

        @Test
        @DisplayName("Should view batch details")
        void shouldViewBatchDetails() {
            // Given
            BatchManagementUseCase.BatchInfo batchInfo =
                new BatchManagementUseCase.BatchInfo(
                    new domain.inventory.Batch(1L,
                        new domain.shared.Code("PROD001"),
                        StockLocation.MAIN_STORE,
                        java.time.LocalDateTime.now(),
                        LocalDate.now().plusDays(30),
                        new domain.shared.Quantity(100))
                );

            when(batchManagementUseCase.findBatch(1L)).thenReturn(java.util.Optional.of(batchInfo));

            String input = "3\n1\n0\n"; // View batch, enter ID, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).findBatch(1L);
            String output = outputStream.toString();
            assertTrue(output.contains("Batch Details:"));
            assertTrue(output.contains("ID: 1"));
            assertTrue(output.contains("Product Code: PROD001"));
        }

        @Test
        @DisplayName("Should handle batch not found")
        void shouldHandleBatchNotFound() {
            // Given
            when(batchManagementUseCase.findBatch(999L)).thenReturn(java.util.Optional.empty());

            String input = "3\n999\n0\n"; // View non-existent batch
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Batch not found"));
        }
    }

    @Nested
    @DisplayName("Update Batch Tests")
    class UpdateBatchTests {

        @Test
        @DisplayName("Should update batch successfully")
        void shouldUpdateBatchSuccessfully() {
            // Given
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.SUCCESS);

            String input = "2\n1\n2026-01-15\n150\n0\n"; // Update: ID, expiry, quantity, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).updateBatch(argThat(request ->
                request.batchId() == 1L &&
                request.expiry().equals(LocalDate.of(2026, 1, 15)) &&
                request.quantity() == 150
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Batch updated successfully"));
        }

        @Test
        @DisplayName("Should handle batch not found for update")
        void shouldHandleBatchNotFoundForUpdate() {
            // Given
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.NOT_FOUND);

            String input = "2\n999\n2025-12-31\n100\n0\n"; // Update non-existent batch
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Batch not found"));
        }
    }

    @Nested
    @DisplayName("Delete Batch Tests")
    class DeleteBatchTests {

        @Test
        @DisplayName("Should delete batch with confirmation")
        void shouldDeleteBatchWithConfirmation() {
            // Given
            when(batchManagementUseCase.deleteBatch(1L)).thenReturn(BatchManagementUseCase.DeleteResult.SUCCESS);

            String input = "7\n1\ny\n0\n"; // Delete, ID, confirm, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).deleteBatch(1L);
            String output = outputStream.toString();
            assertTrue(output.contains("Batch deleted successfully"));
        }

        @Test
        @DisplayName("Should handle delete confirmation cancellation")
        void shouldHandleDeleteConfirmationCancellation() {
            // Given
            String input = "7\n1\nn\n0\n"; // Delete, ID, cancel, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            batchManagementCLI.run();

            // Then
            verifyNoInteractions(batchManagementUseCase);
            String output = outputStream.toString();
            assertTrue(output.contains("Delete cancelled"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create BatchManagementCLI with required dependencies")
        void shouldCreateBatchManagementCLIWithRequiredDependencies() {
            // When
            BatchManagementCLI cli = new BatchManagementCLI(batchManagementUseCase);

            // Then
            assertNotNull(cli);
        }
    }
}
