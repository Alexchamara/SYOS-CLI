package application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.AvailabilityService;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import infrastructure.concurrency.Tx;

import java.sql.Connection;
import java.util.Map;
import java.util.function.Function;

@DisplayName("AvailabilityService Tests")
class AvailabilityServiceTest {

    @Mock
    private Tx tx;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private Connection connection;

    private AvailabilityService availabilityService;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        availabilityService = new AvailabilityService(tx, inventoryRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Available Quantity Tests")
    class AvailableQuantityTests {

        @Test
        @DisplayName("Should return available quantity for product at specific location")
        void shouldReturnAvailableQuantityForProductAtSpecificLocation() {
            // Given
            String productCode = "PROD001";
            StockLocation location = StockLocation.MAIN_STORE;
            int expectedQuantity = 150;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Integer> function = (Function<Connection, Integer>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, location.name())).thenReturn(expectedQuantity);

            // When
            int result = availabilityService.available(productCode, location);

            // Then
            assertEquals(expectedQuantity, result);
            verify(tx).inTx(any());
            verify(inventoryRepository).totalAvailable(connection, productCode, location.name());
        }

        @Test
        @DisplayName("Should return zero when no stock available")
        void shouldReturnZeroWhenNoStockAvailable() {
            // Given
            String productCode = "PROD002";
            StockLocation location = StockLocation.SHELF;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Integer> function = (Function<Connection, Integer>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, location.name())).thenReturn(0);

            // When
            int result = availabilityService.available(productCode, location);

            // Then
            assertEquals(0, result);
            verify(inventoryRepository).totalAvailable(connection, productCode, location.name());
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() {
            // Given
            String productCode = "PROD001";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Integer> function = (Function<Connection, Integer>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.MAIN_STORE.name())).thenReturn(100);
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.SHELF.name())).thenReturn(50);
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.WEB.name())).thenReturn(25);

            // When
            int mainStoreResult = availabilityService.available(productCode, StockLocation.MAIN_STORE);
            int shelfResult = availabilityService.available(productCode, StockLocation.SHELF);
            int webResult = availabilityService.available(productCode, StockLocation.WEB);

            // Then
            assertEquals(100, mainStoreResult);
            assertEquals(50, shelfResult);
            assertEquals(25, webResult);
        }

        @Test
        @DisplayName("Should handle transaction exceptions")
        void shouldHandleTransactionExceptions() {
            // Given
            String productCode = "PROD001";
            StockLocation location = StockLocation.MAIN_STORE;

            when(tx.inTx(any())).thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> availabilityService.available(productCode, location));
            assertEquals("Database connection failed", exception.getMessage());

            verify(tx).inTx(any());
        }
    }

    @Nested
    @DisplayName("Transfer Stock Tests")
    class TransferStockTests {

        @Test
        @DisplayName("Should transfer stock between locations successfully")
        void shouldTransferStockBetweenLocationsSuccessfully() {
            // Given
            String productCode = "PROD001";
            StockLocation fromLocation = StockLocation.MAIN_STORE;
            StockLocation toLocation = StockLocation.SHELF;
            int quantity = 50;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            availabilityService.transferStock(productCode, fromLocation, toLocation, quantity);

            // Then
            verify(tx).inTx(any());
            verify(inventoryRepository).transferStock(connection, productCode, fromLocation, toLocation, quantity);
        }

        @Test
        @DisplayName("Should handle different transfer scenarios")
        void shouldHandleDifferentTransferScenarios() {
            // Given
            String productCode = "PROD002";
            int quantity = 30;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            availabilityService.transferStock(productCode, StockLocation.MAIN_STORE, StockLocation.WEB, quantity);
            availabilityService.transferStock(productCode, StockLocation.SHELF, StockLocation.MAIN_STORE, quantity);
            availabilityService.transferStock(productCode, StockLocation.WEB, StockLocation.SHELF, quantity);

            // Then
            verify(inventoryRepository).transferStock(connection, productCode, StockLocation.MAIN_STORE, StockLocation.WEB, quantity);
            verify(inventoryRepository).transferStock(connection, productCode, StockLocation.SHELF, StockLocation.MAIN_STORE, quantity);
            verify(inventoryRepository).transferStock(connection, productCode, StockLocation.WEB, StockLocation.SHELF, quantity);
        }

        @Test
        @DisplayName("Should handle zero quantity transfer")
        void shouldHandleZeroQuantityTransfer() {
            // Given
            String productCode = "PROD001";
            StockLocation fromLocation = StockLocation.MAIN_STORE;
            StockLocation toLocation = StockLocation.SHELF;
            int quantity = 0;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Object> function = (Function<Connection, Object>) invocation.getArgument(0);
                return function.apply(connection);
            });

            // When
            availabilityService.transferStock(productCode, fromLocation, toLocation, quantity);

            // Then
            verify(inventoryRepository).transferStock(connection, productCode, fromLocation, toLocation, quantity);
        }

        @Test
        @DisplayName("Should handle transfer exceptions")
        void shouldHandleTransferExceptions() {
            // Given
            String productCode = "PROD001";
            StockLocation fromLocation = StockLocation.MAIN_STORE;
            StockLocation toLocation = StockLocation.SHELF;
            int quantity = 50;

            when(tx.inTx(any())).thenThrow(new RuntimeException("Transfer failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> availabilityService.transferStock(productCode, fromLocation, toLocation, quantity));
            assertEquals("Transfer failed", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Availability Across All Locations Tests")
    class GetAvailabilityAcrossAllLocationsTests {

        @Test
        @DisplayName("Should return availability across all locations")
        void shouldReturnAvailabilityAcrossAllLocations() {
            // Given
            String productCode = "PROD001";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Map<StockLocation, Integer>> function = (Function<Connection, Map<StockLocation, Integer>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.MAIN_STORE.name())).thenReturn(100);
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.SHELF.name())).thenReturn(50);
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.WEB.name())).thenReturn(25);

            // When
            Map<StockLocation, Integer> result = availabilityService.getAvailabilityAcrossAllLocations(productCode);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(100, result.get(StockLocation.MAIN_STORE));
            assertEquals(50, result.get(StockLocation.SHELF));
            assertEquals(25, result.get(StockLocation.WEB));

            verify(tx).inTx(any());
            verify(inventoryRepository).totalAvailable(connection, productCode, StockLocation.MAIN_STORE.name());
            verify(inventoryRepository).totalAvailable(connection, productCode, StockLocation.SHELF.name());
            verify(inventoryRepository).totalAvailable(connection, productCode, StockLocation.WEB.name());
        }

        @Test
        @DisplayName("Should handle zero availability across all locations")
        void shouldHandleZeroAvailabilityAcrossAllLocations() {
            // Given
            String productCode = "PROD002";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Map<StockLocation, Integer>> function = (Function<Connection, Map<StockLocation, Integer>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, any())).thenReturn(0);

            // When
            Map<StockLocation, Integer> result = availabilityService.getAvailabilityAcrossAllLocations(productCode);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            result.values().forEach(quantity -> assertEquals(0, quantity));
        }

        @Test
        @DisplayName("Should handle mixed availability across locations")
        void shouldHandleMixedAvailabilityAcrossLocations() {
            // Given
            String productCode = "PROD003";

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Map<StockLocation, Integer>> function = (Function<Connection, Map<StockLocation, Integer>>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.MAIN_STORE.name())).thenReturn(200);
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.SHELF.name())).thenReturn(0);
            when(inventoryRepository.totalAvailable(connection, productCode, StockLocation.WEB.name())).thenReturn(75);

            // When
            Map<StockLocation, Integer> result = availabilityService.getAvailabilityAcrossAllLocations(productCode);

            // Then
            assertEquals(200, result.get(StockLocation.MAIN_STORE));
            assertEquals(0, result.get(StockLocation.SHELF));
            assertEquals(75, result.get(StockLocation.WEB));
        }

        @Test
        @DisplayName("Should handle exceptions during availability check")
        void shouldHandleExceptionsDuringAvailabilityCheck() {
            // Given
            String productCode = "PROD001";

            when(tx.inTx(any())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> availabilityService.getAvailabilityAcrossAllLocations(productCode));
            assertEquals("Database error", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create AvailabilityService with required dependencies")
        void shouldCreateAvailabilityServiceWithRequiredDependencies() {
            // When
            AvailabilityService service = new AvailabilityService(tx, inventoryRepository);

            // Then
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large quantities")
        void shouldHandleVeryLargeQuantities() {
            // Given
            String productCode = "PROD001";
            StockLocation location = StockLocation.MAIN_STORE;
            int largeQuantity = Integer.MAX_VALUE;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Integer> function = (Function<Connection, Integer>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, location.name())).thenReturn(largeQuantity);

            // When
            int result = availabilityService.available(productCode, location);

            // Then
            assertEquals(largeQuantity, result);
        }

        @Test
        @DisplayName("Should handle empty product codes")
        void shouldHandleEmptyProductCodes() {
            // Given
            String emptyProductCode = "";
            StockLocation location = StockLocation.MAIN_STORE;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Integer> function = (Function<Connection, Integer>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, emptyProductCode, location.name())).thenReturn(0);

            // When
            int result = availabilityService.available(emptyProductCode, location);

            // Then
            assertEquals(0, result);
            verify(inventoryRepository).totalAvailable(connection, emptyProductCode, location.name());
        }
    }
}
