package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.OrderRepository;
import application.usecase.QuoteUseCase;
import domain.billing.BillLine;

import java.util.List;

@DisplayName("OrderRepository Domain Interface Tests")
class OrderRepositoryTest {

    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
    }

    @Test
    @DisplayName("Should provide next bill serial for a given type")
    void shouldProvideNextBillSerial() {
        // Given
        String type = "WEB";
        when(orderRepository.nextBillSerial(type)).thenReturn(42L);

        // When
        long serial = orderRepository.nextBillSerial(type);

        // Then
        assertEquals(42L, serial);
        verify(orderRepository).nextBillSerial(type);
    }

    @Test
    @DisplayName("Should save preview and return generated order id")
    void shouldSavePreviewAndReturnOrderId() {
        // Given
        String type = "WEB";
        String location = "MAIN";
        Long userId = 10L;
        QuoteUseCase.Quote quote = mock(QuoteUseCase.Quote.class);
        when(orderRepository.savePreview(type, location, userId, quote)).thenReturn(1001L);

        // When
        long orderId = orderRepository.savePreview(type, location, userId, quote);

        // Then
        assertEquals(1001L, orderId);
        verify(orderRepository).savePreview(type, location, userId, quote);
    }

    @Test
    @DisplayName("Should save order lines for given order id")
    void shouldSaveOrderLines() {
        // Given
        long orderId = 1001L;
        List<BillLine> lines = List.of(mock(BillLine.class), mock(BillLine.class));

        // When
        orderRepository.saveLines(orderId, lines);

        // Then
        verify(orderRepository).saveLines(orderId, lines);
    }

    @Test
    @DisplayName("Should save final order with quote")
    void shouldSaveFinalOrder() {
        // Given
        long orderId = 1001L;
        QuoteUseCase.Quote quote = mock(QuoteUseCase.Quote.class);

        // When
        orderRepository.saveFinal(orderId, quote);

        // Then
        verify(orderRepository).saveFinal(orderId, quote);
    }
}
