package domain.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.shared.Code;
import domain.shared.Quantity;
import java.time.LocalDate;
import java.time.LocalDateTime;

@DisplayName("Batch Domain Entity Tests")
class BatchTest {

    private Code productCode;
    private StockLocation stockLocation;
    private Quantity quantity;
    private LocalDate expiryDate;
    private LocalDateTime receivedAt;

    @BeforeEach
    void setUp() {
        productCode = new Code("PROD001");
        stockLocation = StockLocation.MAIN_STORE;
        quantity = new Quantity(100);
        expiryDate = LocalDate.now().plusDays(30);
        receivedAt = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should create Batch with valid parameters")
    void shouldCreateBatchWithValidParameters() {
        // When
        Batch batch = new Batch(1L, productCode, stockLocation, receivedAt, expiryDate, quantity);

        // Then
        assertEquals(1L, batch.id());
        assertEquals(productCode, batch.productCode());
        assertEquals(stockLocation, batch.location());
        assertEquals(receivedAt, batch.receivedAt());
        assertEquals(expiryDate, batch.expiry());
        assertEquals(quantity, batch.quantity());
    }

    @Test
    @DisplayName("Should create multiple batches with different IDs")
    void shouldCreateMultipleBatchesWithDifferentIds() {
        // When
        Batch batch1 = new Batch(1L, productCode, stockLocation, receivedAt, expiryDate, quantity);
        Batch batch2 = new Batch(2L, productCode, stockLocation, receivedAt, expiryDate, quantity);

        // Then
        assertNotEquals(batch1.id(), batch2.id());
        assertEquals(productCode, batch1.productCode());
        assertEquals(productCode, batch2.productCode());
    }

    @Test
    @DisplayName("Should handle different stock locations")
    void shouldHandleDifferentStockLocations() {
        // When
        Batch mainStoreBatch = new Batch(1L, productCode, StockLocation.MAIN_STORE, receivedAt, expiryDate, quantity);
        Batch shelfBatch = new Batch(2L, productCode, StockLocation.SHELF, receivedAt, expiryDate, quantity);
        Batch webBatch = new Batch(3L, productCode, StockLocation.WEB, receivedAt, expiryDate, quantity);

        // Then
        assertEquals(StockLocation.MAIN_STORE, mainStoreBatch.location());
        assertEquals(StockLocation.SHELF, shelfBatch.location());
        assertEquals(StockLocation.WEB, webBatch.location());
    }

    @Test
    @DisplayName("Should handle different quantities")
    void shouldHandleDifferentQuantities() {
        // Given
        Quantity smallQuantity = new Quantity(10);
        Quantity largeQuantity = new Quantity(1000);

        // When
        Batch smallBatch = new Batch(1L, productCode, stockLocation, receivedAt, expiryDate, smallQuantity);
        Batch largeBatch = new Batch(2L, productCode, stockLocation, receivedAt, expiryDate, largeQuantity);

        // Then
        assertEquals(smallQuantity, smallBatch.quantity());
        assertEquals(largeQuantity, largeBatch.quantity());
    }

    @Test
    @DisplayName("Should handle different expiry dates")
    void shouldHandleDifferentExpiryDates() {
        // Given
        LocalDate nearExpiry = LocalDate.now().plusDays(5);
        LocalDate farExpiry = LocalDate.now().plusDays(365);

        // When
        Batch nearExpiryBatch = new Batch(1L, productCode, stockLocation, receivedAt, nearExpiry, quantity);
        Batch farExpiryBatch = new Batch(2L, productCode, stockLocation, receivedAt, farExpiry, quantity);

        // Then
        assertEquals(nearExpiry, nearExpiryBatch.expiry());
        assertEquals(farExpiry, farExpiryBatch.expiry());
    }
}
