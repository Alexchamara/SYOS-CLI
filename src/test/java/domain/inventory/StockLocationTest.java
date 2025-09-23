package domain.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import domain.inventory.StockLocation;

@DisplayName("StockLocation Enum Tests")
class StockLocationTest {

    @Test
    @DisplayName("Should have all expected stock locations")
    void shouldHaveAllExpectedStockLocations() {
        // When & Then
        assertEquals(3, StockLocation.values().length);
        assertNotNull(StockLocation.MAIN_STORE);
        assertNotNull(StockLocation.SHELF);
        assertNotNull(StockLocation.WEB);
    }

    @Test
    @DisplayName("Should convert to string correctly")
    void shouldConvertToStringCorrectly() {
        // Then
        assertEquals("MAIN_STORE", StockLocation.MAIN_STORE.toString());
        assertEquals("SHELF", StockLocation.SHELF.toString());
        assertEquals("WEB", StockLocation.WEB.toString());
    }

    @Test
    @DisplayName("Should support valueOf operations")
    void shouldSupportValueOfOperations() {
        // Then
        assertEquals(StockLocation.MAIN_STORE, StockLocation.valueOf("MAIN_STORE"));
        assertEquals(StockLocation.SHELF, StockLocation.valueOf("SHELF"));
        assertEquals(StockLocation.WEB, StockLocation.valueOf("WEB"));
    }
}
