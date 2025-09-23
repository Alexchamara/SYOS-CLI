package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.ProductRepository;
import domain.product.Product;
import domain.shared.Code;

import java.util.List;
import java.util.Optional;

@DisplayName("ProductRepository Domain Interface Tests")
class ProductRepositoryTest {

    private ProductRepository productRepository;
    private Product product;
    private Code productCode;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        product = mock(Product.class);
        productCode = new Code("PROD001");
    }

    @Test
    @DisplayName("Should define contract for upserting product")
    void shouldDefineContractForUpsertingProduct() {
        // When
        productRepository.upsert(product);

        // Then
        verify(productRepository).upsert(product);
    }

    @Test
    @DisplayName("Should define contract for finding product by code")
    void shouldDefineContractForFindingProductByCode() {
        // Given
        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));

        // When
        Optional<Product> found = productRepository.findByCode(productCode);

        // Then
        assertTrue(found.isPresent());
        assertEquals(product, found.get());
        verify(productRepository).findByCode(productCode);
    }

    @Test
    @DisplayName("Should define contract for finding all products")
    void shouldDefineContractForFindingAllProducts() {
        // Given
        List<Product> expected = List.of(product);
        when(productRepository.findAll()).thenReturn(expected);

        // When
        List<Product> result = productRepository.findAll();

        // Then
        assertEquals(expected, result);
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should define contract for deleting product by code")
    void shouldDefineContractForDeletingProductByCode() {
        // Given
        when(productRepository.deleteByCode(productCode)).thenReturn(true);

        // When
        boolean deleted = productRepository.deleteByCode(productCode);

        // Then
        assertTrue(deleted);
        verify(productRepository).deleteByCode(productCode);
    }
}

