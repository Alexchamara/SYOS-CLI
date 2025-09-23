package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.InventoryAdminRepository;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;

@DisplayName("InventoryAdminRepository Domain Interface Tests")
class InventoryAdminRepositoryTest {

    private InventoryAdminRepository inventoryAdminRepository;
    private Connection connection;

    private String productCode;
    private String location;
    private int quantity;

    @BeforeEach
    void setUp() {
        inventoryAdminRepository = mock(InventoryAdminRepository.class);
        connection = mock(Connection.class);
        productCode = "PROD001";
        location = "MAIN_STORE";
        quantity = 50;
    }

    @Test
    @DisplayName("Should define contract for inserting a batch and returning generated id")
    void shouldDefineContractForInsertingBatch() {
        // Given
        LocalDateTime receivedAt = LocalDateTime.now();
        LocalDate expiry = LocalDate.now().plusDays(30);
        when(inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity))
            .thenReturn(123L);

        // When
        long id = inventoryAdminRepository.insertBatch(connection, productCode, location, receivedAt, expiry, quantity);

        // Then
        assertEquals(123L, id);
        verify(inventoryAdminRepository).insertBatch(connection, productCode, location, receivedAt, expiry, quantity);
    }
}
