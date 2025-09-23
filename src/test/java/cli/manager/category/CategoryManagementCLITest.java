package cli.manager.category;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cli.manager.category.CategoryManagementCLI;
import application.usecase.CategoryManagementUseCase;
import domain.product.Category;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

@DisplayName("CategoryManagementCLI Tests")
class CategoryManagementCLITest {

    @Mock
    private CategoryManagementUseCase categoryManagementUseCase;

    private CategoryManagementCLI categoryManagementCLI;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        categoryManagementCLI = new CategoryManagementCLI(categoryManagementUseCase);

        // Capture System.out and System.in for testing
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;
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
        @DisplayName("Should display category management menu")
        void shouldDisplayCategoryManagementMenu() {
            // Given
            String input = "0\n"; // Exit immediately
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("CATEGORY MANAGEMENT"));
            assertTrue(output.contains("1. Add New Category"));
            assertTrue(output.contains("2. Update Category"));
            assertTrue(output.contains("3. View Category Details"));
            assertTrue(output.contains("4. View All Categories"));
            assertTrue(output.contains("5. Delete Category"));
            assertTrue(output.contains("0. Back to Manager Menu"));
        }
    }

    @Nested
    @DisplayName("Add Category Tests")
    class AddCategoryTests {

        @Test
        @DisplayName("Should add category successfully")
        void shouldAddCategorySuccessfully() {
            // Given
            when(categoryManagementUseCase.createCategory(any())).thenReturn(
                CategoryManagementUseCase.CreateCategoryResult.SUCCESS);

            String input = "1\nELEC\nElectronics\nElectronic devices\nEL\n1\n0\n"; // Add category
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            verify(categoryManagementUseCase).createCategory(argThat(request ->
                request.code().equals("ELEC") &&
                request.name().equals("Electronics") &&
                request.description().equals("Electronic devices") &&
                request.prefix().equals("EL") &&
                request.displayOrder() == 1
            ));

            String output = outputStream.toString();
            assertTrue(output.contains("Category created successfully"));
        }

        @Test
        @DisplayName("Should handle category update scenario")
        void shouldHandleCategoryUpdateScenario() {
            // Given
            when(categoryManagementUseCase.createCategory(any())).thenReturn(
                CategoryManagementUseCase.CreateCategoryResult.UPDATED);

            String input = "1\nELEC\nElectronics Updated\nUpdated description\nEL\n1\n0\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Category updated successfully"));
        }

        @Test
        @DisplayName("Should handle empty category code")
        void shouldHandleEmptyCategoryCode() {
            // Given
            String input = "1\n\nVALID\nValid Category\nDescription\nVL\n1\n0\n"; // Empty then valid code
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(categoryManagementUseCase.createCategory(any())).thenReturn(
                CategoryManagementUseCase.CreateCategoryResult.SUCCESS);

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Category code cannot be empty"));
        }

        @Test
        @DisplayName("Should handle invalid display order")
        void shouldHandleInvalidDisplayOrder() {
            // Given
            String input = "1\nTEST\nTest Category\nDescription\nTS\nabc\n2\n0\n"; // Invalid then valid order
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            when(categoryManagementUseCase.createCategory(any())).thenReturn(
                CategoryManagementUseCase.CreateCategoryResult.SUCCESS);

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid display order"));
        }
    }

    @Nested
    @DisplayName("View Category Tests")
    class ViewCategoryTests {

        @Test
        @DisplayName("Should view category details")
        void shouldViewCategoryDetails() {
            // Given
            Category category = new Category("ELEC", "Electronics", "Electronic devices", "EL", 5, 1, true);
            when(categoryManagementUseCase.findCategory("ELEC")).thenReturn(Optional.of(category));

            String input = "3\nELEC\n0\n"; // View category, enter code, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            verify(categoryManagementUseCase).findCategory("ELEC");
            String output = outputStream.toString();
            assertTrue(output.contains("Category Details:"));
            assertTrue(output.contains("Code: ELEC"));
            assertTrue(output.contains("Name: Electronics"));
            assertTrue(output.contains("Description: Electronic devices"));
            assertTrue(output.contains("Prefix: EL"));
            assertTrue(output.contains("Next Sequence: 5"));
        }

        @Test
        @DisplayName("Should handle category not found")
        void shouldHandleCategoryNotFound() {
            // Given
            when(categoryManagementUseCase.findCategory("NONEXISTENT")).thenReturn(Optional.empty());

            String input = "3\nNONEXISTENT\n0\n"; // View non-existent category
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Category not found"));
        }
    }

    @Nested
    @DisplayName("View All Categories Tests")
    class ViewAllCategoriesTests {

        @Test
        @DisplayName("Should display all categories")
        void shouldDisplayAllCategories() {
            // Given
            List<Category> categories = List.of(
                new Category("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true),
                new Category("FOOD", "Food", "Food items", "FD", 1, 2, false)
            );

            when(categoryManagementUseCase.getAllCategories()).thenReturn(categories);

            String input = "4\n0\n"; // View all categories, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            verify(categoryManagementUseCase).getAllCategories();
            String output = outputStream.toString();
            assertTrue(output.contains("All Categories:"));
            assertTrue(output.contains("ELEC"));
            assertTrue(output.contains("FOOD"));
            assertTrue(output.contains("Electronics"));
            assertTrue(output.contains("Food"));
            assertTrue(output.contains("Active: true"));
            assertTrue(output.contains("Active: false"));
        }

        @Test
        @DisplayName("Should handle empty category list")
        void shouldHandleEmptyCategoryList() {
            // Given
            when(categoryManagementUseCase.getAllCategories()).thenReturn(List.of());

            String input = "4\n0\n"; // View all categories, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("No categories found"));
        }
    }

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategorySuccessfully() {
            // Given
            String input = "5\nELEC\ny\n0\n"; // Delete, code, confirm, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            verify(categoryManagementUseCase).deleteCategory("ELEC");
            String output = outputStream.toString();
            assertTrue(output.contains("Category deleted successfully"));
        }

        @Test
        @DisplayName("Should handle delete confirmation cancellation")
        void shouldHandleDeleteConfirmationCancellation() {
            // Given
            String input = "5\nELEC\nn\n0\n"; // Delete, code, cancel, exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            verifyNoInteractions(categoryManagementUseCase);
            String output = outputStream.toString();
            assertTrue(output.contains("Delete cancelled"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle invalid menu choices")
        void shouldHandleInvalidMenuChoices() {
            // Given
            String input = "99\nabc\n-1\n0\n"; // Invalid choices, then exit
            System.setIn(new ByteArrayInputStream(input.getBytes()));

            // When
            categoryManagementCLI.run();

            // Then
            String output = outputStream.toString();
            long invalidMessages = output.lines()
                .filter(line -> line.contains("Invalid option"))
                .count();
            assertTrue(invalidMessages >= 3);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create CategoryManagementCLI with required dependencies")
        void shouldCreateCategoryManagementCLIWithRequiredDependencies() {
            // When
            CategoryManagementCLI cli = new CategoryManagementCLI(categoryManagementUseCase);

            // Then
            assertNotNull(cli);
        }
    }
}
