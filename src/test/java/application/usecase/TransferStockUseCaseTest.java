package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.TransferStockUseCase;
import domain.inventory.StockLocation;
import domain.policies.BatchSelectionStrategy;
import domain.repository.InventoryAdminRepository;
import domain.repository.InventoryRepository;
import domain.shared.Code;
import infrastructure.concurrency.Tx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferStockUseCase Tests")
class TransferStockUseCaseTest {

    @Mock
    private Tx tx;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryAdminRepository inventoryAdminRepository;

    @Mock
    private BatchSelectionStrategy batchSelectionStrategy;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    private TransferStockUseCase transferStockUseCase;

    @BeforeEach
    void setUp() {
        transferStockUseCase = new TransferStockUseCase(
            tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy
        );
    }

    @Nested
    @DisplayName("Transfer Stock Tests")
    class TransferStockTests {

        @Test
        @DisplayName("Should transfer stock successfully with valid parameters")
        void shouldTransferStockSuccessfullyWithValidParameters() throws Exception {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 50;

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, ?> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, from.name())).thenReturn(100);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            // When
            transferStockUseCase.transfer(productCode, from, to, qty);

            // Then
            verify(tx).inTx(any());
            verify(inventoryRepository).totalAvailable(connection, productCode, from.name());
            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, from);
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), eq(to.name()), any(LocalDateTime.class), eq(null), eq(qty)
            );
        }

        @Test
        @DisplayName("Should throw exception when product code is null")
        void shouldThrowExceptionWhenProductCodeIsNull() {
            // Given
            String productCode = null;
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 50;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Product code cannot be empty", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when product code is empty")
        void shouldThrowExceptionWhenProductCodeIsEmpty() {
            // Given
            String productCode = "  ";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 50;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Product code cannot be empty", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 0;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Quantity must be positive, got: 0", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = -10;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Quantity must be positive, got: -10", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when source location is null")
        void shouldThrowExceptionWhenSourceLocationIsNull() {
            // Given
            String productCode = "PROD001";
            StockLocation from = null;
            StockLocation to = StockLocation.SHELF;
            int qty = 50;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Source location cannot be null", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when destination location is null")
        void shouldThrowExceptionWhenDestinationLocationIsNull() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = null;
            int qty = 50;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Destination location cannot be null", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when source and destination are the same")
        void shouldThrowExceptionWhenSourceAndDestinationAreTheSame() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.MAIN_STORE;
            int qty = 50;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Source and destination locations cannot be the same", exception.getMessage());

            verifyNoInteractions(tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy);
        }

        @Test
        @DisplayName("Should throw exception when insufficient stock available")
        void shouldThrowExceptionWhenInsufficientStockAvailable() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 100;

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, ?> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, from.name())).thenReturn(50); // Less than requested

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertTrue(exception.getMessage().contains("Insufficient stock at MAIN_STORE"));
            assertTrue(exception.getMessage().contains("Available: 50, Requested: 100"));

            verify(inventoryRepository).totalAvailable(connection, productCode, from.name());
            verifyNoInteractions(batchSelectionStrategy, inventoryAdminRepository);
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() throws Exception {
            // Given
            String productCode = "PROD001";
            int qty = 30;

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, ?> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(any(Connection.class), anyString(), anyString())).thenReturn(100);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            // When
            transferStockUseCase.transfer(productCode, StockLocation.MAIN_STORE, StockLocation.SHELF, qty);
            transferStockUseCase.transfer(productCode, StockLocation.SHELF, StockLocation.WEB, qty);
            transferStockUseCase.transfer(productCode, StockLocation.WEB, StockLocation.MAIN_STORE, qty);

            // Then
            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, StockLocation.MAIN_STORE);
            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, StockLocation.SHELF);
            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, StockLocation.WEB);

            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), eq(StockLocation.SHELF.name()), any(LocalDateTime.class), eq(null), eq(qty)
            );
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), eq(StockLocation.WEB.name()), any(LocalDateTime.class), eq(null), eq(qty)
            );
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), eq(StockLocation.MAIN_STORE.name()), any(LocalDateTime.class), eq(null), eq(qty)
            );
        }

        @Test
        @DisplayName("Should handle transaction exceptions")
        void shouldHandleTransactionExceptions() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 50;

            when(tx.inTx(any())).thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertEquals("Database connection failed", exception.getMessage());

            verify(tx).inTx(any());
        }

        @Test
        @DisplayName("Should handle batch selection strategy exceptions")
        void shouldHandleBatchSelectionStrategyExceptions() {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 50;

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, ?> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, from.name())).thenReturn(100);
            doThrow(new RuntimeException("Deduction failed")).when(batchSelectionStrategy)
                .deduct(connection, new Code(productCode), qty, from);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transferStockUseCase.transfer(productCode, from, to, qty));
            assertTrue(exception.getMessage().contains("Transfer operation failed"));

            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, from);
            verifyNoInteractions(inventoryAdminRepository);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create TransferStockUseCase with required dependencies")
        void shouldCreateTransferStockUseCaseWithRequiredDependencies() {
            // When
            TransferStockUseCase useCase = new TransferStockUseCase(
                tx, inventoryRepository, inventoryAdminRepository, batchSelectionStrategy
            );

            // Then
            assertNotNull(useCase);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exact available quantity transfer")
        void shouldHandleExactAvailableQuantityTransfer() throws Exception {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = 75; // Exact available amount

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, ?> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, from.name())).thenReturn(75);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            // When
            transferStockUseCase.transfer(productCode, from, to, qty);

            // Then
            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, from);
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), eq(to.name()), any(LocalDateTime.class), eq(null), eq(qty)
            );
        }

        @Test
        @DisplayName("Should handle very large quantities")
        void shouldHandleVeryLargeQuantities() throws Exception {
            // Given
            String productCode = "PROD001";
            StockLocation from = StockLocation.MAIN_STORE;
            StockLocation to = StockLocation.SHELF;
            int qty = Integer.MAX_VALUE;

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, ?> function = invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryRepository.totalAvailable(connection, productCode, from.name())).thenReturn(Integer.MAX_VALUE);
            when(connection.prepareStatement(anyString())).thenAnswer(invocation -> preparedStatement);

            // When
            transferStockUseCase.transfer(productCode, from, to, qty);

            // Then
            verify(batchSelectionStrategy).deduct(connection, new Code(productCode), qty, from);
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), eq(to.name()), any(LocalDateTime.class), eq(null), eq(qty)
            );
        }
    }
}
