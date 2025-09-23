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
import application.reports.dto.OrderHeaderRow;
import application.reports.dto.OrderLineRow;
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

@DisplayName("OrderReportScreen Tests")
class OrderReportScreenTest {

    @Mock
    private ReportService reportService;

    private Scanner scanner;
    private OrderReportScreen orderReportScreen;
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
        @DisplayName("Should display order report screen menu")
        void shouldDisplayOrderReportScreenMenu() {
            // Given
            String input = "0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            // When
            orderReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("ORDER REPORT (WEB SALES)"));
            assertTrue(output.contains("1. Today's Orders"));
            assertTrue(output.contains("2. Orders by Date"));
            assertTrue(output.contains("3. Orders by Date Range"));
            assertTrue(output.contains("4. View Order Details"));
            assertTrue(output.contains("0. Back"));
        }

        @Test
        @DisplayName("Should display today's orders")
        void shouldDisplayTodaysOrders() {
            // Given
            List<OrderHeaderRow> todaysOrders = List.of(
                new OrderHeaderRow(1, 201L, 2001L, "WEB", "Online Store",
                    LocalDateTime.now().minusHours(3), new BigDecimal("299.99"), "Card: ****1234"),
                new OrderHeaderRow(2, 202L, 2002L, "MOBILE", "Mobile App",
                    LocalDateTime.now().minusHours(1), new BigDecimal("149.99"), "PayPal")
            );

            when(reportService.orders(any(ReportFilters.class))).thenReturn(todaysOrders);

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            // When
            orderReportScreen.run();

            // Then
            verify(reportService).orders(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.now())
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Today's Orders"));
            assertTrue(output.contains("2001"));
            assertTrue(output.contains("2002"));
            assertTrue(output.contains("WEB"));
            assertTrue(output.contains("MOBILE"));
            assertTrue(output.contains("****1234"));
        }

        @Test
        @DisplayName("Should view order details")
        void shouldViewOrderDetails() {
            // Given
            List<OrderLineRow> orderLines = List.of(
                new OrderLineRow("PROD001", "Gaming Laptop", new BigDecimal("1299.99"), 1, new BigDecimal("1299.99")),
                new OrderLineRow("PROD002", "Gaming Mouse", new BigDecimal("79.99"), 1, new BigDecimal("79.99"))
            );

            when(reportService.orderLines(201L)).thenReturn(orderLines);

            String input = "4\n201\n0\n"; // View details, order ID, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            // When
            orderReportScreen.run();

            // Then
            verify(reportService).orderLines(201L);
            String output = outputStream.toString();
            assertTrue(output.contains("Order Details for ID: 201"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Gaming Laptop"));
            assertTrue(output.contains("1299.99"));
        }

        @Test
        @DisplayName("Should handle empty order data")
        void shouldHandleEmptyOrderData() {
            // Given
            when(reportService.orders(any(ReportFilters.class))).thenReturn(List.of());

            String input = "1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            // When
            orderReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No orders found"));
        }
    }

    @Nested
    @DisplayName("Date Filter Tests")
    class DateFilterTests {

        @Test
        @DisplayName("Should filter orders by specific date")
        void shouldFilterOrdersBySpecificDate() {
            // Given
            when(reportService.orders(any(ReportFilters.class))).thenReturn(List.of());

            String input = "2\n2025-09-20\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            // When
            orderReportScreen.run();

            // Then
            verify(reportService).orders(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY &&
                filters.day().equals(LocalDate.of(2025, 9, 20))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Orders for 2025-09-20"));
        }

        @Test
        @DisplayName("Should filter orders by date range")
        void shouldFilterOrdersByDateRange() {
            // Given
            when(reportService.orders(any(ReportFilters.class))).thenReturn(List.of());

            String input = "3\n2025-09-15\n2025-09-22\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            // When
            orderReportScreen.run();

            // Then
            verify(reportService).orders(argThat(filters ->
                filters.dateMode() == ReportFilters.DateMode.DATE_RANGE &&
                filters.fromDate().equals(LocalDate.of(2025, 9, 15)) &&
                filters.toDate().equals(LocalDate.of(2025, 9, 22))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Orders from 2025-09-15 to 2025-09-22"));
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
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            when(reportService.orders(any(ReportFilters.class))).thenReturn(List.of());

            // When
            orderReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
        }

        @Test
        @DisplayName("Should handle invalid order ID")
        void shouldHandleInvalidOrderId() {
            // Given
            String input = "4\nabc\n201\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            scanner = new Scanner(System.in);
            orderReportScreen = new OrderReportScreen(reportService, scanner);

            when(reportService.orderLines(201L)).thenReturn(List.of());

            // When
            orderReportScreen.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid order ID"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create OrderReportScreen with required dependencies")
        void shouldCreateOrderReportScreenWithRequiredDependencies() {
            // Given
            scanner = new Scanner(System.in);

            // When
            OrderReportScreen screen = new OrderReportScreen(reportService, scanner);

            // Then
            assertNotNull(screen);
        }
    }
}
