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

import cli.manager.TransferFromMainCLI;
import application.usecase.TransferStockUseCase;
import application.services.AvailabilityService;
import application.usecase.QuoteUseCase;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import infrastructure.concurrency.Tx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

@DisplayName("TransferFromMainCLI Tests")
class TransferFromMainCLITest {

    @Mock
    private TransferStockUseCase transferStockUseCase;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private QuoteUseCase quoteUseCase;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private Tx tx;

    private TransferFromMainCLI transferFromMainCLI;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        transferFromMainCLI = new TransferFromMainCLI(
            transferStockUseCase, availabilityService, quoteUseCase, inventoryRepository, tx
        );

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
        try { if (mocks != null) mocks.close(); } catch (Exception ignored) {}
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPathTests {
        @Test
        @DisplayName("Should transfer stock between locations successfully")
        void shouldTransferStockSuccessfully() {
            // Given
            String input = String.join("\n",
                "PROD001",        // product code
                "MAIN_STORE",     // from
                "SHELF",          // to
                "n",              // don't view batch details
                "50",             // quantity
                "y",              // confirm
                "n"               // perform another transfer? -> no
            ) + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(quoteUseCase.productExists("PROD001")).thenReturn(true);

            Map<StockLocation, Integer> availability = new HashMap<>();
            availability.put(StockLocation.MAIN_STORE, 100);
            availability.put(StockLocation.SHELF, 10);
            availability.put(StockLocation.WEB, 5);
            when(availabilityService.getAvailabilityAcrossAllLocations("PROD001")).thenReturn(availability);
            when(availabilityService.available("PROD001", StockLocation.MAIN_STORE)).thenReturn(100);

            // When
            transferFromMainCLI.run();

            // Then
            verify(transferStockUseCase).transfer("PROD001", StockLocation.MAIN_STORE, StockLocation.SHELF, 50);
            String output = outputStream.toString();
            assertTrue(output.contains("Transfer completed successfully"));
            assertTrue(output.contains("=== Updated Stock Levels ==="));
        }

        @Test
        @DisplayName("Should transfer to WEB successfully")
        void shouldTransferToWebSuccessfully() {
            // Given
            String input = String.join("\n",
                "PROD002",
                "SHELF",
                "WEB",
                "n",
                "75",
                "y",
                "n"
            ) + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(quoteUseCase.productExists("PROD002")).thenReturn(true);
            Map<StockLocation, Integer> availability = new HashMap<>();
            availability.put(StockLocation.SHELF, 100);
            when(availabilityService.getAvailabilityAcrossAllLocations("PROD002")).thenReturn(availability);
            when(availabilityService.available("PROD002", StockLocation.SHELF)).thenReturn(100);

            // When
            transferFromMainCLI.run();

            // Then
            verify(transferStockUseCase).transfer("PROD002", StockLocation.SHELF, StockLocation.WEB, 75);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {
        @Test
        @DisplayName("Should handle non-existing product code and retry")
        void shouldHandleNonExistingProductCode() {
            // Given
            String input = String.join("\n",
                "INVALID",     // not exists
                "PROD001",     // then valid
                "MAIN_STORE",
                "SHELF",
                "n",
                "30",
                "y",
                "n"
            ) + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(quoteUseCase.productExists("INVALID")).thenReturn(false);
            when(quoteUseCase.productExists("PROD001")).thenReturn(true);
            when(availabilityService.getAvailabilityAcrossAllLocations("PROD001")).thenReturn(Map.of());
            when(availabilityService.available("PROD001", StockLocation.MAIN_STORE)).thenReturn(100);

            // When
            transferFromMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("does not exist. Please try again."));
            verify(transferStockUseCase).transfer("PROD001", StockLocation.MAIN_STORE, StockLocation.SHELF, 30);
        }

        @Test
        @DisplayName("Should handle invalid quantity then accept a valid one")
        void shouldHandleInvalidQuantityThenValid() {
            // Given
            String input = String.join("\n",
                "PROD001",
                "MAIN_STORE",
                "SHELF",
                "n",
                "abc",   // invalid
                "30",    // valid
                "y",
                "n"
            ) + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(quoteUseCase.productExists("PROD001")).thenReturn(true);
            when(availabilityService.getAvailabilityAcrossAllLocations("PROD001")).thenReturn(Map.of());
            when(availabilityService.available("PROD001", StockLocation.MAIN_STORE)).thenReturn(100);

            // When
            transferFromMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid number. Please enter a valid quantity"));
            verify(transferStockUseCase).transfer("PROD001", StockLocation.MAIN_STORE, StockLocation.SHELF, 30);
        }

        @Test
        @DisplayName("Entering 0 for quantity should go back to product prompt")
        void zeroQuantityGoesBackToStart() {
            // Given
            String input = String.join("\n",
                "PROD001",
                "MAIN_STORE",
                "SHELF",
                "n",
                "0",        // go back
                "exit"      // exit program
            ) + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(quoteUseCase.productExists("PROD001")).thenReturn(true);
            when(availabilityService.getAvailabilityAcrossAllLocations("PROD001")).thenReturn(Map.of());
            when(availabilityService.available("PROD001", StockLocation.MAIN_STORE)).thenReturn(100);

            // When
            transferFromMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product code (or 'exit' to quit): "));
            verifyNoInteractions(transferStockUseCase);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        @Test
        @DisplayName("Should report error when transfer fails")
        void shouldReportErrorWhenTransferFails() {
            // Given
            String input = String.join("\n",
                "PROD001",
                "MAIN_STORE",
                "SHELF",
                "n",
                "100",
                "y",
                "exit"
            ) + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(quoteUseCase.productExists("PROD001")).thenReturn(true);
            when(availabilityService.getAvailabilityAcrossAllLocations("PROD001")).thenReturn(Map.of());
            when(availabilityService.available("PROD001", StockLocation.MAIN_STORE)).thenReturn(100);

            doThrow(new RuntimeException("Insufficient stock")).when(transferStockUseCase)
                .transfer("PROD001", StockLocation.MAIN_STORE, StockLocation.SHELF, 100);

            // When
            transferFromMainCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("✗ Error:"));
            assertTrue(output.contains("Insufficient stock"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        @DisplayName("Should create TransferFromMainCLI with required dependencies")
        void shouldCreateTransferFromMainCLIWithRequiredDependencies() {
            // When
            TransferFromMainCLI cli = new TransferFromMainCLI(
                transferStockUseCase, availabilityService, quoteUseCase, inventoryRepository, tx
            );

            // Then
            assertNotNull(cli);
        }
    }
}
