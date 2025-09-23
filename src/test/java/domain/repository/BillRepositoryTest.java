package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.BillRepository;
import domain.billing.Bill;
import java.sql.Connection;

@DisplayName("BillRepository Domain Interface Tests")
class BillRepositoryTest {

    private BillRepository billRepository;
    private Connection connection;
    private Bill bill;

    @BeforeEach
    void setUp() {
        billRepository = mock(BillRepository.class);
        connection = mock(Connection.class);
        bill = mock(Bill.class);
    }

    @Test
    @DisplayName("Should define contract for saving bills and returning generated id")
    void shouldDefineContractForSavingBills() {
        // Given
        when(billRepository.save(connection, bill)).thenReturn(123L);

        // When
        long generatedId = billRepository.save(connection, bill);

        // Then
        assertEquals(123L, generatedId);
        verify(billRepository).save(connection, bill);
    }

    @Test
    @DisplayName("Should allow multiple saves returning different ids")
    void shouldAllowMultipleSavesReturningDifferentIds() {
        // Given
        when(billRepository.save(connection, bill)).thenReturn(1L, 2L);

        // When
        long first = billRepository.save(connection, bill);
        long second = billRepository.save(connection, bill);

        // Then
        assertEquals(1L, first);
        assertEquals(2L, second);
        verify(billRepository, times(2)).save(connection, bill);
    }
}
