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

import cli.manager.ManagerMenu;
import application.services.ShortageEventService;
import application.usecase.DiscountManagementUseCase;
import cli.manager.product.ProductManagementCLI;
import cli.manager.batch.BatchManagementCLI;
import cli.manager.category.CategoryManagementCLI;
import domain.user.User;
import domain.user.Role;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

@DisplayName("ManagerMenu Tests")
class ManagerMenuTest {

    @Mock private DataSource dataSource;
    @Mock private Runnable checkout;
    @Mock private ShortageEventService shortageEventService;
    @Mock private Runnable receiveToMain;
    @Mock private Runnable transferFromMain;
    @Mock private ProductManagementCLI productManagementCLI;
    @Mock private BatchManagementCLI batchManagementCLI;
    @Mock private CategoryManagementCLI categoryManagementCLI;
    @Mock private DiscountManagementUseCase discountManagementUseCase;

    private User managerUser;
    private ManagerMenu managerMenu;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        managerUser = new User(1L, "manager", "hash", "manager@syos.com", "Manager User", Role.MANAGER);
        managerMenu = new ManagerMenu(dataSource, checkout, shortageEventService,
            receiveToMain, transferFromMain, productManagementCLI, batchManagementCLI,
            categoryManagementCLI, managerUser, discountManagementUseCase);

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
        @DisplayName("Should display manager menu with all options")
        void shouldDisplayManagerMenuWithAllOptions() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("[MANAGER]"));
            assertTrue(output.contains("1) Manager Console"));
            assertTrue(output.contains("2) Checkout"));
            assertTrue(output.contains("3) Reorder <50"));
            assertTrue(output.contains("4) Show Shortages"));
            assertTrue(output.contains("5) Transfer Batch MAIN->SHELF/WEB"));
            assertTrue(output.contains("6) Product Management"));
            assertTrue(output.contains("7) Batch Management"));
            assertTrue(output.contains("8) Category Management"));
            assertTrue(output.contains("9) Discount Management"));
            assertTrue(output.contains("0) Logout"));
        }

        @Test
        @DisplayName("Should handle manager console navigation")
        void shouldHandleManagerConsoleNavigation() {
            // Given
            String input = "1\n0\n0\n"; // Go to console, exit console, exit menu
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("[MANAGER]"));
        }

        @Test
        @DisplayName("Should handle checkout navigation")
        void shouldHandleCheckoutNavigation() {
            // Given
            String input = "2\n0\n"; // Go to checkout, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            verify(checkout).run();
        }

        @Test
        @DisplayName("Should handle shortages display")
        void shouldHandleShortagesDisplay() {
            // Given
            List<String> shortages = List.of("PROD001 - Low stock: 5 remaining", "PROD002 - Critical: 2 remaining");
            when(shortageEventService.list()).thenReturn(shortages);
            String input = "4\n0\n"; // Show shortages, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            verify(shortageEventService).list();
            String output = outputStream.toString();
            assertTrue(output.contains("PROD001") || output.contains("shortage"));
        }

        @Test
        @DisplayName("Should handle product management navigation")
        void shouldHandleProductManagementNavigation() {
            // Given
            String input = "6\n0\n"; // Product management, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            verify(productManagementCLI).run();
        }

        @Test
        @DisplayName("Should handle batch management navigation")
        void shouldHandleBatchManagementNavigation() {
            // Given
            String input = "7\n0\n"; // Batch management, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            verify(batchManagementCLI).run();
        }

        @Test
        @DisplayName("Should handle category management navigation")
        void shouldHandleCategoryManagementNavigation() {
            // Given
            String input = "8\n0\n"; // Category management, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            verify(categoryManagementCLI).run();
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid menu choices")
        void shouldHandleInvalidMenuChoices() {
            // Given
            String input = "10\nabc\n-1\n0\n"; // Invalid choices, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("[MANAGER]"));
        }

        @Test
        @DisplayName("Should handle empty input")
        void shouldHandleEmptyInput() {
            // Given
            String input = "\n\n0\n"; // Empty inputs, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            managerMenu.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("[MANAGER]"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ManagerMenu with all dependencies")
        void shouldCreateManagerMenuWithAllDependencies() {
            // When
            ManagerMenu menu = new ManagerMenu(dataSource, checkout, shortageEventService,
                receiveToMain, transferFromMain, productManagementCLI, batchManagementCLI,
                categoryManagementCLI, managerUser, discountManagementUseCase);

            // Then
            assertNotNull(menu);
        }
    }
}
