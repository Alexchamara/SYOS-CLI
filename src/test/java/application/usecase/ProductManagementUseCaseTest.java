package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.ProductManagementUseCase;
import application.usecase.ProductManagementUseCase.CreateProductRequest;
import application.usecase.ProductManagementUseCase.CreateProductWithCategoryRequest;
import application.usecase.ProductManagementUseCase.ProductInfo;
import application.usecase.ProductManagementUseCase.CreateResult;
import application.usecase.ProductManagementUseCase.DeleteResult;
import application.usecase.CategoryManagementUseCase;
import domain.product.Product;
import domain.repository.ProductRepository;
import domain.shared.Code;
import domain.shared.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@DisplayName("ProductManagementUseCase Tests")
class ProductManagementUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryManagementUseCase categoryManagementUseCase;

    private ProductManagementUseCase productManagementUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productManagementUseCase = new ProductManagementUseCase(productRepository, categoryManagementUseCase);
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create new product successfully")
        void shouldCreateNewProductSuccessfully() {
            // Given
            CreateProductRequest request = new CreateProductRequest("PROD001", "Test Product", new BigDecimal("10.00"));
            when(productRepository.findByCode(new Code("PROD001"))).thenReturn(Optional.empty());

            // When
            CreateResult result = productManagementUseCase.createProduct(request);

            // Then
            assertEquals(CreateResult.SUCCESS, result);
            verify(productRepository).findByCode(new Code("PROD001"));
            verify(productRepository).upsert(any(Product.class));
        }

        @Test
        @DisplayName("Should update existing product")
        void shouldUpdateExistingProduct() {
            // Given
            CreateProductRequest request = new CreateProductRequest("PROD001", "Updated Product", new BigDecimal("15.00"));
            Product existingProduct = new Product(new Code("PROD001"), "Old Product", Money.of(new BigDecimal("10.00")));
            when(productRepository.findByCode(new Code("PROD001"))).thenReturn(Optional.of(existingProduct));

            // When
            CreateResult result = productManagementUseCase.createProduct(request);

            // Then
            assertEquals(CreateResult.UPDATED, result);
            verify(productRepository).findByCode(new Code("PROD001"));
            verify(productRepository).upsert(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when product code is null")
        void shouldThrowExceptionWhenProductCodeIsNull() {
            // Given
            CreateProductRequest request = new CreateProductRequest(null, "Test Product", new BigDecimal("10.00"));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productManagementUseCase.createProduct(request));
            assertTrue(exception.getMessage().contains("Product code cannot be null or blank"));

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("Should throw exception when product name is null")
        void shouldThrowExceptionWhenProductNameIsNull() {
            // Given
            CreateProductRequest request = new CreateProductRequest("PROD001", null, new BigDecimal("10.00"));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productManagementUseCase.createProduct(request));
            assertTrue(exception.getMessage().contains("Product name cannot be null or blank"));

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("Should throw exception when price is null")
        void shouldThrowExceptionWhenPriceIsNull() {
            // Given
            CreateProductRequest request = new CreateProductRequest("PROD001", "Test Product", null);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productManagementUseCase.createProduct(request));
            assertTrue(exception.getMessage().contains("Price cannot be null"));

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("Should throw exception when price is negative")
        void shouldThrowExceptionWhenPriceIsNegative() {
            // Given
            CreateProductRequest request = new CreateProductRequest("PROD001", "Test Product", new BigDecimal("-5.00"));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productManagementUseCase.createProduct(request));
            assertTrue(exception.getMessage().contains("Price must be positive"));

            verifyNoInteractions(productRepository);
        }
    }

    @Nested
    @DisplayName("Create Product With Category Tests")
    class CreateProductWithCategoryTests {

        @Test
        @DisplayName("Should create product with auto-generated code from category")
        void shouldCreateProductWithAutoGeneratedCodeFromCategory() {
            // Given
            CreateProductWithCategoryRequest request = new CreateProductWithCategoryRequest(
                "ELEC", "Laptop", new BigDecimal("999.99")
            );
            when(categoryManagementUseCase.generateProductCode("ELEC")).thenReturn("EL001");
            when(productRepository.findByCode(new Code("EL001"))).thenReturn(Optional.empty());

            // When
            ProductManagementUseCase.CreateProductWithCategoryResult result =
                productManagementUseCase.createProductWithCategory(request);

            // Then
            assertTrue(result.isSuccess());
            assertEquals("EL001", result.getGeneratedCode());
            verify(categoryManagementUseCase).generateProductCode("ELEC");
            verify(productRepository).upsert(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when category code is null")
        void shouldThrowExceptionWhenCategoryCodeIsNull() {
            // Given
            CreateProductWithCategoryRequest request = new CreateProductWithCategoryRequest(
                null, "Laptop", new BigDecimal("999.99")
            );

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productManagementUseCase.createProductWithCategory(request));
            assertTrue(exception.getMessage().contains("Category code cannot be null or blank"));

            verifyNoInteractions(categoryManagementUseCase, productRepository);
        }

        @Test
        @DisplayName("Should handle category not found exception")
        void shouldHandleCategoryNotFoundException() {
            // Given
            CreateProductWithCategoryRequest request = new CreateProductWithCategoryRequest(
                "NONEXISTENT", "Product", new BigDecimal("10.00")
            );
            when(categoryManagementUseCase.generateProductCode("NONEXISTENT"))
                .thenThrow(new IllegalArgumentException("Category not found: NONEXISTENT"));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productManagementUseCase.createProductWithCategory(request));
            assertEquals("Category not found: NONEXISTENT", exception.getMessage());

            verify(categoryManagementUseCase).generateProductCode("NONEXISTENT");
            verifyNoInteractions(productRepository);
        }
    }

    @Nested
    @DisplayName("Get Products Tests")
    class GetProductsTests {

        @Test
        @DisplayName("Should get all products")
        void shouldGetAllProducts() {
            // Given
            List<Product> products = List.of(
                new Product(new Code("PROD001"), "Product 1", Money.of(new BigDecimal("10.00"))),
                new Product(new Code("PROD002"), "Product 2", Money.of(new BigDecimal("20.00")))
            );
            when(productRepository.findAll()).thenReturn(products);

            // When
            List<ProductInfo> result = productManagementUseCase.getAllProducts();

            // Then
            assertEquals(2, result.size());
            assertEquals("PROD001", result.get(0).code());
            assertEquals("Product 1", result.get(0).name());
            assertEquals(new BigDecimal("10.00"), result.get(0).price());
            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("Should get product by code")
        void shouldGetProductByCode() {
            // Given
            String productCode = "PROD001";
            Product product = new Product(new Code(productCode), "Test Product", Money.of(new BigDecimal("15.00")));
            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.of(product));

            // When
            Optional<ProductInfo> result = productManagementUseCase.getProductByCode(productCode);

            // Then
            assertTrue(result.isPresent());
            assertEquals(productCode, result.get().code());
            assertEquals("Test Product", result.get().name());
            assertEquals(new BigDecimal("15.00"), result.get().price());
            verify(productRepository).findByCode(new Code(productCode));
        }

        @Test
        @DisplayName("Should return empty when product not found")
        void shouldReturnEmptyWhenProductNotFound() {
            // Given
            String productCode = "NONEXISTENT";
            when(productRepository.findByCode(new Code(productCode))).thenReturn(Optional.empty());

            // When
            Optional<ProductInfo> result = productManagementUseCase.getProductByCode(productCode);

            // Then
            assertFalse(result.isPresent());
            verify(productRepository).findByCode(new Code(productCode));
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete existing product successfully")
        void shouldDeleteExistingProductSuccessfully() {
            // Given
            String productCode = "PROD001";
            when(productRepository.existsByCode(new Code(productCode))).thenReturn(true);

            // When
            DeleteResult result = productManagementUseCase.deleteProduct(productCode);

            // Then
            assertEquals(DeleteResult.SUCCESS, result);
            verify(productRepository).existsByCode(new Code(productCode));
            verify(productRepository).deleteByCode(new Code(productCode));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when product does not exist")
        void shouldReturnNotFoundWhenProductDoesNotExist() {
            // Given
            String productCode = "NONEXISTENT";
            when(productRepository.existsByCode(new Code(productCode))).thenReturn(false);

            // When
            DeleteResult result = productManagementUseCase.deleteProduct(productCode);

            // Then
            assertEquals(DeleteResult.NOT_FOUND, result);
            verify(productRepository).existsByCode(new Code(productCode));
            verify(productRepository, never()).deleteByCode(any(Code.class));
        }
    }

    @Nested
    @DisplayName("Request Record Tests")
    class RequestRecordTests {

        @Test
        @DisplayName("Should create CreateProductRequest correctly")
        void shouldCreateCreateProductRequestCorrectly() {
            // When
            CreateProductRequest request = new CreateProductRequest("PROD001", "Test Product", new BigDecimal("10.00"));

            // Then
            assertEquals("PROD001", request.code());
            assertEquals("Test Product", request.name());
            assertEquals(new BigDecimal("10.00"), request.price());
        }

        @Test
        @DisplayName("Should create CreateProductWithCategoryRequest correctly")
        void shouldCreateCreateProductWithCategoryRequestCorrectly() {
            // When
            CreateProductWithCategoryRequest request = new CreateProductWithCategoryRequest(
                "ELEC", "Laptop", new BigDecimal("999.99")
            );

            // Then
            assertEquals("ELEC", request.categoryCode());
            assertEquals("Laptop", request.name());
            assertEquals(new BigDecimal("999.99"), request.price());
        }

        @Test
        @DisplayName("Should create ProductInfo from Product correctly")
        void shouldCreateProductInfoFromProductCorrectly() {
            // Given
            Product product = new Product(new Code("PROD001"), "Test Product", Money.of(new BigDecimal("15.00")));

            // When
            ProductInfo productInfo = new ProductInfo(product);

            // Then
            assertEquals("PROD001", productInfo.code());
            assertEquals("Test Product", productInfo.name());
            assertEquals(new BigDecimal("15.00"), productInfo.price());
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("Should have correct CreateResult values")
        void shouldHaveCorrectCreateResultValues() {
            // Then
            assertEquals(2, CreateResult.values().length);
            assertEquals(CreateResult.SUCCESS, CreateResult.valueOf("SUCCESS"));
            assertEquals(CreateResult.UPDATED, CreateResult.valueOf("UPDATED"));
        }

        @Test
        @DisplayName("Should have correct DeleteResult values")
        void shouldHaveCorrectDeleteResultValues() {
            // Then
            assertEquals(2, DeleteResult.values().length);
            assertEquals(DeleteResult.SUCCESS, DeleteResult.valueOf("SUCCESS"));
            assertEquals(DeleteResult.NOT_FOUND, DeleteResult.valueOf("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ProductManagementUseCase with required dependencies")
        void shouldCreateProductManagementUseCaseWithRequiredDependencies() {
            // When
            ProductManagementUseCase useCase = new ProductManagementUseCase(productRepository, categoryManagementUseCase);

            // Then
            assertNotNull(useCase);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large prices")
        void shouldHandleVeryLargePrices() {
            // Given
            CreateProductRequest request = new CreateProductRequest(
                "PROD001", "Expensive Product", new BigDecimal("999999.99")
            );
            when(productRepository.findByCode(new Code("PROD001"))).thenReturn(Optional.empty());

            // When
            CreateResult result = productManagementUseCase.createProduct(request);

            // Then
            assertEquals(CreateResult.SUCCESS, result);
            verify(productRepository).upsert(any(Product.class));
        }

        @Test
        @DisplayName("Should handle products with long names")
        void shouldHandleProductsWithLongNames() {
            // Given
            String longName = "A".repeat(1000); // Very long product name
            CreateProductRequest request = new CreateProductRequest("PROD001", longName, new BigDecimal("10.00"));
            when(productRepository.findByCode(new Code("PROD001"))).thenReturn(Optional.empty());

            // When
            CreateResult result = productManagementUseCase.createProduct(request);

            // Then
            assertEquals(CreateResult.SUCCESS, result);
            verify(productRepository).upsert(argThat(product -> product.name().equals(longName)));
        }
    }
}
