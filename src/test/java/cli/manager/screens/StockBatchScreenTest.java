package cli.manager.screens;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.ReportService;
import application.reports.dto.StockBatchRow;
import cli.manager.filters.ReportFilters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

@DisplayName("StockBatchScreen Tests")
class StockBatchScreenTest {

    @Mock
    private ReportService reportService;

    private Scanner scanner;
    private StockBatchScreen stockBatchScreen;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        if (scanner != null) {
            scanner.close();
        }
    }

    @Nested
    @DisplayName("Screen Display Tests")
    class ScreenDisplayTests {

        @Test
        @DisplayName("Should display stock batch screen menu")
        void shouldDisplayStockBatchScreenMenu() {
            // Given
            String input = "0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("STOCK BATCH REPORT"));
            assertTrue(output.contains("1. All Batches"));
            assertTrue(output.contains("2. Expiring Soon (< 7 days)"));
            assertTrue(output.contains("3. By Location"));
            assertTrue(output.contains("0. Back"));
        }

        @Test
        @DisplayName("Should display all batches")
        void shouldDisplayAllBatches() {
            // Given
            List<StockBatchRow> allBatches = List.of(
                new StockBatchRow("PROD001", "Laptop", "BATCH001",
                    LocalDate.now().plusDays(30), LocalDateTime.now().minusDays(5), 50, "MAIN_STORE"),
                new StockBatchRow("PROD002", "Mouse", "BATCH002",
                    LocalDate.now().plusDays(45), LocalDateTime.now().minusDays(3), 100, "SHELF")
            );

            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(allBatches);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            verify(reportService).stockBatches(any(ReportFilters.class));
            String output = outputStream.toString();
            assertTrue(output.contains("All Stock Batches"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("BATCH001"));
            assertTrue(output.contains("MAIN_STORE"));
            assertTrue(output.contains("50"));
        }

        @Test
        @DisplayName("Should display expiring batches")
        void shouldDisplayExpiringBatches() {
            // Given
            List<StockBatchRow> expiringBatches = List.of(
                new StockBatchRow("PROD003", "Perishable", "BATCH003",
                    LocalDate.now().plusDays(3), LocalDateTime.now().minusDays(1), 25, "SHELF")
            );

            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(expiringBatches);

            String input = "2\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Expiring Soon"));
            assertTrue(output.contains("PROD003"));
            assertTrue(output.contains("Perishable"));
        }

        @Test
        @DisplayName("Should display batches by location")
        void shouldDisplayBatchesByLocation() {
            // Given
            List<StockBatchRow> locationBatches = List.of(
                new StockBatchRow("PROD004", "Location Item", "BATCH004",
                    LocalDate.now().plusDays(60), LocalDateTime.now().minusDays(2), 75, "WEB")
            );

            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(locationBatches);

            String input = "3\n1\n0\n"; // By location, select MAIN_STORE, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Select Location:"));
            assertTrue(output.contains("1. MAIN_STORE"));
            assertTrue(output.contains("2. SHELF"));
            assertTrue(output.contains("3. WEB"));
        }

        @Test
        @DisplayName("Should handle empty batch data")
        void shouldHandleEmptyBatchData() {
            // Given
            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(List.of());

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No batches found"));
        }
    }

    @Nested
    @DisplayName("Location Filter Tests")
    class LocationFilterTests {

        @Test
        @DisplayName("Should filter by MAIN_STORE location")
        void shouldFilterByMainStoreLocation() {
            // Given
            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(List.of());

            String input = "3\n1\n0\n"; // By location, MAIN_STORE, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("MAIN_STORE Batches"));
        }

        @Test
        @DisplayName("Should filter by SHELF location")
        void shouldFilterByShelfLocation() {
            // Given
            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(List.of());

            String input = "3\n2\n0\n"; // By location, SHELF, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("SHELF Batches"));
        }

        @Test
        @DisplayName("Should filter by WEB location")
        void shouldFilterByWebLocation() {
            // Given
            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(List.of());

            String input = "3\n3\n0\n"; // By location, WEB, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("WEB Batches"));
        }

        @Test
        @DisplayName("Should handle invalid location selection")
        void shouldHandleInvalidLocationSelection() {
            // Given
            String input = "3\n99\n1\n0\n"; // Invalid then valid location
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(List.of());

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid location"));
        }
    }

    @Nested
    @DisplayName("Data Formatting Tests")
    class DataFormattingTests {

        @Test
        @DisplayName("Should format batch data with expiry information")
        void shouldFormatBatchDataWithExpiryInformation() {
            // Given
            List<StockBatchRow> batches = List.of(
                new StockBatchRow("PROD001", "Expiring Item", "BATCH001",
                    LocalDate.now().plusDays(5), LocalDateTime.now().minusDays(1), 30, "SHELF")
            );

            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(batches);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Code"));
            assertTrue(output.contains("Name"));
            assertTrue(output.contains("Batch ID"));
            assertTrue(output.contains("Expiry"));
            assertTrue(output.contains("Quantity"));
            assertTrue(output.contains("Location"));
        }

        @Test
        @DisplayName("Should handle batches without expiry dates")
        void shouldHandleBatchesWithoutExpiryDates() {
            // Given
            List<StockBatchRow> batches = List.of(
                new StockBatchRow("PROD002", "No Expiry Item", "BATCH002",
                    null, LocalDateTime.now().minusDays(2), 40, "MAIN_STORE")
            );

            when(reportService.stockBatches(any(ReportFilters.class))).thenReturn(batches);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No Expiry") || output.contains("N/A"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle report service exceptions")
        void shouldHandleReportServiceExceptions() {
            // Given
            when(reportService.stockBatches(any(ReportFilters.class)))
                .thenThrow(new RuntimeException("Query execution failed"));

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            stockBatchScreen = new StockBatchScreen(reportService, scanner);

            // When
            stockBatchScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error generating batch report") ||
                      output.contains("Query execution failed"));
        }
    }
}
