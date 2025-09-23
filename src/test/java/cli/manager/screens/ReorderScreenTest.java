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
import application.reports.dto.ReorderRow;
import cli.manager.filters.ReportFilters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

@DisplayName("ReorderScreen Tests")
class ReorderScreenTest {

    @Mock
    private ReportService reportService;

    private Scanner scanner;
    private ReorderScreen reorderScreen;
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
        @DisplayName("Should display reorder screen menu")
        void shouldDisplayReorderScreenMenu() {
            // Given
            String input = "0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            // When
            reorderScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("REORDER REPORT"));
            assertTrue(output.contains("1. Standard Reorder (< 50)"));
            assertTrue(output.contains("2. Custom Threshold"));
            assertTrue(output.contains("0. Back"));
        }

        @Test
        @DisplayName("Should generate standard reorder report")
        void shouldGenerateStandardReorderReport() {
            // Given
            List<ReorderRow> reorderData = List.of(
                new ReorderRow("PROD001", "Critical Item", "BATCH001", "SHELF", 15,
                    LocalDate.now().plusDays(10), "CRITICAL"),
                new ReorderRow("PROD002", "Low Stock", "BATCH002", "MAIN_STORE", 35,
                    LocalDate.now().plusDays(30), "LOW")
            );

            when(reportService.reorder(any(ReportFilters.class), eq(50))).thenReturn(reorderData);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            // When
            reorderScreen.run();

            // Then
            verify(reportService).reorder(any(ReportFilters.class), eq(50));
            String output = outputStream.toString();
            assertTrue(output.contains("Reorder Report (Threshold: 50)"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("CRITICAL"));
            assertTrue(output.contains("PROD002"));
            assertTrue(output.contains("LOW"));
        }

        @Test
        @DisplayName("Should generate custom threshold reorder report")
        void shouldGenerateCustomThresholdReorderReport() {
            // Given
            List<ReorderRow> customData = List.of(
                new ReorderRow("PROD003", "Custom Threshold", "BATCH003", "WEB", 5,
                    LocalDate.now().plusDays(5), "URGENT")
            );

            when(reportService.reorder(any(ReportFilters.class), eq(25))).thenReturn(customData);

            String input = "2\n25\n0\n"; // Custom threshold 25, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            // When
            reorderScreen.run();

            // Then
            verify(reportService).reorder(any(ReportFilters.class), eq(25));
            String output = outputStream.toString();
            assertTrue(output.contains("Reorder Report (Threshold: 25)"));
            assertTrue(output.contains("URGENT"));
        }

        @Test
        @DisplayName("Should handle empty reorder data")
        void shouldHandleEmptyReorderData() {
            // Given
            when(reportService.reorder(any(ReportFilters.class), anyInt())).thenReturn(List.of());

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            // When
            reorderScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No items need reordering"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid threshold input")
        void shouldHandleInvalidThresholdInput() {
            // Given
            String input = "2\nabc\n30\n0\n"; // Invalid then valid threshold
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            when(reportService.reorder(any(ReportFilters.class), eq(30))).thenReturn(List.of());

            // When
            reorderScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid threshold"));
        }

        @Test
        @DisplayName("Should handle negative threshold")
        void shouldHandleNegativeThreshold() {
            // Given
            String input = "2\n-10\n20\n0\n"; // Negative then valid threshold
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            when(reportService.reorder(any(ReportFilters.class), eq(20))).thenReturn(List.of());

            // When
            reorderScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Threshold must be positive"));
        }

        @Test
        @DisplayName("Should handle invalid menu choices")
        void shouldHandleInvalidMenuChoices() {
            // Given
            String input = "99\nabc\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            // When
            reorderScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid option"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ReorderScreen with required dependencies")
        void shouldCreateReorderScreenWithRequiredDependencies() {
            // Given
            scanner = new Scanner(System.in);

            // When
            ReorderScreen screen = new ReorderScreen(reportService, scanner);

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
            when(reportService.reorder(any(ReportFilters.class), anyInt()))
                .thenThrow(new RuntimeException("Database connection failed"));

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            reorderScreen = new ReorderScreen(reportService, scanner);

            // When
            reorderScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Error generating reorder report") ||
                      output.contains("Database connection failed"));
        }
    }
}
