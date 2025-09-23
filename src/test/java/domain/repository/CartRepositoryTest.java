package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.CartRepository;
import java.util.List;

@DisplayName("CartRepository Domain Interface Tests")
class CartRepositoryTest {

    private CartRepository cartRepository;

    private long userId;
    private long cartId;
    private String productCode;
    private int qty;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        userId = 42L;
        cartId = 1001L;
        productCode = "PROD001";
        qty = 2;
    }

    @Test
    @DisplayName("Should define contract for getting or creating a cart for user")
    void shouldDefineContractForGetOrCreateCart() {
        // Given
        when(cartRepository.getOrCreateCart(userId)).thenReturn(cartId);

        // When
        long result = cartRepository.getOrCreateCart(userId);

        // Then
        assertEquals(cartId, result);
        verify(cartRepository).getOrCreateCart(userId);
    }

    @Test
    @DisplayName("Should define contract for upserting items in cart")
    void shouldDefineContractForUpsertingItemsInCart() {
        // When
        cartRepository.upsertItem(cartId, productCode, qty);

        // Then
        verify(cartRepository).upsertItem(cartId, productCode, qty);
    }

    @Test
    @DisplayName("Should define contract for removing items from cart")
    void shouldDefineContractForRemovingItemsFromCart() {
        // When
        cartRepository.removeItem(cartId, productCode);

        // Then
        verify(cartRepository).removeItem(cartId, productCode);
    }

    @Test
    @DisplayName("Should define contract for listing items in cart")
    void shouldDefineContractForListingItemsInCart() {
        // Given
        List<CartRepository.CartItem> expected = List.of(new CartRepository.CartItem(productCode, qty));
        when(cartRepository.items(cartId)).thenReturn(expected);

        // When
        List<CartRepository.CartItem> items = cartRepository.items(cartId);

        // Then
        assertEquals(expected, items);
        verify(cartRepository).items(cartId);
    }

    @Test
    @DisplayName("Should define contract for clearing the cart")
    void shouldDefineContractForClearingTheCart() {
        // When
        cartRepository.clearCart(cartId);

        // Then
        verify(cartRepository).clearCart(cartId);
    }
}
