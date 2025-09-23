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
import application.reports.dto.ReshelveRow;
import cli.manager.filters.ReportFilters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

@DisplayName("ReshelveScreen Tests")
class ReshelveScreenTest {

    @Mock
    private ReportService reportService;

    private Scanner scanner;
    private ReshelveScreen reshelveScreen;
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
        @DisplayName("Should display reshelve screen menu")
        void shouldDisplayReshelveScreenMenu() {
            // Given
            String input = "0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("RESHELVE REPORT"));
            assertTrue(output.contains("1. Today's Movements"));
            assertTrue(output.contains("2. Movements by Date"));
            assertTrue(output.contains("3. Movements by Date Range"));
            assertTrue(output.contains("0. Back"));
        }

        @Test
        @DisplayName("Should display today's reshelve movements")
        void shouldDisplayTodaysReshelveMovements() {
            // Given
            List<ReshelveRow> todaysMovements = List.of(
                new ReshelveRow(1L, LocalDateTime.now().minusHours(2), "PROD001", "Laptop",
                    "MAIN_STORE", "SHELF", 25, "Manual transfer to shelf"),
                new ReshelveRow(2L, LocalDateTime.now().minusHours(1), "PROD002", "Monitor",
                    "MAIN_STORE", "WEB", 15, "Stock transfer for online orders")
            );

            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(todaysMovements);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            verify(reportService).reshelve(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.now())
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Today's Reshelve Movements"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("MAIN_STORE -> SHELF"));
            assertTrue(output.contains("25"));
            assertTrue(output.contains("Manual transfer to shelf"));
        }

        @Test
        @DisplayName("Should display movements by specific date")
        void shouldDisplayMovementsBySpecificDate() {
            // Given
            List<ReshelveRow> dateMovements = List.of(
                new ReshelveRow(3L, LocalDateTime.of(2025, 9, 20, 14, 30), "PROD003", "Keyboard",
                    "SHELF", "WEB", 10, "Transfer for web stock")
            );

            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(dateMovements);

            String input = "2\n2025-09-20\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            verify(reportService).reshelve(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.of(2025, 9, 20))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Reshelve Movements for 2025-09-20"));
            assertTrue(output.contains("PROD003"));
            assertTrue(output.contains("SHELF -> WEB"));
        }

        @Test
        @DisplayName("Should display movements by date range")
        void shouldDisplayMovementsByDateRange() {
            // Given
            List<ReshelveRow> rangeMovements = List.of(
                new ReshelveRow(4L, LocalDateTime.of(2025, 9, 18, 10, 0), "PROD004", "Mouse",
                    "MAIN_STORE", "SHELF", 30, "Restocking shelf"),
                new ReshelveRow(5L, LocalDateTime.of(2025, 9, 19, 15, 30), "PROD005", "Cable",
                    "SHELF", "WEB", 20, "Online demand")
            );

            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(rangeMovements);

            String input = "3\n2025-09-18\n2025-09-20\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            verify(reportService).reshelve(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.DATE_RANGE &&
                filters.fromDate().equals(LocalDate.of(2025, 9, 18)) &&
                filters.toDate().equals(LocalDate.of(2025, 9, 20))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Reshelve Movements from 2025-09-18 to 2025-09-20"));
            assertTrue(output.contains("PROD004"));
            assertTrue(output.contains("PROD005"));
        }

        @Test
        @DisplayName("Should handle empty movement data")
        void shouldHandleEmptyMovementData() {
            // Given
            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(List.of());

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No movements found"));
        }
    }

    @Nested
    @DisplayName("Data Formatting Tests")
    class DataFormattingTests {

        @Test
        @DisplayName("Should format movement data with timestamps")
        void shouldFormatMovementDataWithTimestamps() {
            // Given
            List<ReshelveRow> movements = List.of(
                new ReshelveRow(1L, LocalDateTime.of(2025, 9, 22, 10, 30, 45), "PROD001", "Test Product",
                    "MAIN_STORE", "SHELF", 15, "Test movement")
            );

            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(movements);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Time"));
            assertTrue(output.contains("Product"));
            assertTrue(output.contains("From"));
            assertTrue(output.contains("To"));
            assertTrue(output.contains("Quantity"));
            assertTrue(output.contains("Note"));
            assertTrue(output.contains("10:30")); // Time formatting
        }

        @Test
        @DisplayName("Should show movement direction clearly")
        void shouldShowMovementDirectionClearly() {
            // Given
            List<ReshelveRow> movements = List.of(
                new ReshelveRow(1L, LocalDateTime.now(), "PROD001", "Product",
                    "MAIN_STORE", "SHELF", 20, "To shelf"),
                new ReshelveRow(2L, LocalDateTime.now(), "PROD002", "Product",
                    "SHELF", "WEB", 10, "To web")
            );

            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(movements);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("MAIN_STORE") && output.contains("SHELF"));
            assertTrue(output.contains("SHELF") && output.contains("WEB"));
            assertTrue(output.contains("->") || output.contains("to"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String input = "2\ninvalid-date\n2025-09-20\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            when(reportService.reshelve(any(ReportFilters.class))).thenReturn(List.of());

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle invalid menu choices")
        void shouldHandleInvalidMenuChoices() {
            // Given
            String input = "99\nabc\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid option"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ReshelveScreen with required dependencies")
        void shouldCreateReshelveScreenWithRequiredDependencies() {
            // Given
            scanner = new Scanner(System.in);

            // When
            ReshelveScreen screen = new ReshelveScreen(reportService, scanner);

            // Then
            assertNotNull(screen);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle report service exceptions")
        void shouldHandleReportServiceExceptions() {
            // Given
            when(reportService.reshelve(any(ReportFilters.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reshelveScreen = new ReshelveScreen(reportService, scanner);

            // When
            reshelveScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error generating report") ||
                      output.contains("Database connection failed"));
        }
    }
}
