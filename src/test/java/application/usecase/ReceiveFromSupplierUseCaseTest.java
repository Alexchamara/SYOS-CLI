package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.ReceiveFromSupplierUseCase;
import domain.inventory.StockLocation;
import domain.repository.InventoryAdminRepository;
import infrastructure.concurrency.Tx;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

@DisplayName("ReceiveFromSupplierUseCase Tests")
class ReceiveFromSupplierUseCaseTest {

    @Mock
    private Tx tx;

    @Mock
    private InventoryAdminRepository inventoryAdminRepository;

    @Mock
    private Connection connection;

    private ReceiveFromSupplierUseCase receiveFromSupplierUseCase;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        receiveFromSupplierUseCase = new ReceiveFromSupplierUseCase(tx, inventoryAdminRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Receive Batch Tests")
    class ReceiveBatchTests {

        @Test
        @DisplayName("Should receive batch successfully with valid parameters")
        void shouldReceiveBatchSuccessfullyWithValidParameters() {
            // Given
            String productCode = "PROD001";
            int qty = 100;
            LocalDate expiry = LocalDate.now().plusDays(30);
            long expectedBatchId = 123L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                eq(connection),
                eq(productCode),
                eq(StockLocation.MAIN_STORE.name()),
                any(LocalDateTime.class),
                eq(expiry),
                eq(qty)
            )).thenReturn(expectedBatchId);

            // When
            long result = receiveFromSupplierUseCase.receive(productCode, qty, expiry);

            // Then
            assertEquals(expectedBatchId, result);
            verify(tx).inTx(any());
            verify(inventoryAdminRepository).insertBatch(
                eq(connection),
                eq(productCode),
                eq(StockLocation.MAIN_STORE.name()),
                any(LocalDateTime.class),
                eq(expiry),
                eq(qty)
            );
        }

        @Test
        @DisplayName("Should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            // Given
            String productCode = "PROD001";
            int qty = 0;
            LocalDate expiry = LocalDate.now().plusDays(30);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> receiveFromSupplierUseCase.receive(productCode, qty, expiry));
            assertEquals("qty must be > 0", exception.getMessage());

