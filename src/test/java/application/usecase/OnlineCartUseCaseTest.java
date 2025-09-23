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
import domain.shared.Code;
import domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Domain classes for testing
record Cart(Long customerId, List<CartItem> items, Money total, LocalDateTime lastUpdated) {}
record CartItem(String productCode, String productName, int quantity, Money unitPrice, Money totalPrice) {}

// Result classes
record AddItemResult(boolean success, Cart cart, String errorMessage) {}
record RemoveItemResult(boolean success, Cart cart, String errorMessage) {}

// Repository interface for testing
interface CartRepository {
    Optional<Cart> findByCustomerId(Long customerId);
    Cart save(Cart cart);
    boolean delete(Long customerId);
}

// Use case class for testing
class OnlineCartUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;

    public OnlineCartUseCase(CartRepository cartRepository, ProductRepository productRepository, DiscountService discountService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.discountService = discountService;
    }

    public OnlineCartUseCase(CartRepository cartRepository, ProductRepository productRepository) {
        this(cartRepository, productRepository, null);
    }

    public AddItemResult addItemToCart(Long customerId, String productCode, int quantity) {
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        if (productCode == null || productCode.isEmpty()) throw new IllegalArgumentException("Product code cannot be null or empty");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");

        Optional<Product> product = productRepository.findByCode(new Code(productCode));
        if (product.isEmpty()) {
            return new AddItemResult(false, null, "Product not found");
        }

        Optional<Cart> existingCart = cartRepository.findByCustomerId(customerId);
        // Implementation would handle cart logic here
        Cart savedCart = cartRepository.save(new Cart(customerId, Collections.emptyList(), Money.of(BigDecimal.ZERO), LocalDateTime.now()));
        return new AddItemResult(true, savedCart, null);
    }

    public RemoveItemResult removeItemFromCart(Long customerId, String productCode) {
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        if (productCode == null || productCode.isEmpty()) throw new IllegalArgumentException("Product code cannot be null or empty");

        Optional<Cart> cart = cartRepository.findByCustomerId(customerId);
        if (cart.isEmpty()) {
            return new RemoveItemResult(false, null, "Cart not found");
        }

        // Implementation would handle removal logic here
        return new RemoveItemResult(true, null, null);
    }

    public AddItemResult updateItemQuantity(Long customerId, String productCode, int newQuantity) {
        Optional<Cart> cart = cartRepository.findByCustomerId(customerId);
        if (cart.isEmpty()) {
            return new AddItemResult(false, null, "Cart not found");
        }

        // Implementation would handle quantity update logic here
        return new AddItemResult(true, cart.get(), null);
    }

    public Optional<Cart> getCart(Long customerId) {
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        return cartRepository.findByCustomerId(customerId);
    }

    public boolean clearCart(Long customerId) {
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        return cartRepository.delete(customerId);
    }

    public Money calculateCartTotal(Long customerId) {
        Optional<Cart> cart = cartRepository.findByCustomerId(customerId);
        return cart.map(Cart::total).orElse(Money.of(BigDecimal.ZERO));
    }

    public int getCartItemCount(Long customerId) {
        Optional<Cart> cart = cartRepository.findByCustomerId(customerId);
        return cart.map(c -> c.items().stream().mapToInt(CartItem::quantity).sum()).orElse(0);
    }
}

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
            Long customerId = 123L;
            String productCode = "PROD001";
            int quantity = 5;

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(10.99)), "Product description");
            Cart newCart = new Cart(customerId, Collections.emptyList(), Money.of(BigDecimal.valueOf(10.99)), LocalDateTime.now());

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

            // When
            AddItemResult result = onlineCartUseCase.addItemToCart(customerId, productCode, quantity);

            // Then
            assertTrue(result.success());
            assertEquals(1, result.cart().items().size());
            assertEquals(Money.of(BigDecimal.valueOf(10.99)), result.cart().total());

            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should update quantity when item already exists in cart")
        void shouldUpdateQuantityWhenItemAlreadyExistsInCart() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";
            int additionalQuantity = 2;

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(10.99)), "Product description");
            CartItem existingItem = new CartItem(productCode, "Product 1", 1, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(10.99)));
            Cart existingCart = new Cart(customerId, Arrays.asList(existingItem), Money.of(BigDecimal.valueOf(10.99)), LocalDateTime.now());

            CartItem updatedItem = new CartItem(productCode, "Product 1", 3, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(32.97)));
            Cart updatedCart = new Cart(customerId, Arrays.asList(updatedItem), Money.of(BigDecimal.valueOf(32.97)), LocalDateTime.now());

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(updatedCart);

            // When
            AddItemResult result = onlineCartUseCase.addItemToCart(customerId, productCode, additionalQuantity);

            // Then
            assertTrue(result.success());
            assertEquals(1, result.cart().items().size());
            assertEquals(3, result.cart().items().get(0).quantity());
            assertEquals(Money.of(BigDecimal.valueOf(32.97)), result.cart().total());
        }

        @Test
        @DisplayName("Should fail when product not found")
        void shouldFailWhenProductNotFound() {
            // Given
            Long customerId = 1L;
            String productCode = "UNKNOWN";
            int quantity = 1;

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.empty());

            // When
            AddItemResult result = onlineCartUseCase.addItemToCart(customerId, productCode, quantity);

            // Then
            assertFalse(result.success());
            assertEquals("Product not found", result.errorMessage());
            assertNull(result.cart());

            verify(productRepository).findByCode(new Code(productCode));
            verifyNoInteractions(cartRepository);
        }

        @Test
        @DisplayName("Should handle null customer ID")
        void shouldHandleNullCustomerId() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addItemToCart(null, "PRD001", 1));
            assertEquals("Customer ID cannot be null", exception.getMessage());

            verifyNoInteractions(productRepository, cartRepository);
        }

        @Test
        @DisplayName("Should handle null product code")
        void shouldHandleNullProductCode() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addItemToCart(1L, null, 1));
            assertEquals("Product code cannot be null or empty", exception.getMessage());

            verifyNoInteractions(productRepository, cartRepository);
        }

        @Test
        @DisplayName("Should handle empty product code")
        void shouldHandleEmptyProductCode() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addItemToCart(1L, "", 1));
            assertEquals("Product code cannot be null or empty", exception.getMessage());

            verifyNoInteractions(productRepository, cartRepository);
        }

        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addItemToCart(1L, "PRD001", 0));
            assertEquals("Quantity must be greater than zero", exception.getMessage());

            verifyNoInteractions(productRepository, cartRepository);
        }

        @Test
        @DisplayName("Should handle negative quantity")
        void shouldHandleNegativeQuantity() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.addItemToCart(1L, "PRD001", -1));
            assertEquals("Quantity must be greater than zero", exception.getMessage());

            verifyNoInteractions(productRepository, cartRepository);
        }
    }

    @Nested
    @DisplayName("Remove Item from Cart Tests")
    class RemoveItemFromCartTests {

        @Test
        @DisplayName("Should remove item from cart successfully")
        void shouldRemoveItemFromCartSuccessfully() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";

            CartItem itemToRemove = new CartItem(productCode, "Product 1", 2, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(21.98)));
            CartItem otherItem = new CartItem("PRD002", "Product 2", 1, Money.of(BigDecimal.valueOf(5.99)), Money.of(BigDecimal.valueOf(5.99)));
            Cart existingCart = new Cart(customerId, Arrays.asList(itemToRemove, otherItem), Money.of(BigDecimal.valueOf(27.97)), LocalDateTime.now());
            Cart updatedCart = new Cart(customerId, Arrays.asList(otherItem), Money.of(BigDecimal.valueOf(5.99)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(updatedCart);

            // When
            RemoveItemResult result = onlineCartUseCase.removeItemFromCart(customerId, productCode);

            // Then
            assertTrue(result.success());
            assertNull(result.errorMessage());
            assertEquals(1, result.cart().items().size());
            assertEquals("PRD002", result.cart().items().get(0).productCode());
            assertEquals(Money.of(BigDecimal.valueOf(5.99)), result.cart().total());

            verify(cartRepository).findByCustomerId(customerId);
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should remove entire cart when removing last item")
        void shouldRemoveEntireCartWhenRemovingLastItem() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";

            CartItem lastItem = new CartItem(productCode, "Product 1", 1, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(10.99)));
            Cart existingCart = new Cart(customerId, Arrays.asList(lastItem), Money.of(BigDecimal.valueOf(10.99)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));
            when(cartRepository.delete(customerId)).thenReturn(true);

            // When
            RemoveItemResult result = onlineCartUseCase.removeItemFromCart(customerId, productCode);

            // Then
            assertTrue(result.success());
            assertNull(result.cart());

            verify(cartRepository).findByCustomerId(customerId);
            verify(cartRepository).delete(customerId);
        }

        @Test
        @DisplayName("Should fail when cart not found")
        void shouldFailWhenCartNotFound() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            // When
            RemoveItemResult result = onlineCartUseCase.removeItemFromCart(customerId, productCode);

            // Then
            assertFalse(result.success());
            assertEquals("Cart not found", result.errorMessage());
            assertNull(result.cart());

            verify(cartRepository).findByCustomerId(customerId);
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should fail when item not found in cart")
        void shouldFailWhenItemNotFoundInCart() {
            // Given
            Long customerId = 1L;
            String productCode = "UNKNOWN";

            CartItem existingItem = new CartItem("PRD001", "Product 1", 1, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(10.99)));
            Cart existingCart = new Cart(customerId, Arrays.asList(existingItem), Money.of(BigDecimal.valueOf(10.99)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));

            // When
            RemoveItemResult result = onlineCartUseCase.removeItemFromCart(customerId, productCode);

            // Then
            assertFalse(result.success());
            assertEquals("Item not found in cart", result.errorMessage());
            assertNull(result.cart());

            verify(cartRepository).findByCustomerId(customerId);
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should handle null customer ID for remove")
        void shouldHandleNullCustomerIdForRemove() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.removeItemFromCart(null, "PRD001"));
            assertEquals("Customer ID cannot be null", exception.getMessage());

            verifyNoInteractions(cartRepository);
        }

        @Test
        @DisplayName("Should handle null product code for remove")
        void shouldHandleNullProductCodeForRemove() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.removeItemFromCart(1L, null));
            assertEquals("Product code cannot be null or empty", exception.getMessage());

            verifyNoInteractions(cartRepository);
        }
    }

    @Nested
    @DisplayName("Update Item Quantity Tests")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("Should update item quantity successfully")
        void shouldUpdateItemQuantitySuccessfully() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";
            int newQuantity = 5;

            CartItem existingItem = new CartItem(productCode, "Product 1", 2, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(21.98)));
            Cart existingCart = new Cart(customerId, Arrays.asList(existingItem), Money.of(BigDecimal.valueOf(21.98)), LocalDateTime.now());

            CartItem updatedItem = new CartItem(productCode, "Product 1", newQuantity, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(54.95)));
            Cart updatedCart = new Cart(customerId, Arrays.asList(updatedItem), Money.of(BigDecimal.valueOf(54.95)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(updatedCart);

            // When
            AddItemResult result = onlineCartUseCase.updateItemQuantity(customerId, productCode, newQuantity);

            // Then
            assertTrue(result.success());
            assertEquals(newQuantity, result.cart().items().get(0).quantity());
            assertEquals(Money.of(BigDecimal.valueOf(54.95)), result.cart().total());

            verify(cartRepository).findByCustomerId(customerId);
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should remove item when updating quantity to zero")
        void shouldRemoveItemWhenUpdatingQuantityToZero() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";
            int newQuantity = 0;

            CartItem itemToRemove = new CartItem(productCode, "Product 1", 2, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(21.98)));
            CartItem otherItem = new CartItem("PRD002", "Product 2", 1, Money.of(BigDecimal.valueOf(5.99)), Money.of(BigDecimal.valueOf(5.99)));
            Cart existingCart = new Cart(customerId, Arrays.asList(itemToRemove, otherItem), Money.of(BigDecimal.valueOf(27.97)), LocalDateTime.now());
            Cart updatedCart = new Cart(customerId, Arrays.asList(otherItem), Money.of(BigDecimal.valueOf(5.99)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(updatedCart);

            // When
            AddItemResult result = onlineCartUseCase.updateItemQuantity(customerId, productCode, newQuantity);

            // Then
            assertTrue(result.success());
            assertEquals(1, result.cart().items().size());
            assertEquals("PRD002", result.cart().items().get(0).productCode());
        }

        @Test
        @DisplayName("Should fail when item not found for quantity update")
        void shouldFailWhenItemNotFoundForQuantityUpdate() {
            // Given
            Long customerId = 1L;
            String productCode = "UNKNOWN";
            int newQuantity = 3;

            CartItem existingItem = new CartItem("PRD001", "Product 1", 1, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(10.99)));
            Cart existingCart = new Cart(customerId, Arrays.asList(existingItem), Money.of(BigDecimal.valueOf(10.99)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCart));

            // When
            AddItemResult result = onlineCartUseCase.updateItemQuantity(customerId, productCode, newQuantity);

            // Then
            assertFalse(result.success());
            assertEquals("Item not found in cart", result.errorMessage());

            verify(cartRepository).findByCustomerId(customerId);
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("Get Cart Tests")
    class GetCartTests {

        @Test
        @DisplayName("Should get cart successfully")
        void shouldGetCartSuccessfully() {
            // Given
            Long customerId = 1L;
            CartItem item = new CartItem("PRD001", "Product 1", 2, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(21.98)));
            Cart expectedCart = new Cart(customerId, Arrays.asList(item), Money.of(BigDecimal.valueOf(21.98)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(expectedCart));

            // When
            Optional<Cart> result = onlineCartUseCase.getCart(customerId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(expectedCart, result.get());

            verify(cartRepository).findByCustomerId(customerId);
        }

        @Test
        @DisplayName("Should return empty when cart not found")
        void shouldReturnEmptyWhenCartNotFound() {
            // Given
            Long customerId = 1L;

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            // When
            Optional<Cart> result = onlineCartUseCase.getCart(customerId);

            // Then
            assertFalse(result.isPresent());

            verify(cartRepository).findByCustomerId(customerId);
        }

        @Test
        @DisplayName("Should handle null customer ID for get cart")
        void shouldHandleNullCustomerIdForGetCart() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.getCart(null));
            assertEquals("Customer ID cannot be null", exception.getMessage());

            verifyNoInteractions(cartRepository);
        }
    }

    @Nested
    @DisplayName("Clear Cart Tests")
    class ClearCartTests {

        @Test
        @DisplayName("Should clear cart successfully")
        void shouldClearCartSuccessfully() {
            // Given
            Long customerId = 1L;

            when(cartRepository.delete(customerId)).thenReturn(true);

            // When
            boolean result = onlineCartUseCase.clearCart(customerId);

            // Then
            assertTrue(result);

            verify(cartRepository).delete(customerId);
        }

        @Test
        @DisplayName("Should handle clear cart failure")
        void shouldHandleClearCartFailure() {
            // Given
            Long customerId = 1L;

            when(cartRepository.delete(customerId)).thenReturn(false);

            // When
            boolean result = onlineCartUseCase.clearCart(customerId);

            // Then
            assertFalse(result);

            verify(cartRepository).delete(customerId);
        }

        @Test
        @DisplayName("Should handle null customer ID for clear cart")
        void shouldHandleNullCustomerIdForClearCart() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onlineCartUseCase.clearCart(null));
            assertEquals("Customer ID cannot be null", exception.getMessage());

            verifyNoInteractions(cartRepository);
        }
    }

    @Nested
    @DisplayName("Cart Calculations Tests")
    class CartCalculationsTests {

        @Test
        @DisplayName("Should calculate cart total correctly")
        void shouldCalculateCartTotalCorrectly() {
            // Given
            Long customerId = 1L;
            List<CartItem> items = Arrays.asList(
                new CartItem("PRD001", "Product 1", 2, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(21.98))),
                new CartItem("PRD002", "Product 2", 1, Money.of(BigDecimal.valueOf(5.99)), Money.of(BigDecimal.valueOf(5.99)))
            );
            Cart cart = new Cart(customerId, items, Money.of(BigDecimal.valueOf(27.97)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

            // When
            Money result = onlineCartUseCase.calculateCartTotal(customerId);

            // Then
            assertEquals(Money.of(BigDecimal.valueOf(27.97)), result);

            verify(cartRepository).findByCustomerId(customerId);
        }

        @Test
        @DisplayName("Should return zero total for empty cart")
        void shouldReturnZeroTotalForEmptyCart() {
            // Given
            Long customerId = 1L;

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            // When
            Money result = onlineCartUseCase.calculateCartTotal(customerId);

            // Then
            assertEquals(Money.of(BigDecimal.ZERO), result);

            verify(cartRepository).findByCustomerId(customerId);
        }

        @Test
        @DisplayName("Should count items in cart correctly")
        void shouldCountItemsInCartCorrectly() {
            // Given
            Long customerId = 1L;
            List<CartItem> items = Arrays.asList(
                new CartItem("PRD001", "Product 1", 2, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(21.98))),
                new CartItem("PRD002", "Product 2", 3, Money.of(BigDecimal.valueOf(5.99)), Money.of(BigDecimal.valueOf(17.97)))
            );
            Cart cart = new Cart(customerId, items, Money.of(BigDecimal.valueOf(39.95)), LocalDateTime.now());

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

            // When
            int result = onlineCartUseCase.getCartItemCount(customerId);

            // Then
            assertEquals(5, result); // 2 + 3 = 5 total items

            verify(cartRepository).findByCustomerId(customerId);
        }
    }

    @Nested
    @DisplayName("Record Tests")
    class RecordTests {

        @Test
        @DisplayName("Should create Cart record properly")
        void shouldCreateCartRecordProperly() {
            // Given
            Long customerId = 1L;
            LocalDateTime now = LocalDateTime.now();
            List<CartItem> items = Arrays.asList(
                new CartItem("PRD001", "Product 1", 1, Money.of(BigDecimal.valueOf(10.99)), Money.of(BigDecimal.valueOf(10.99)))
            );

            // When
            Cart cart = new Cart(customerId, items, Money.of(BigDecimal.valueOf(10.99)), now);

            // Then
            assertEquals(customerId, cart.customerId());
            assertEquals(1, cart.items().size());
            assertEquals(Money.of(BigDecimal.valueOf(10.99)), cart.total());
            assertEquals(now, cart.lastUpdated());
        }

        @Test
        @DisplayName("Should create CartItem record properly")
        void shouldCreateCartItemRecordProperly() {
            // When
            CartItem item = new CartItem(
                "PRD001",
                "Product 1",
                2,
                Money.of(BigDecimal.valueOf(10.99)),
                Money.of(BigDecimal.valueOf(21.98))
            );

            // Then
            assertEquals("PRD001", item.productCode());
            assertEquals("Product 1", item.productName());
            assertEquals(2, item.quantity());
            assertEquals(Money.of(BigDecimal.valueOf(10.99)), item.unitPrice());
            assertEquals(Money.of(BigDecimal.valueOf(21.98)), item.totalPrice());
        }

        @Test
        @DisplayName("Should create AddItemResult record properly")
        void shouldCreateAddItemResultRecordProperly() {
            // Given
            Cart cart = new Cart(1L, Collections.emptyList(), Money.of(BigDecimal.ZERO), LocalDateTime.now());

            // When
            AddItemResult successResult = new AddItemResult(true, cart, null);
            AddItemResult failureResult = new AddItemResult(false, null, "Error message");

            // Then
            assertTrue(successResult.success());
            assertNotNull(successResult.cart());
            assertNull(successResult.errorMessage());

            assertFalse(failureResult.success());
            assertNull(failureResult.cart());
            assertEquals("Error message", failureResult.errorMessage());
        }

        @Test
        @DisplayName("Should create RemoveItemResult record properly")
        void shouldCreateRemoveItemResultRecordProperly() {
            // Given
            Cart cart = new Cart(1L, Collections.emptyList(), Money.of(BigDecimal.ZERO), LocalDateTime.now());

            // When
            RemoveItemResult successResult = new RemoveItemResult(true, cart, null);
            RemoveItemResult failureResult = new RemoveItemResult(false, null, "Error message");

            // Then
            assertTrue(successResult.success());
            assertNotNull(successResult.cart());
            assertNull(successResult.errorMessage());

            assertFalse(failureResult.success());
            assertNull(failureResult.cart());
            assertEquals("Error message", failureResult.errorMessage());
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("Should handle repository exceptions")
        void shouldHandleRepositoryExceptions() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";

            when(productRepository.findByCode(new Code(productCode)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> onlineCartUseCase.addItemToCart(customerId, productCode, 1));
            assertEquals("Database connection failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle very large quantities")
        void shouldHandleVeryLargeQuantities() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";
            int largeQuantity = Integer.MAX_VALUE;

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(0.01)), "Product description");

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            // When & Then
            assertDoesNotThrow(() -> onlineCartUseCase.addItemToCart(customerId, productCode, largeQuantity));
        }

        @Test
        @DisplayName("Should handle cart save failures")
        void shouldHandleCartSaveFailures() {
            // Given
            Long customerId = 1L;
            String productCode = "PRD001";

            Product product = new Product(new Code(productCode), "Product 1", Money.of(BigDecimal.valueOf(10.99)), "Product description");

            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenThrow(new RuntimeException("Failed to save cart"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> onlineCartUseCase.addItemToCart(customerId, productCode, 1));
            assertEquals("Failed to save cart", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create OnlineCartUseCase with valid dependencies")
        void shouldCreateOnlineCartUseCaseWithValidDependencies() {
            // When
            OnlineCartUseCase useCase = new OnlineCartUseCase(cartRepository, productRepository, discountService);

            // Then
            assertNotNull(useCase);
        }

        @Test
        @DisplayName("Should handle null CartRepository")
        void shouldHandleNullCartRepository() {
            // When & Then
            assertDoesNotThrow(() -> new OnlineCartUseCase(null, productRepository, discountService));
        }

        @Test
        @DisplayName("Should handle null ProductRepository")
        void shouldHandleNullProductRepository() {
            // When & Then
            assertDoesNotThrow(() -> new OnlineCartUseCase(cartRepository, null, discountService));
        }
    }
}
