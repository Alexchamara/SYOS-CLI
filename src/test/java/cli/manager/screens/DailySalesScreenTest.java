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

import cli.manager.screens.DailySalesScreen;
import application.reports.ReportService;
import application.reports.dto.DailySalesRow;
import cli.manager.filters.ReportFilters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

@DisplayName("DailySalesScreen Tests")
class DailySalesScreenTest {

    @Mock
    private ReportService reportService;

    private Scanner scanner;
    private DailySalesScreen dailySalesScreen;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

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
        if (scanner != null) {
            scanner.close();
        }
    }

    @Nested
    @DisplayName("Screen Display Tests")
    class ScreenDisplayTests {

        @Test
        @DisplayName("Should display daily sales screen menu")
        void shouldDisplayDailySalesScreenMenu() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("DAILY SALES REPORT"));
            assertTrue(output.contains("1. Today's Sales"));
            assertTrue(output.contains("2. Sales by Date"));
            assertTrue(output.contains("3. Sales by Date Range"));
            assertTrue(output.contains("0. Back"));
        }

        @Test
        @DisplayName("Should display today's sales")
        void shouldDisplayTodaysSales() {
            // Given
            List<DailySalesRow> todaysSales = List.of(
                new DailySalesRow("PROD001", "Laptop", "SHELF", 5,
                    new BigDecimal("2500.00"), new BigDecimal("250.00"), new BigDecimal("2250.00")),
                new DailySalesRow("PROD002", "Mouse", "WEB", 10,
                    new BigDecimal("300.00"), new BigDecimal("30.00"), new BigDecimal("270.00"))
            );

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(todaysSales);

            String input = "1\n0\n"; // Today's sales, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            verify(reportService).dailySales(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.now())
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Today's Sales Report"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Laptop"));
            assertTrue(output.contains("SHELF"));
            assertTrue(output.contains("2250.00"));
        }

        @Test
        @DisplayName("Should display sales by specific date")
        void shouldDisplaySalesBySpecificDate() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD003", "Keyboard", "WEB", 3,
                    new BigDecimal("150.00"), new BigDecimal("15.00"), new BigDecimal("135.00"))
            );

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(salesData);

            String input = "2\n2025-09-20\n0\n"; // Sales by date, specific date, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            verify(reportService).dailySales(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.of(2025, 9, 20))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Sales Report for 2025-09-20"));
            assertTrue(output.contains("PROD003"));
        }

        @Test
        @DisplayName("Should display sales by date range")
        void shouldDisplaySalesByDateRange() {
            // Given
            List<DailySalesRow> rangeData = List.of(
                new DailySalesRow("PROD004", "Monitor", "SHELF", 2,
                    new BigDecimal("800.00"), new BigDecimal("50.00"), new BigDecimal("750.00"))
            );

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(rangeData);

            String input = "3\n2025-09-15\n2025-09-22\n0\n"; // Date range, from, to, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            verify(reportService).dailySales(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.DATE_RANGE &&
                filters.fromDate().equals(LocalDate.of(2025, 9, 15)) &&
                filters.toDate().equals(LocalDate.of(2025, 9, 22))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Sales Report from 2025-09-15 to 2025-09-22"));
        }
    }

    @Nested
    @DisplayName("Data Display Tests")
    class DataDisplayTests {

        @Test
        @DisplayName("Should handle empty sales data")
        void shouldHandleEmptySalesData() {
            // Given
            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(List.of());

            String input = "1\n0\n"; // Today's sales with no data
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No sales data found"));
        }

        @Test
        @DisplayName("Should format sales data correctly")
        void shouldFormatSalesDataCorrectly() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Test Product", "SHELF", 1,
                    new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"))
            );

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(salesData);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product"));
            assertTrue(output.contains("Location"));
            assertTrue(output.contains("Qty"));
            assertTrue(output.contains("Gross"));
            assertTrue(output.contains("Discount"));
            assertTrue(output.contains("Net"));
        }

        @Test
        @DisplayName("Should calculate totals correctly")
        void shouldCalculateTotalsCorrectly() {
            // Given
            List<DailySalesRow> salesData = List.of(
                new DailySalesRow("PROD001", "Product 1", "SHELF", 2,
                    new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("180.00")),
                new DailySalesRow("PROD002", "Product 2", "WEB", 3,
                    new BigDecimal("300.00"), new BigDecimal("30.00"), new BigDecimal("270.00"))
            );

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(salesData);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Total Quantity: 5"));
            assertTrue(output.contains("Total Gross: Rs. 500.00"));
            assertTrue(output.contains("Total Discount: Rs. 50.00"));
            assertTrue(output.contains("Total Net: Rs. 450.00"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String input = "2\ninvalid-date\n2025-09-20\n0\n"; // Invalid then valid date
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(List.of());

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle invalid date range")
        void shouldHandleInvalidDateRange() {
            // Given
            String input = "3\n2025-09-20\n2025-09-15\n2025-09-15\n2025-09-22\n0\n"; // End before start, then valid
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            when(reportService.dailySales(any(ReportFilters.class))).thenReturn(List.of());

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("End date must be after start date"));
        }

        @Test
        @DisplayName("Should handle invalid menu choices")
        void shouldHandleInvalidMenuChoices() {
            // Given
            String input = "99\nabc\n-1\n0\n"; // Invalid choices, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

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
        @DisplayName("Should create DailySalesScreen with required dependencies")
        void shouldCreateDailySalesScreenWithRequiredDependencies() {
            // Given
            scanner = new Scanner(System.in);

            // When
            DailySalesScreen screen = new DailySalesScreen(reportService, scanner);

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
            when(reportService.dailySales(any(ReportFilters.class)))
                .thenThrow(new RuntimeException("Database error"));

            String input = "1\n0\n"; // Today's sales
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            dailySalesScreen = new DailySalesScreen(reportService, scanner);

            // When
            dailySalesScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error generating report") || output.contains("Database error"));
        }
    }
}
