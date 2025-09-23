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

import cli.manager.screens.BillReportScreen;
import application.reports.ReportService;
import application.reports.dto.BillHeaderRow;
import application.reports.dto.BillLineRow;
import cli.manager.filters.ReportFilters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

@DisplayName("BillReportScreen Tests")
class BillReportScreenTest {

    @Mock
    private ReportService reportService;

    private Scanner scanner;
    private BillReportScreen billReportScreen;
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
        @DisplayName("Should display bill report screen menu")
        void shouldDisplayBillReportScreenMenu() {
            // Given
            String input = "0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            billReportScreen = new BillReportScreen(reportService, scanner);

            // When
            billReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("BILL REPORT (SHELF SALES)"));
            assertTrue(output.contains("1. Today's Bills"));
            assertTrue(output.contains("2. Bills by Date"));
            assertTrue(output.contains("3. Bills by Date Range"));
            assertTrue(output.contains("4. View Bill Details"));
            assertTrue(output.contains("0. Back"));
        }

        @Test
        @DisplayName("Should display today's bills")
        void shouldDisplayTodaysBills() {
            // Given
            List<BillHeaderRow> todaysBills = List.of(
                new BillHeaderRow(1, 101L, "C-000001", "COUNTER", "Main Store",
                    LocalDateTime.now().minusHours(2), new BigDecimal("150.75"), "Cash: $150.75"),
                new BillHeaderRow(2, 102L, "C-000002", "COUNTER", "Main Store",
                    LocalDateTime.now().minusHours(1), new BigDecimal("89.50"), "Cash: $90.00")
            );

            when(reportService.bills(any(ReportFilters.class))).thenReturn(todaysBills);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            billReportScreen = new BillReportScreen(reportService, scanner);

            // When
            billReportScreen.run();

            // Then
            verify(reportService).bills(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.now())
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Today's Bills"));
            assertTrue(output.contains("C-000001"));
            assertTrue(output.contains("C-000002"));
            assertTrue(output.contains("150.75"));
        }

        @Test
        @DisplayName("Should view bill details")
        void shouldViewBillDetails() {
            // Given
            List<BillLineRow> billLines = List.of(
                new BillLineRow("PROD001", "Laptop", new BigDecimal("999.99"), 1, new BigDecimal("999.99")),
                new BillLineRow("PROD002", "Mouse", new BigDecimal("29.99"), 2, new BigDecimal("59.98"))
            );

            when(reportService.billLines(101L)).thenReturn(billLines);

            String input = "4\n101\n0\n"; // View details, bill ID, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            billReportScreen = new BillReportScreen(reportService, scanner);

            // When
            billReportScreen.run();

            // Then
            verify(reportService).billLines(101L);
            String output = outputStream.toString();
            assertTrue(output.contains("Bill Details for ID: 101"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Laptop"));
            assertTrue(output.contains("999.99"));
        }

        @Test
        @DisplayName("Should handle empty bill lines")
        void shouldHandleEmptyBillLines() {
            // Given
            when(reportService.billLines(999L)).thenReturn(List.of());

            String input = "4\n999\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            billReportScreen = new BillReportScreen(reportService, scanner);

            // When
            billReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No line items found"));
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
            billReportScreen = new BillReportScreen(reportService, scanner);

            when(reportService.bills(any(ReportFilters.class))).thenReturn(List.of());

            // When
            billReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle invalid bill ID")
        void shouldHandleInvalidBillId() {
            // Given
            String input = "4\nabc\n101\n0\n"; // Invalid then valid ID
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            billReportScreen = new BillReportScreen(reportService, scanner);

            when(reportService.billLines(101L)).thenReturn(List.of());

            // When
            billReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid bill ID"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create BillReportScreen with required dependencies")
        void shouldCreateBillReportScreenWithRequiredDependencies() {
            // Given
            scanner = new Scanner(System.in);

            // When
            BillReportScreen screen = new BillReportScreen(reportService, scanner);

            // Then
            assertNotNull(screen);
        }
    }
}