            verifyNoInteractions(tx, inventoryAdminRepository);
        }

        @Test
        @DisplayName("Should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            // Given
            String productCode = "PROD001";
            int qty = -10;
            LocalDate expiry = LocalDate.now().plusDays(30);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> receiveFromSupplierUseCase.receive(productCode, qty, expiry));
            assertEquals("qty must be > 0", exception.getMessage());

            verifyNoInteractions(tx, inventoryAdminRepository);
        }

        @Test
        @DisplayName("Should handle different product codes")
        void shouldHandleDifferentProductCodes() {
            // Given
            String productCode1 = "PROD001";
            String productCode2 = "PROD002";
            int qty = 50;
            LocalDate expiry = LocalDate.now().plusDays(30);
            long batchId1 = 123L;
            long batchId2 = 124L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                eq(connection), eq(productCode1), anyString(), any(LocalDateTime.class), eq(expiry), eq(qty)
            )).thenReturn(batchId1);
            when(inventoryAdminRepository.insertBatch(
                eq(connection), eq(productCode2), anyString(), any(LocalDateTime.class), eq(expiry), eq(qty)
            )).thenReturn(batchId2);

            // When
            long result1 = receiveFromSupplierUseCase.receive(productCode1, qty, expiry);
            long result2 = receiveFromSupplierUseCase.receive(productCode2, qty, expiry);

            // Then
            assertEquals(batchId1, result1);
            assertEquals(batchId2, result2);
            verify(tx, times(2)).inTx(any());
        }

        @Test
        @DisplayName("Should handle different quantities")
        void shouldHandleDifferentQuantities() {
            // Given
            String productCode = "PROD001";
            int qty1 = 50;
            int qty2 = 200;
            LocalDate expiry = LocalDate.now().plusDays(30);
            long batchId1 = 123L;
            long batchId2 = 124L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                eq(connection), eq(productCode), anyString(), any(LocalDateTime.class), eq(expiry), eq(qty1)
            )).thenReturn(batchId1);
            when(inventoryAdminRepository.insertBatch(
                eq(connection), eq(productCode), anyString(), any(LocalDateTime.class), eq(expiry), eq(qty2)
            )).thenReturn(batchId2);

            // When
            long result1 = receiveFromSupplierUseCase.receive(productCode, qty1, expiry);
            long result2 = receiveFromSupplierUseCase.receive(productCode, qty2, expiry);

            // Then
            assertEquals(batchId1, result1);
            assertEquals(batchId2, result2);
        }

        @Test
        @DisplayName("Should handle different expiry dates")
        void shouldHandleDifferentExpiryDates() {
            // Given
            String productCode = "PROD001";
            int qty = 100;
            LocalDate expiry1 = LocalDate.now().plusDays(30);
            LocalDate expiry2 = LocalDate.now().plusDays(60);
            long batchId1 = 123L;
            long batchId2 = 124L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                eq(connection), eq(productCode), anyString(), any(LocalDateTime.class), eq(expiry1), eq(qty)
            )).thenReturn(batchId1);
            when(inventoryAdminRepository.insertBatch(
                eq(connection), eq(productCode), anyString(), any(LocalDateTime.class), eq(expiry2), eq(qty)
            )).thenReturn(batchId2);

            // When
            long result1 = receiveFromSupplierUseCase.receive(productCode, qty, expiry1);
            long result2 = receiveFromSupplierUseCase.receive(productCode, qty, expiry2);

            // Then
            assertEquals(batchId1, result1);
            assertEquals(batchId2, result2);
        }

        @Test
        @DisplayName("Should always insert into MAIN_STORE location")
        void shouldAlwaysInsertIntoMainStoreLocation() {
            // Given
            String productCode = "PROD001";
            int qty = 100;
            LocalDate expiry = LocalDate.now().plusDays(30);
            long expectedBatchId = 123L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                any(Connection.class), anyString(), anyString(), any(LocalDateTime.class), any(LocalDate.class), anyInt()
            )).thenReturn(expectedBatchId);

            // When
            receiveFromSupplierUseCase.receive(productCode, qty, expiry);

            // Then
            verify(inventoryAdminRepository).insertBatch(
                eq(connection),
                eq(productCode),
                eq(StockLocation.MAIN_STORE.name()), // Should always be MAIN_STORE
                any(LocalDateTime.class),
                eq(expiry),
                eq(qty)
            );
        }

        @Test
        @DisplayName("Should handle transaction exceptions")
        void shouldHandleTransactionExceptions() {
            // Given
            String productCode = "PROD001";
            int qty = 100;
            LocalDate expiry = LocalDate.now().plusDays(30);

            when(tx.inTx(any())).thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> receiveFromSupplierUseCase.receive(productCode, qty, expiry));
            assertEquals("Database connection failed", exception.getMessage());

            verify(tx).inTx(any());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ReceiveFromSupplierUseCase with required dependencies")
        void shouldCreateReceiveFromSupplierUseCaseWithRequiredDependencies() {
            // When
            ReceiveFromSupplierUseCase useCase = new ReceiveFromSupplierUseCase(tx, inventoryAdminRepository);

            // Then
            assertNotNull(useCase);
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
            int qty = Integer.MAX_VALUE;
            LocalDate expiry = LocalDate.now().plusDays(30);
            long expectedBatchId = 123L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                any(Connection.class), anyString(), anyString(), any(LocalDateTime.class), any(LocalDate.class), anyInt()
            )).thenReturn(expectedBatchId);

            // When
            long result = receiveFromSupplierUseCase.receive(productCode, qty, expiry);

            // Then
            assertEquals(expectedBatchId, result);
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), anyString(), any(LocalDateTime.class), eq(expiry), eq(qty)
            );
        }

        @Test
        @DisplayName("Should handle past expiry dates")
        void shouldHandlePastExpiryDates() {
            // Given
            String productCode = "PROD001";
            int qty = 100;
            LocalDate pastExpiry = LocalDate.now().minusDays(10);
            long expectedBatchId = 123L;

            when(tx.inTx(any(Function.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Function<Connection, Long> function = (Function<Connection, Long>) invocation.getArgument(0);
                return function.apply(connection);
            });
            when(inventoryAdminRepository.insertBatch(
                any(Connection.class), anyString(), anyString(), any(LocalDateTime.class), any(LocalDate.class), anyInt()
            )).thenReturn(expectedBatchId);

            // When
            long result = receiveFromSupplierUseCase.receive(productCode, qty, pastExpiry);

            // Then
            assertEquals(expectedBatchId, result);
            verify(inventoryAdminRepository).insertBatch(
                eq(connection), eq(productCode), anyString(), any(LocalDateTime.class), eq(pastExpiry), eq(qty)
            );
        }
    }
}
