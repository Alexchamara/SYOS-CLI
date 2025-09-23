package domain.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import static org.junit.jupiter.api.Assertions.*;

import domain.product.Product;
import domain.shared.Code;
import domain.shared.Money;
import java.math.BigDecimal;

@DisplayName("Product Domain Entity Tests")
class ProductTest {

    private Code productCode;
    private Money price;

    @BeforeEach
    void setUp() {
        productCode = new Code("PROD001");
        price = Money.of(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Should create Product with valid parameters")
    void shouldCreateProductWithValidParameters() {
        // Given
        String name = "Laptop";

        // When
        Product product = new Product(productCode, name, price);

        // Then
        assertEquals(productCode, product.code());
        assertEquals(name, product.name());
        assertEquals(price, product.price());
        assertNull(product.categoryCode());
    }

    @Test
    @DisplayName("Should create Product with category code")
    void shouldCreateProductWithCategoryCode() {
        // Given
        String name = "Laptop";
        String categoryCode = "ELEC";

        // When
        Product product = new Product(productCode, name, price, categoryCode);

        // Then
        assertEquals(productCode, product.code());
        assertEquals(name, product.name());
        assertEquals(price, product.price());
        assertEquals(categoryCode, product.categoryCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid product names")
    void shouldThrowExceptionForInvalidProductNames(String invalidName) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Product(productCode, invalidName, price));
        assertEquals("name is null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when price is null")
    void shouldThrowExceptionWhenPriceIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Product(productCode, "Laptop", null));
        assertEquals("price is null", exception.getMessage());
    }

    @Test
    @DisplayName("Should trim whitespace from product name")
    void shouldTrimWhitespaceFromProductName() {
        // Given
        String nameWithWhitespace = "  Laptop  ";

        // When
        Product product = new Product(productCode, nameWithWhitespace, price);

        // Then
        assertEquals("Laptop", product.name());
    }

    @Test
    @DisplayName("Should handle different product codes")
    void shouldHandleDifferentProductCodes() {
        // Given
        Code code1 = new Code("PROD001");
        Code code2 = new Code("PROD002");

        // When
        Product product1 = new Product(code1, "Laptop", price);
        Product product2 = new Product(code2, "Mouse", price);

        // Then
        assertEquals(code1, product1.code());
        assertEquals(code2, product2.code());
        assertNotEquals(product1.code(), product2.code());
    }

    @Test
    @DisplayName("Should handle different prices")
    void shouldHandleDifferentPrices() {
        // Given
        Money price1 = Money.of(new BigDecimal("99.99"));
        Money price2 = Money.of(new BigDecimal("199.99"));

        // When
        Product product1 = new Product(productCode, "Laptop", price1);
        Product product2 = new Product(productCode, "Gaming Laptop", price2);

        // Then
        assertEquals(price1, product1.price());
        assertEquals(price2, product2.price());
    }

    @Test
    @DisplayName("Should handle null category code")
    void shouldHandleNullCategoryCode() {
        // When
        Product product = new Product(productCode, "Laptop", price, null);

        // Then
        assertNull(product.categoryCode());
    }

    @Test
    @DisplayName("Should handle empty category code")
    void shouldHandleEmptyCategoryCode() {
        // When
        Product product = new Product(productCode, "Laptop", price, "");

        // Then
        assertEquals("", product.categoryCode());
    }
}
