package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.DiscountService;
import domain.pricing.Discount;
import domain.product.Product;
import domain.repository.ProductRepository;
import domain.repository.CartRepository;
import domain.shared.Code;
import domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@DisplayName("OnlineCartUseCase Tests")
class OnlineCartUseCaseTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DiscountService discountService;

    private OnlineCartUseCase onlineCartUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        onlineCartUseCase = new OnlineCartUseCase(cartRepository, productRepository, discountService);
    }

    @Nested
    @DisplayName("Add to Cart Tests")
    class AddToCartTests {

        @Test
        @DisplayName("Should add product to cart successfully")
        void shouldAddProductToCartSuccessfully() {
            // Given
            long customerId = 123L;
            String productCode = "PROD001";
            int quantity = 5;

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(10.99)), "Product description");

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);

            // When & Then
            assertDoesNotThrow(() -> onlineCartUseCase.addToCart(customerId, productCode, quantity));

            verify(cartRepository).getOrCreateCart(customerId);
            verify(cartRepository).upsertItem(1L, productCode, quantity);
        }

        @Test
        @DisplayName("Should fail when product not found")
        void shouldFailWhenProductNotFound() {
            // Given
            long customerId = 1L;
            String productCode = "UNKNOWN";
            int quantity = 1;

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addToCart(customerId, productCode, quantity));
        }

        @Test
        @DisplayName("Should fail when quantity is zero or negative")
        void shouldFailWhenQuantityIsZeroOrNegative() {
            // Given
            long customerId = 1L;
            String productCode = "PROD001";

            // When & Then
            assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addToCart(customerId, productCode, 0));
            assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addToCart(customerId, productCode, -1));
        }
    }

    @Nested
    @DisplayName("Remove Item from Cart Tests")
    class RemoveItemFromCartTests {

        @Test
        @DisplayName("Should remove item from cart successfully")
        void shouldRemoveItemFromCartSuccessfully() {
            // Given
            long customerId = 1L;
            String productCode = "PROD001";

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);

            // When & Then
            assertDoesNotThrow(() -> onlineCartUseCase.removeFromCart(customerId, productCode));

            verify(cartRepository).removeItem(1L, productCode);
        }

        @Test
        @DisplayName("Should remove entire cart when removing last item")
        void shouldRemoveEntireCartWhenRemovingLastItem() {
            // Given
            long customerId = 1L;
            String productCode = "PROD001";

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);
            // Simulate empty cart after removal
            when(cartRepository.items(1L)).thenReturn(Collections.emptyList());

            // When
            onlineCartUseCase.removeFromCart(customerId, productCode);

            // Then
            verify(cartRepository).removeItem(1L, productCode);
        }

        @Test
        @DisplayName("Should fail when item not found in cart")
        void shouldFailWhenItemNotFoundInCart() {
            // Given
            long customerId = 1L;
            String productCode = "NONEXISTENT";

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);

            // When & Then - Should not throw exception, just remove silently
            assertDoesNotThrow(() -> onlineCartUseCase.removeFromCart(customerId, productCode));

            verify(cartRepository).removeItem(1L, productCode);
        }
    }

    @Nested
    @DisplayName("Update Item Quantity Tests")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("Should update item quantity successfully")
        void shouldUpdateItemQuantitySuccessfully() {
            // Given
            long customerId = 1L;
            String productCode = "PROD001";
            int newQuantity = 5;

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(10.99)), "Product description");
            CartRepository.CartItem cartItem = new CartRepository.CartItem(productCode, newQuantity);

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);
            when(cartRepository.items(1L)).thenReturn(List.of(cartItem));
            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(discountService.getActiveDiscountsForProduct(eq(productCode), any())).thenReturn(Collections.emptyList());

            // When
            OnlineCartUseCase.UpdateItemResult result = onlineCartUseCase.updateItemQuantity(customerId, productCode, newQuantity);

            // Then
            assertTrue(result.success());
            assertNotNull(result.cart());
            assertEquals(1, result.cart().lines().size());
            assertEquals(newQuantity, result.cart().lines().get(0).qty);

            verify(cartRepository).upsertItem(1L, productCode, newQuantity);
        }

        @Test
        @DisplayName("Should remove item when updating quantity to zero")
        void shouldRemoveItemWhenUpdatingQuantityToZero() {
            // Given
            long customerId = 1L;
            String productCode = "PROD001";

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);
            when(cartRepository.items(1L)).thenReturn(Collections.emptyList()); // Empty after removal

            // When
            OnlineCartUseCase.UpdateItemResult result = onlineCartUseCase.updateItemQuantity(customerId, productCode, 0);

            // Then
            assertTrue(result.success());
            assertNotNull(result.cart());
            assertEquals(0, result.cart().lines().size());

            verify(cartRepository).removeItem(1L, productCode);
        }

        @Test
        @DisplayName("Should fail when item not found for quantity update")
        void shouldFailWhenItemNotFoundForQuantityUpdate() {
            // Given
            long customerId = 1L;
            String productCode = "NONEXISTENT";
            int newQuantity = 3;

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);
            when(cartRepository.items(1L)).thenReturn(Collections.emptyList());

            // When
            OnlineCartUseCase.UpdateItemResult result = onlineCartUseCase.updateItemQuantity(customerId, productCode, newQuantity);

            // Then - Should still succeed but with empty cart
            assertTrue(result.success());
            assertNotNull(result.cart());
            assertEquals(0, result.cart().lines().size());
        }
    }

    @Nested
    @DisplayName("View Cart Tests")
    class ViewCartTests {

        @Test
        @DisplayName("Should view cart contents successfully")
        void shouldViewCartContentsSuccessfully() {
            // Given
            long customerId = 1L;
            String productCode = "PROD001";

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(10.99)), "Product description");
            CartRepository.CartItem cartItem = new CartRepository.CartItem(productCode, 2);

            when(cartRepository.getOrCreateCart(customerId)).thenReturn(1L);
            when(cartRepository.items(1L)).thenReturn(List.of(cartItem));
            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(discountService.getActiveDiscountsForProduct(eq(productCode), any())).thenReturn(Collections.emptyList());

            // When
            OnlineCartUseCase.CartView result = onlineCartUseCase.viewCart(customerId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.lines().size());
            assertEquals(Money.of(BigDecimal.valueOf(21.98)), result.subtotal());
        }
    }
}
