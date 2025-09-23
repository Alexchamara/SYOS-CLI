package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.PaymentRepository;

@DisplayName("PaymentRepository Domain Interface Tests")
class PaymentRepositoryTest {

    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
    }

    @Test
    @DisplayName("Should define contract for saving card payments")
    void shouldDefineContractForSavingCardPayments() {
        // Given
        long orderId = 1001L;
        String last4 = "1234";
        String authRef = "AUTH-REF-XYZ";

        // When
        paymentRepository.saveCard(orderId, last4, authRef);

        // Then
        verify(paymentRepository).saveCard(orderId, last4, authRef);
    }
}
