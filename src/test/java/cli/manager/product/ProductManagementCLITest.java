package cli.manager.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cli.manager.product.ProductManagementCLI;
import application.usecase.ProductManagementUseCase;
import application.usecase.CategoryManagementUseCase;
import domain.product.Category;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@DisplayName("ProductManagementCLI Tests")
class ProductManagementCLITest {

    @Mock
    private ProductManagementUseCase productManagementUseCase;

    @Mock
    private CategoryManagementUseCase categoryManagementUseCase;

    private ProductManagementCLI productManagementCLI;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Capture System.out and System.in for testing
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;

        // Note: We'll create the CLI instance in each test method with the proper Scanner
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setIn(originalIn);
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Menu Display Tests")
    class MenuDisplayTests {

        @Test
        @DisplayName("Should display product management menu")
        void shouldDisplayProductManagementMenu() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("PRODUCT MANAGEMENT"));
            assertTrue(output.contains("1. Add New Product (Select Category)"));
            assertTrue(output.contains("2. Add New Product (Manual Code)"));
            assertTrue(output.contains("3. Update Product"));
            assertTrue(output.contains("4. View Product Details"));
            assertTrue(output.contains("5. View All Products"));
            assertTrue(output.contains("6. Delete Product"));
            assertTrue(output.contains("0. Back to Manager Menu"));
        }
    }

    @Nested
    @DisplayName("Add Product With Category Tests")
    class AddProductWithCategoryTests {

        @Test
        @DisplayName("Should add product with selected category")
        void shouldAddProductWithSelectedCategory() {
            // Given
            List<Category> categories = List.of(
                new Category("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true),
                new Category("FOOD", "Food", "Food items", "FD", 1, 2, true)
            );

            when(categoryManagementUseCase.getAllActiveCategories()).thenReturn(categories);
            when(productManagementUseCase.createProductWithCategory(any())).thenReturn(
                new ProductManagementUseCase.CreateProductWithCategoryResult(
                    ProductManagementUseCase.CreateResult.SUCCESS, "EL001"));

            String input = "1\n1\nTest Product\n99.99\n\n0\n"; // Option 1, category 1, name, price, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(categoryManagementUseCase).getAllActiveCategories();
            verify(productManagementUseCase).createProductWithCategory(argThat(request ->
                request.categoryCode().equals("ELEC") &&
                request.name().equals("Test Product") &&
                request.price().equals(new BigDecimal("99.99"))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Available categories:"));
            assertTrue(output.contains("1. ELEC - Electronics"));
            assertTrue(output.contains("Product created successfully with code: EL001"));
        }

        @Test
        @DisplayName("Should handle no active categories")
        void shouldHandleNoActiveCategories() {
            // Given
            when(categoryManagementUseCase.getAllActiveCategories()).thenReturn(List.of());
            String input = "1\n\n0\n"; // Try add with category, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No active categories available"));
            verify(categoryManagementUseCase).getAllActiveCategories();
            verifyNoInteractions(productManagementUseCase);
        }

        @Test
        @DisplayName("Should handle invalid category selection")
        void shouldHandleInvalidCategorySelection() {
            // Given
            List<Category> categories = List.of(
                new Category("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true)
            );
            when(categoryManagementUseCase.getAllActiveCategories()).thenReturn(categories);

            String input = "1\n999\n\n0\n"; // Invalid category, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid selection"));
        }

        @Test
        @DisplayName("Should handle invalid price input")
        void shouldHandleInvalidPriceInput() {
            // Given
            List<Category> categories = List.of(
                new Category("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true)
            );
            when(categoryManagementUseCase.getAllActiveCategories()).thenReturn(categories);

            String input = "1\n1\nTest Product\nabc\n10.00\n\n0\n"; // Valid category, invalid then valid price, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(productManagementUseCase.createProductWithCategory(any())).thenReturn(
                new ProductManagementUseCase.CreateProductWithCategoryResult(
                    ProductManagementUseCase.CreateResult.SUCCESS, "EL001"));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid price format"));
        }
    }

    @Nested
    @DisplayName("Add Product Manual Code Tests")
    class AddProductManualCodeTests {

        @Test
        @DisplayName("Should add product with manual code")
        void shouldAddProductWithManualCode() {
            // Given
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.SUCCESS);

            String input = "2\nPROD001\nManual Product\n49.99\n\n0\n"; // Manual code option, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).createProduct(argThat(request ->
                request.code().equals("PROD001") &&
                request.name().equals("Manual Product") &&
                request.price().equals(new BigDecimal("49.99"))
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Product created successfully"));
        }

        @Test
        @DisplayName("Should handle empty product code")
        void shouldHandleEmptyProductCode() {
            // Given
            String input = "2\n\nVALID001\nValid Product\n10.00\n\n0\n"; // Empty code, then valid, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.SUCCESS);

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product code cannot be empty"));
        }
    }

    @Nested
    @DisplayName("View Product Tests")
    class ViewProductTests {

        @Test
        @DisplayName("Should view product details")
        void shouldViewProductDetails() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Test Product",
                        domain.shared.Money.of(new BigDecimal("99.99"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));

            String input = "4\nPROD001\n\n0\n"; // View product, enter code, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            String output = outputStream.toString();
            assertTrue(output.contains("Product Details"));
            assertTrue(output.contains("Code:"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Name:"));
            assertTrue(output.contains("Test Product"));
        }

        @Test
        @DisplayName("Should handle product not found")
        void shouldHandleProductNotFound() {
            // Given
            when(productManagementUseCase.findProduct("NONEXISTENT")).thenReturn(Optional.empty());

            String input = "4\nNONEXISTENT\n\n0\n"; // View non-existent product, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product not found"));
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void shouldDeleteProductSuccessfully() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Test Product",
                        domain.shared.Money.of(new BigDecimal("99.99"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));
            when(productManagementUseCase.deleteProduct("PROD001")).thenReturn(
                ProductManagementUseCase.DeleteResult.SUCCESS);

            String input = "6\nPROD001\ny\n\n0\n"; // Delete, code, confirm, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            verify(productManagementUseCase).deleteProduct("PROD001");
            String output = outputStream.toString();
            assertTrue(output.contains("Product deleted successfully"));
        }

        @Test
        @DisplayName("Should handle delete confirmation cancellation")
        void shouldHandleDeleteConfirmationCancellation() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Test Product",
                        domain.shared.Money.of(new BigDecimal("99.99"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));

            String input = "6\nPROD001\nn\n\n0\n"; // Delete, code, cancel, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            verify(productManagementUseCase, never()).deleteProduct("PROD001");
            String output = outputStream.toString();
            assertTrue(output.contains("Delete operation cancelled"));
        }

        @Test
        @DisplayName("Should handle delete product not found")
        void shouldHandleDeleteProductNotFound() {
            // Given
            when(productManagementUseCase.findProduct("NONEXISTENT")).thenReturn(Optional.empty());

            String input = "6\nNONEXISTENT\n\n0\n"; // Delete, non-existent code, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("NONEXISTENT");
            verify(productManagementUseCase, never()).deleteProduct(anyString());
            String output = outputStream.toString();
            assertTrue(output.contains("Product not found with code: NONEXISTENT"));
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product name only")
        void shouldUpdateProductNameOnly() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Old Product",
                        domain.shared.Money.of(new BigDecimal("50.00"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.UPDATED);

            String input = "3\nPROD001\n1\nNew Product Name\n0\n"; // Update, code, name only, new name, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            verify(productManagementUseCase).createProduct(argThat(request ->
                request.code().equals("PROD001") &&
                request.name().equals("New Product Name") &&
                request.price().equals(new BigDecimal("50.00"))
            ));
            String output = outputStream.toString();
            assertTrue(output.contains("Product name updated successfully"));
        }

        @Test
        @DisplayName("Should update product price only")
        void shouldUpdateProductPriceOnly() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Test Product",
                        domain.shared.Money.of(new BigDecimal("50.00"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.UPDATED);

            String input = "3\nPROD001\n2\n75.50\n0\n"; // Update, code, price only, new price, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            verify(productManagementUseCase).createProduct(argThat(request ->
                request.code().equals("PROD001") &&
                request.name().equals("Test Product") &&
                request.price().equals(new BigDecimal("75.50"))
            ));
            String output = outputStream.toString();
            assertTrue(output.contains("Product price updated successfully"));
        }

        @Test
        @DisplayName("Should update both name and price")
        void shouldUpdateBothNameAndPrice() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Old Product",
                        domain.shared.Money.of(new BigDecimal("50.00"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.UPDATED);

            String input = "3\nPROD001\n3\nNew Product\n99.99\n0\n"; // Update, code, both, new name, new price, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            verify(productManagementUseCase).createProduct(argThat(request ->
                request.code().equals("PROD001") &&
                request.name().equals("New Product") &&
                request.price().equals(new BigDecimal("99.99"))
            ));
            String output = outputStream.toString();
            assertTrue(output.contains("Product updated successfully"));
        }

        @Test
        @DisplayName("Should cancel update operation")
        void shouldCancelUpdateOperation() {
            // Given
            ProductManagementUseCase.ProductInfo productInfo =
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Test Product",
                        domain.shared.Money.of(new BigDecimal("50.00"))
                    )
                );

            when(productManagementUseCase.findProduct("PROD001")).thenReturn(Optional.of(productInfo));

            String input = "3\nPROD001\n0\n0\n"; // Update, code, cancel, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("PROD001");
            verify(productManagementUseCase, never()).createProduct(any());
            String output = outputStream.toString();
            assertTrue(output.contains("Update cancelled"));
        }

        @Test
        @DisplayName("Should handle update product not found")
        void shouldHandleUpdateProductNotFound() {
            // Given
            when(productManagementUseCase.findProduct("NONEXISTENT")).thenReturn(Optional.empty());

            String input = "3\nNONEXISTENT\n\n0\n"; // Update, non-existent code, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).findProduct("NONEXISTENT");
            verify(productManagementUseCase, never()).createProduct(any());
            String output = outputStream.toString();
            assertTrue(output.contains("Product not found with code: NONEXISTENT"));
        }
    }

    @Nested
    @DisplayName("View All Products Tests")
    class ViewAllProductsTests {

        @Test
        @DisplayName("Should display all products")
        void shouldDisplayAllProducts() {
            // Given
            List<ProductManagementUseCase.ProductInfo> products = List.of(
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD001"),
                        "Product One",
                        domain.shared.Money.of(new BigDecimal("10.50"))
                    )
                ),
                new ProductManagementUseCase.ProductInfo(
                    new domain.product.Product(
                        new domain.shared.Code("PROD002"),
                        "Product Two",
                        domain.shared.Money.of(new BigDecimal("25.75"))
                    )
                )
            );

            when(productManagementUseCase.listAllProducts()).thenReturn(products);

            String input = "5\n\n0\n"; // View all, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).listAllProducts();
            String output = outputStream.toString();
            assertTrue(output.contains("ALL PRODUCTS"));
            assertTrue(output.contains("PROD001"));
            assertTrue(output.contains("Product One"));
            assertTrue(output.contains("PROD002"));
            assertTrue(output.contains("Product Two"));
            assertTrue(output.contains("Total products: 2"));
        }

        @Test
        @DisplayName("Should handle no products found")
        void shouldHandleNoProductsFound() {
            // Given
            when(productManagementUseCase.listAllProducts()).thenReturn(List.of());

            String input = "5\n\n0\n"; // View all, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            verify(productManagementUseCase).listAllProducts();
            String output = outputStream.toString();
            assertTrue(output.contains("No products found in the system"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid menu option")
        void shouldHandleInvalidMenuOption() {
            // Given
            String input = "999\n\n0\n"; // Invalid option, press enter, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid option. Please try again."));
        }

        @Test
        @DisplayName("Should handle negative price input")
        void shouldHandleNegativePriceInput() {
            // Given
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.SUCCESS);

            String input = "2\nPROD001\nTest Product\n-10.00\n15.00\n\n0\n"; // Manual code, negative price, then valid price
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Price must be positive"));
        }

        @Test
        @DisplayName("Should handle zero price input")
        void shouldHandleZeroPriceInput() {
            // Given
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.SUCCESS);

            String input = "2\nPROD001\nTest Product\n0.00\n15.00\n\n0\n"; // Manual code, zero price, then valid price
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Price must be positive"));
        }

        @Test
        @DisplayName("Should handle empty product name")
        void shouldHandleEmptyProductName() {
            // Given
            when(productManagementUseCase.createProduct(any())).thenReturn(
                ProductManagementUseCase.CreateResult.SUCCESS);

            String input = "2\nPROD001\n\nValid Product\n15.00\n\n0\n"; // Manual code, empty name, then valid name
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            productManagementCLI = new ProductManagementCLI(productManagementUseCase, categoryManagementUseCase);
            productManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Product name cannot be empty"));
        }
    }

    // Helper method to create input with enough newlines for any CLI flow
    private String createInput(String... inputs) {
        StringBuilder sb = new StringBuilder();
        for (String input : inputs) {
            sb.append(input).append("\n");
        }
        // Add extra newlines to handle multiple pressEnterToContinue calls
        sb.append("\n\n\n\n\n");
        return sb.toString();
    }
}
