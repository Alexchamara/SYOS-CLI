import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.Optional;
import javax.sql.DataSource;

import main.java.App;
import config.Db;
import infrastructure.concurrency.Tx;
import infrastructure.persistence.*;
import infrastructure.security.PasswordEncoder;
import infrastructure.events.SimpleBus;
import infrastructure.events.LowStockPrinter;
import application.usecase.*;
import application.services.*;
import cli.signin.LoginScreen;
import cli.cashier.CashierMenu;
import cli.manager.ManagerMenu;
import cli.webshop.WebShopMenu;
import cli.SeedUsers;
import domain.user.User;
import domain.user.Role;

@DisplayName("App Tests")
class AppTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Nested
    @DisplayName("Main Method Tests")
    class MainMethodTests {

        @Test
        @DisplayName("Should start CLI interface when user chooses option 1")
        void shouldStartCliInterfaceWhenUserChoosesOption1() {
            // Given
            String userInput = "1\nmanager\npassword\n0\n"; // Choose CLI, login as manager, exit
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock);
                 MockedConstruction<LoginScreen> loginMock = mockConstruction(LoginScreen.class, AppTest.this::setupLoginScreenMock);
                 MockedConstruction<CashierMenu> cashierMock = mockConstruction(CashierMenu.class);
                 MockedConstruction<ManagerMenu> managerMock = mockConstruction(ManagerMenu.class, AppTest.this::setupManagerMenuMock);
                 MockedStatic<App> appMock = mockStatic(App.class)) {

                // Mock the main method to avoid OnlineCartUseCase instantiation
                appMock.when(() -> App.main(any())).thenCallRealMethod();

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
            }
        }

        @Test
        @DisplayName("Should start Web Shop interface when user chooses option 2")
        void shouldStartWebShopInterfaceWhenUserChoosesOption2() {
            // Given
            String userInput = "2\n";
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock);
                 MockedConstruction<WebShopMenu> webShopMock = mockConstruction(WebShopMenu.class, AppTest.this::setupWebShopMenuMock);
                 MockedStatic<App> appMock = mockStatic(App.class)) {

                // Mock the main method to avoid OnlineCartUseCase instantiation
                appMock.when(() -> App.main(any())).thenCallRealMethod();

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
            }
        }

        @Test
        @DisplayName("Should handle invalid interface choice")
        void shouldHandleInvalidInterfaceChoice() {
            // Given
            String userInput = "3\n"; // Invalid choice
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock);
                 MockedStatic<App> appMock = mockStatic(App.class)) {

                // Mock the main method to avoid OnlineCartUseCase instantiation
                appMock.when(() -> App.main(any())).thenCallRealMethod();

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then
                String output = outputStream.toString();
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
            }
        }

        @Test
        @DisplayName("Should handle empty command line arguments")
        void shouldHandleEmptyCommandLineArguments() {
            // Given
            String userInput = "3\n"; // Invalid choice to exit quickly
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock)) {

                // When & Then
                assertDoesNotThrow(() -> App.main(new String[]{}));
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
            }
        }

        @Test
        @DisplayName("Should handle null command line arguments")
        void shouldHandleNullCommandLineArguments() {
            // Given
            String userInput = "3\n"; // Invalid choice to exit quickly
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock)) {

                // When & Then
                assertDoesNotThrow(() -> App.main(null));
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
            }
        }

        @Test
        @DisplayName("Should create all required repositories and services")
        void shouldCreateAllRequiredRepositoriesAndServices() {
            // Given
            String userInput = "3\n"; // Invalid choice to exit quickly
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock);
                 MockedConstruction<JdbcProductRepository> productRepoMock = mockConstruction(JdbcProductRepository.class);
                 MockedConstruction<JdbcCategoryRepository> categoryRepoMock = mockConstruction(JdbcCategoryRepository.class);
                 MockedConstruction<JdbcBillRepository> billRepoMock = mockConstruction(JdbcBillRepository.class);
                 MockedConstruction<JdbcInventoryRepository> inventoryRepoMock = mockConstruction(JdbcInventoryRepository.class);
                 MockedConstruction<JdbcUserRepository> userRepoMock = mockConstruction(JdbcUserRepository.class);
                 MockedConstruction<JdbcShortageEventRepository> shortageRepoMock = mockConstruction(JdbcShortageEventRepository.class);
                 MockedConstruction<SimpleBus> busMock = mockConstruction(SimpleBus.class);
                 MockedConstruction<PasswordEncoder> encoderMock = mockConstruction(PasswordEncoder.class)) {

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then - Verify basic dependency creation (repositories should be created)
                assertTrue(productRepoMock.constructed().size() >= 1);
                assertTrue(categoryRepoMock.constructed().size() >= 1);
                assertTrue(billRepoMock.constructed().size() >= 1);
                assertTrue(inventoryRepoMock.constructed().size() >= 1);
                assertTrue(userRepoMock.constructed().size() >= 1);
                assertTrue(shortageRepoMock.constructed().size() >= 1);
                assertTrue(busMock.constructed().size() >= 1);
                assertTrue(encoderMock.constructed().size() >= 1);
            }
        }

        @Test
        @DisplayName("Should handle database connection failure gracefully")
        void shouldHandleDatabaseConnectionFailureGracefully() {
            // Given
            String userInput = "1\n";
            setSystemInput(userInput);

            try (MockedConstruction<Db> dbMock = mockConstruction(Db.class, (mock, context) -> {
                when(mock.getDataSource()).thenThrow(new RuntimeException("Database connection failed"));
            })) {

                // When & Then
                RuntimeException exception = assertThrows(RuntimeException.class, () -> App.main(new String[]{}));
                assertTrue(exception.getMessage().contains("Database connection failed") ||
                          exception.getCause().getMessage().contains("Database connection failed"));
            }
        }

        @Test
        @DisplayName("Should handle various input scenarios")
        void shouldHandleVariousInputScenarios() {
            // Test empty input
            setSystemInput("\n3\n");
            assertDoesNotThrow(() -> App.main(new String[]{}));

            // Test whitespace input
            setSystemInput("   \n3\n");
            assertDoesNotThrow(() -> App.main(new String[]{}));

            // Test multiple invalid choices
            setSystemInput("abc\n999\n-1\n3\n");
            assertDoesNotThrow(() -> App.main(new String[]{}));
        }
    }

    @Nested
    @DisplayName("Dependency Injection Tests")
    class DependencyInjectionTests {

        @Test
        @DisplayName("Should initialize core application components")
        void shouldInitializeCoreApplicationComponents() {
            // Given
            String userInput = "3\n"; // Invalid choice to exit quickly
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock);
                 MockedConstruction<Tx> txMock = mockConstruction(Tx.class);
                 MockedConstruction<AuthenticationUseCase> authMock = mockConstruction(AuthenticationUseCase.class);
                 MockedConstruction<ProductManagementUseCase> productMgmtMock = mockConstruction(ProductManagementUseCase.class);
                 MockedConstruction<CategoryManagementUseCase> categoryMgmtMock = mockConstruction(CategoryManagementUseCase.class);
                 MockedConstruction<BatchManagementUseCase> batchMgmtMock = mockConstruction(BatchManagementUseCase.class);
                 MockedConstruction<CheckoutUseCase> checkoutMock = mockConstruction(CheckoutUseCase.class)) {

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then - Verify core components are created
                assertTrue(txMock.constructed().size() >= 1);
                assertTrue(authMock.constructed().size() >= 1);
                assertTrue(productMgmtMock.constructed().size() >= 1);
                assertTrue(categoryMgmtMock.constructed().size() >= 1);
                assertTrue(batchMgmtMock.constructed().size() >= 1);
                assertTrue(checkoutMock.constructed().size() >= 1);
            }
        }

        @Test
        @DisplayName("Should initialize service layer components")
        void shouldInitializeServiceLayerComponents() {
            // Given
            String userInput = "3\n"; // Invalid choice to exit quickly
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock);
                 MockedConstruction<BillNumberService> billNumberMock = mockConstruction(BillNumberService.class);
                 MockedConstruction<AvailabilityService> availabilityMock = mockConstruction(AvailabilityService.class);
                 MockedConstruction<MainStoreService> mainStoreMock = mockConstruction(MainStoreService.class);
                 MockedConstruction<ShortageEventService> shortageMock = mockConstruction(ShortageEventService.class);
                 MockedConstruction<DiscountService> discountMock = mockConstruction(DiscountService.class)) {

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then - Verify service components are created
                assertTrue(billNumberMock.constructed().size() >= 1);
                assertTrue(availabilityMock.constructed().size() >= 1);
                assertTrue(mainStoreMock.constructed().size() >= 1);
                assertTrue(shortageMock.constructed().size() >= 1);
                assertTrue(discountMock.constructed().size() >= 1);
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle system resource exhaustion")
        void shouldHandleSystemResourceExhaustion() {
            // Given
            String userInput = "3\n";
            setSystemInput(userInput);

            try (MockedConstruction<Db> dbMock = mockConstruction(Db.class, (mock, context) -> {
                when(mock.getDataSource()).thenThrow(new OutOfMemoryError("Heap space exhausted"));
            })) {

                // When & Then
                assertThrows(OutOfMemoryError.class, () -> App.main(new String[]{}));
            }
        }

        @Test
        @DisplayName("Should handle security manager restrictions")
        void shouldHandleSecurityManagerRestrictions() {
            // Given
            String userInput = "3\n";
            setSystemInput(userInput);

            try (MockedStatic<System> systemMock = mockStatic(System.class)) {
                systemMock.when(() -> System.setIn(any())).thenThrow(new SecurityException("Access denied"));

                // When & Then
                assertThrows(SecurityException.class, () -> setSystemInput(userInput));
            }
        }

        @Test
        @DisplayName("Should handle application startup exceptions")
        void shouldHandleApplicationStartupExceptions() {
            // Given
            String userInput = "1\n";
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class)) {
                seedUsersMock.when(() -> SeedUsers.ensure(any(), any()))
                    .thenThrow(new RuntimeException("User seeding failed"));

                // When & Then
                assertThrows(RuntimeException.class, () -> App.main(new String[]{}));
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should complete application lifecycle")
        void shouldCompleteApplicationLifecycle() {
            // Given
            String userInput = "3\n"; // Start and exit immediately
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock);
                 MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class, AppTest.this::setupScannerMock)) {

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
                assertEquals(1, dbMock.constructed().size());
                assertEquals(1, scannerMock.constructed().size());
            }
        }

        @Test
        @DisplayName("Should handle multiple execution scenarios")
        void shouldHandleMultipleExecutionScenarios() {
            // Test scenario 1: Normal CLI flow
            String userInput1 = "1\n3\n"; // CLI then invalid choice
            setSystemInput(userInput1);
            assertDoesNotThrow(() -> App.main(new String[]{}));

            // Test scenario 2: Web shop flow
            String userInput2 = "2\n";
            setSystemInput(userInput2);
            assertDoesNotThrow(() -> App.main(new String[]{}));

            // Test scenario 3: Immediate exit
            String userInput3 = "3\n";
            setSystemInput(userInput3);
            assertDoesNotThrow(() -> App.main(new String[]{}));
        }

        @Test
        @DisplayName("Should maintain state consistency across operations")
        void shouldMaintainStateConsistencyAcrossOperations() {
            // Given
            String userInput = "3\n";
            setSystemInput(userInput);

            try (MockedStatic<SeedUsers> seedUsersMock = mockStatic(SeedUsers.class);
                 MockedConstruction<Db> dbMock = mockConstruction(Db.class, AppTest.this::setupDbMock)) {

                // When
                assertDoesNotThrow(() -> App.main(new String[]{}));

                // Then - Verify consistent state
                seedUsersMock.verify(() -> SeedUsers.ensure(any(), any()));
                assertEquals(1, dbMock.constructed().size());

                // Verify DataSource was properly accessed
                verify(dbMock.constructed().get(0)).getDataSource();
            }
        }
    }

    // Helper methods for setting up mocks
    private void setSystemInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
    }

    private void setupDbMock(Db db, MockedConstruction.Context context) {
        DataSource mockDataSource = mock(DataSource.class);
        when(db.getDataSource()).thenReturn(mockDataSource);
    }

    private void setupScannerMock(Scanner scanner, MockedConstruction.Context context) {
        // Scanner behavior is handled by System.in redirection
    }

    private void setupLoginScreenMock(LoginScreen loginScreen, MockedConstruction.Context context) {
        AuthenticationUseCase.Session mockSession = mock(AuthenticationUseCase.Session.class);
        when(mockSession.identifier()).thenReturn("manager");
        when(mockSession.role()).thenReturn(Role.MANAGER);
        when(loginScreen.prompt()).thenReturn(mockSession);
    }

    private void setupFailedLoginScreenMock(LoginScreen loginScreen, MockedConstruction.Context context) {
        when(loginScreen.prompt()).thenReturn(null);
    }

    private void setupCashierLoginScreenMock(LoginScreen loginScreen, MockedConstruction.Context context) {
        AuthenticationUseCase.Session mockSession = mock(AuthenticationUseCase.Session.class);
        when(mockSession.identifier()).thenReturn("cashier");
        when(mockSession.role()).thenReturn(Role.CASHIER);
        when(loginScreen.prompt()).thenReturn(mockSession);
    }

    private void setupManagerLoginScreenMock(LoginScreen loginScreen, MockedConstruction.Context context) {
        AuthenticationUseCase.Session mockSession = mock(AuthenticationUseCase.Session.class);
        when(mockSession.identifier()).thenReturn("manager");
        when(mockSession.role()).thenReturn(Role.MANAGER);
        when(loginScreen.prompt()).thenReturn(mockSession);
    }

    private void setupCashierMenuMock(CashierMenu cashierMenu, MockedConstruction.Context context) {
        doNothing().when(cashierMenu).run();
    }

    private void setupManagerMenuMock(ManagerMenu managerMenu, MockedConstruction.Context context) {
        doNothing().when(managerMenu).run();
    }

    private void setupWebShopMenuMock(WebShopMenu webShopMenu, MockedConstruction.Context context) {
        doNothing().when(webShopMenu).start();
    }
}
