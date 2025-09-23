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

import application.usecase.BatchManagementUseCase;
import domain.inventory.StockLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.time.LocalDate;

@DisplayName("BatchManagementCLI Tests")
class BatchManagementCLITest {

    @Mock
    private BatchManagementUseCase batchManagementUseCase;

    private BatchManagementCLI batchManagementCLI;
    private Scanner scanner;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Capture System.out for testing
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setIn(originalIn);
        if (scanner != null) {
            scanner.close();
        }
        if (mocks != null) mocks.close();
    }

    private void setupInputAndCLI(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        scanner = new Scanner(System.in);
        batchManagementCLI = new BatchManagementCLI(batchManagementUseCase, scanner);
    }

    @Nested
    @DisplayName("Menu Display Tests")
    class MenuDisplayTests {

        @Test
        @DisplayName("Should display batch management menu")
        void shouldDisplayBatchManagementMenu() {
            // Given
            setupInputAndCLI("0\n"); // Exit immediately

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
            setupInputAndCLI("0\n"); // Exit

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("BATCH MANAGEMENT"));
        }

        @Test
        @DisplayName("Should handle invalid menu option")
        void shouldHandleInvalidMenuOption() {
            // Given
            setupInputAndCLI("99\n\n0\n"); // Invalid option, press enter, then exit

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid option. Please try again."));
            assertTrue(output.contains("Press Enter to continue..."));
        }
    }

    @Nested
    @DisplayName("Add Batch Tests")
    class AddBatchTests {

        @Test
        @DisplayName("Should add batch successfully")
        void shouldAddBatchSuccessfully() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\n2025-12-31\n100\n\n0\n"); // Add batch with enter for continue

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
        @DisplayName("Should handle product not exists")
        void shouldHandleProductNotExists() {
            // Given
            when(batchManagementUseCase.productExists("NONEXISTENT")).thenReturn(false);

            setupInputAndCLI("1\nNONEXISTENT\n\n0\n"); // Product code, enter for continue, exit

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product with code 'NONEXISTENT' does not exist"));
            assertTrue(output.contains("Please create the product first"));
        }

        @Test
        @DisplayName("Should handle invalid stock location")
        void shouldHandleInvalidStockLocation() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nINVALID\nMAIN_STORE\n2025-12-31\n100\n\n0\n"); // Invalid then valid location

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
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\ninvalid-date\n2025-12-31\n100\n\n0\n"); // Invalid then valid date

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle past expiry date")
        void shouldHandlePastExpiryDate() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\n2020-01-01\n2025-12-31\n100\n\n0\n"); // Past date then valid

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Expiry date cannot be in the past"));
        }

        @Test
        @DisplayName("Should handle no expiry date")
        void shouldHandleNoExpiryDate() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\n\n100\n\n0\n"); // Empty expiry date

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).createBatch(argThat(request ->
                request.productCode().equals("PROD001") &&
                request.expiry() == null &&
                request.quantity() == 100
            ));
        }

        @Test
        @DisplayName("Should handle invalid quantity")
        void shouldHandleInvalidQuantity() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\n2025-12-31\nabc\n50\n\n0\n"); // Invalid then valid quantity

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid quantity format"));
        }

        @Test
        @DisplayName("Should handle negative quantity")
        void shouldHandleNegativeQuantity() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\n2025-12-31\n-5\n25\n\n0\n"); // Negative then valid quantity

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Quantity cannot be negative"));
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
            setupInputAndCLI("3\n1\n\n0\n"); // View batch, enter ID, press enter to continue, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).findBatch(1L);
            String output = outputStream.toString();
            assertTrue(output.contains("BATCH DETAILS"));
            assertTrue(output.contains("ID           : 1"));
            assertTrue(output.contains("Product Code : PROD001"));
        }

        @Test
        @DisplayName("Should handle batch not found")
        void shouldHandleBatchNotFound() {
            // Given
            when(batchManagementUseCase.findBatch(999L)).thenReturn(java.util.Optional.empty());
            setupInputAndCLI("3\n999\n\n0\n"); // View non-existent batch, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Batch not found with ID: 999"));
        }

        @Test
        @DisplayName("Should handle invalid batch ID format")
        void shouldHandleInvalidBatchIdFormat() {
            // Given
            setupInputAndCLI("3\nabc\n1\n\n0\n"); // Invalid ID, then valid ID, press enter, exit

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

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid batch ID format"));
        }

        @Test
        @DisplayName("Should handle negative batch ID")
        void shouldHandleNegativeBatchId() {
            // Given
            setupInputAndCLI("3\n-1\n1\n\n0\n"); // Negative ID, then valid ID, press enter, exit

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

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Batch ID must be positive"));
        }
    }

    @Nested
    @DisplayName("Update Batch Tests")
    class UpdateBatchTests {

        @Test
        @DisplayName("Should update batch expiry only")
        void shouldUpdateBatchExpiryOnly() {
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
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.SUCCESS);

            // Input: menu choice 2 (update), batch ID 1, update menu choice 1 (expiry only), new expiry, press enter for continue, exit
            setupInputAndCLI("2\n1\n1\n2026-01-15\n\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).updateBatch(argThat(request ->
                request.batchId() == 1L &&
                request.expiry().equals(LocalDate.of(2026, 1, 15)) &&
                request.quantity() == 100 // Should keep original quantity
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Batch expiry updated successfully"));
        }

        @Test
        @DisplayName("Should update batch quantity only")
        void shouldUpdateBatchQuantityOnly() {
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
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.SUCCESS);

            // Input: menu choice 2 (update), batch ID 1, update menu choice 2 (quantity only), new quantity, press enter for continue, exit
            setupInputAndCLI("2\n1\n2\n150\n\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).updateBatch(argThat(request ->
                request.batchId() == 1L &&
                request.quantity() == 150
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Batch quantity updated successfully"));
        }

        @Test
        @DisplayName("Should update both expiry and quantity")
        void shouldUpdateBothExpiryAndQuantity() {
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
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.SUCCESS);

            // Input: menu choice 2 (update), batch ID 1, update menu choice 3 (both), new expiry, new quantity, press enter for continue, exit
            setupInputAndCLI("2\n1\n3\n2026-01-15\n200\n\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).updateBatch(argThat(request ->
                request.batchId() == 1L &&
                request.expiry().equals(LocalDate.of(2026, 1, 15)) &&
                request.quantity() == 200
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Batch updated successfully"));
        }

        @Test
        @DisplayName("Should handle update cancellation")
        void shouldHandleUpdateCancellation() {
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

            // Input: menu choice 2 (update), batch ID 1, update menu choice 0 (cancel), main menu exit
            setupInputAndCLI("2\n1\n0\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase, never()).updateBatch(any());
            String output = outputStream.toString();
            assertTrue(output.contains("Update cancelled"));
        }

        @Test
        @DisplayName("Should handle batch not found for update")
        void shouldHandleBatchNotFoundForUpdate() {
            // Given
            when(batchManagementUseCase.findBatch(999L)).thenReturn(java.util.Optional.empty());

            // Input: menu choice 2 (update), batch ID 999 (non-existent), press enter for continue, exit
            setupInputAndCLI("2\n999\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Batch not found with ID: 999"));
        }

        @Test
        @DisplayName("Should handle invalid update menu option")
        void shouldHandleInvalidUpdateMenuOption() {
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

            // Input: menu choice 2 (update), batch ID 1, invalid update option 99, then cancel (0), main menu exit
            setupInputAndCLI("2\n1\n99\n0\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid option. Please try again"));
        }

        @Test
        @DisplayName("Should handle update batch with past expiry date retry")
        void shouldHandleUpdateBatchWithPastExpiryDateRetry() {
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
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.SUCCESS);

            // Input: update, batch ID, expiry only, past date (invalid), future date (valid), press enter, exit
            setupInputAndCLI("2\n1\n1\n2020-01-01\n2026-01-15\n\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Expiry date cannot be in the past"));
            assertTrue(output.contains("Batch expiry updated successfully"));
        }

        @Test
        @DisplayName("Should handle update batch failure")
        void shouldHandleUpdateBatchFailure() {
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
            when(batchManagementUseCase.updateBatch(any())).thenReturn(
                BatchManagementUseCase.UpdateResult.NOT_FOUND);

            setupInputAndCLI("2\n1\n1\n2026-01-15\n\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Failed to update batch: NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Delete Batch Tests")
    class DeleteBatchTests {

        @Test
        @DisplayName("Should delete batch with confirmation")
        void shouldDeleteBatchWithConfirmation() {
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
            when(batchManagementUseCase.deleteBatch(1L)).thenReturn(BatchManagementUseCase.DeleteResult.SUCCESS);

            setupInputAndCLI("7\n1\ny\n\n0\n"); // Delete, ID, confirm, press enter, exit

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

            setupInputAndCLI("7\n1\nn\n\n0\n"); // Delete, ID, cancel, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase, never()).deleteBatch(anyLong());
            String output = outputStream.toString();
            assertTrue(output.contains("Delete operation cancelled"));
        }

        @Test
        @DisplayName("Should handle batch not found for delete")
        void shouldHandleBatchNotFoundForDelete() {
            // Given
            when(batchManagementUseCase.findBatch(999L)).thenReturn(java.util.Optional.empty());

            setupInputAndCLI("7\n999\n\n0\n"); // Delete non-existent batch, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase, never()).deleteBatch(anyLong());
            String output = outputStream.toString();
            assertTrue(output.contains("Batch not found with ID: 999"));
        }

        @Test
        @DisplayName("Should handle delete result not found")
        void shouldHandleDeleteResultNotFound() {
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
            when(batchManagementUseCase.deleteBatch(1L)).thenReturn(BatchManagementUseCase.DeleteResult.NOT_FOUND);

            setupInputAndCLI("7\n1\ny\n\n0\n"); // Delete, ID, confirm, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).deleteBatch(1L);
            String output = outputStream.toString();
            assertTrue(output.contains("Batch not found"));
        }
    }

    @Nested
    @DisplayName("List Operations Tests")
    class ListOperationsTests {

        @Test
        @DisplayName("Should view all batches when empty")
        void shouldViewAllBatchesWhenEmpty() {
            // Given
            when(batchManagementUseCase.listAllBatches()).thenReturn(java.util.List.of());

            setupInputAndCLI("4\n\n0\n"); // View all batches, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).listAllBatches();
            String output = outputStream.toString();
            assertTrue(output.contains("No batches found in the system"));
            assertTrue(output.contains("The database is empty"));
        }

        @Test
        @DisplayName("Should view all batches with data")
        void shouldViewAllBatchesWithData() {
            // Given
            java.util.List<BatchManagementUseCase.BatchInfo> batches = java.util.List.of(
                new BatchManagementUseCase.BatchInfo(
                    new domain.inventory.Batch(1L,
                        new domain.shared.Code("PROD001"),
                        StockLocation.MAIN_STORE,
                        java.time.LocalDateTime.now(),
                        LocalDate.now().plusDays(30),
                        new domain.shared.Quantity(100))
                )
            );

            when(batchManagementUseCase.listAllBatches()).thenReturn(batches);

            setupInputAndCLI("4\n\n0\n"); // View all batches, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).listAllBatches();
            String output = outputStream.toString();
            assertTrue(output.contains("ID"));
            assertTrue(output.contains("PRODUCT"));
            assertTrue(output.contains("LOCATION"));
            assertTrue(output.contains("Total batches: 1"));
        }

        @Test
        @DisplayName("Should view batches by product")
        void shouldViewBatchesByProduct() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            java.util.List<BatchManagementUseCase.BatchInfo> batches = java.util.List.of(
                new BatchManagementUseCase.BatchInfo(
                    new domain.inventory.Batch(1L,
                        new domain.shared.Code("PROD001"),
                        StockLocation.MAIN_STORE,
                        java.time.LocalDateTime.now(),
                        LocalDate.now().plusDays(30),
                        new domain.shared.Quantity(100))
                )
            );

            when(batchManagementUseCase.listBatchesByProduct("PROD001")).thenReturn(batches);

            setupInputAndCLI("5\nPROD001\n\n0\n"); // View by product, product code, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).listBatchesByProduct("PROD001");
            String output = outputStream.toString();
            assertTrue(output.contains("Batches for product: PROD001"));
            assertTrue(output.contains("Total batches: 1"));
        }

        @Test
        @DisplayName("Should handle product not exists for view by product")
        void shouldHandleProductNotExistsForViewByProduct() {
            // Given
            when(batchManagementUseCase.productExists("NONEXISTENT")).thenReturn(false);

            setupInputAndCLI("5\nNONEXISTENT\n\n0\n"); // View by product, invalid product, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product with code 'NONEXISTENT' does not exist"));
        }

        @Test
        @DisplayName("Should view batches by location")
        void shouldViewBatchesByLocation() {
            // Given
            java.util.List<BatchManagementUseCase.BatchInfo> batches = java.util.List.of(
                new BatchManagementUseCase.BatchInfo(
                    new domain.inventory.Batch(1L,
                        new domain.shared.Code("PROD001"),
                        StockLocation.MAIN_STORE,
                        java.time.LocalDateTime.now(),
                        LocalDate.now().plusDays(30),
                        new domain.shared.Quantity(100))
                )
            );

            when(batchManagementUseCase.listBatchesByLocation(StockLocation.MAIN_STORE)).thenReturn(batches);

            setupInputAndCLI("6\nMAIN_STORE\n\n0\n"); // View by location, location, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).listBatchesByLocation(StockLocation.MAIN_STORE);
            String output = outputStream.toString();
            assertTrue(output.contains("Batches at location: MAIN_STORE"));
            assertTrue(output.contains("Total batches: 1"));
        }

        @Test
        @DisplayName("Should handle empty batches by location")
        void shouldHandleEmptyBatchesByLocation() {
            // Given
            when(batchManagementUseCase.listBatchesByLocation(StockLocation.SHELF)).thenReturn(java.util.List.of());

            setupInputAndCLI("6\nSHELF\n\n0\n"); // View by location, location, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            verify(batchManagementUseCase).listBatchesByLocation(StockLocation.SHELF);
            String output = outputStream.toString();
            assertTrue(output.contains("No batches found at location: SHELF"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle empty product code input")
        void shouldHandleEmptyProductCodeInput() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.SUCCESS);

            setupInputAndCLI("1\n\nPROD001\nMAIN_STORE\n2025-12-31\n100\n\n0\n"); // Empty then valid product code

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product code cannot be empty"));
        }

        @Test
        @DisplayName("Should handle create batch failure")
        void shouldHandleCreateBatchFailure() {
            // Given
            when(batchManagementUseCase.productExists("PROD001")).thenReturn(true);
            when(batchManagementUseCase.createBatch(any())).thenReturn(
                BatchManagementUseCase.CreateResult.INVALID_INPUT);

            setupInputAndCLI("1\nPROD001\nMAIN_STORE\n2025-12-31\n100\n\n0\n");

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error: Invalid input provided"));
        }

        @Test
        @DisplayName("Should handle exception during batch operations")
        void shouldHandleExceptionDuringBatchOperations() {
            // Given
            when(batchManagementUseCase.listAllBatches()).thenThrow(new RuntimeException("Database error"));

            setupInputAndCLI("4\n\n0\n"); // View all batches, press enter, exit

            // When
            batchManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("An error occurred while retrieving batches"));
            assertTrue(output.contains("Database error"));
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
