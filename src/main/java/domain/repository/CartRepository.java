package domain.repository;

import java.util.List;

/**
 * Repository interface for shopping cart management
 */
public interface CartRepository {
    /**
     * Get existing cart or create new one for user
     * @param userId user ID
     * @return cart ID
     */
    long getOrCreateCart(long userId);

    /**
     * Add or update item in cart
     * @param cartId cart ID
     * @param productCode product code
     * @param qty quantity (if 0, item will be removed)
     */
    void upsertItem(long cartId, String productCode, int qty);

    /**
     * Remove item from cart
     * @param cartId cart ID
     * @param productCode product code to remove
     */
    void removeItem(long cartId, String productCode);

    /**
     * Get all items in cart
     * @param cartId cart ID
     * @return list of cart items (productCode, qty)
     */
    List<CartItem> items(long cartId);

    /**
     * Clear all items from cart
     * @param cartId cart ID
     */
    void clearCart(long cartId);

    /**
     * Cart item data class
     */
    class CartItem {
        public final String productCode;
        public final int qty;

        public CartItem(String productCode, int qty) {
            this.productCode = productCode;
            this.qty = qty;
        }
    }
}
