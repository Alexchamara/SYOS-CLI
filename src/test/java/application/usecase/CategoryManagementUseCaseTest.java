package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.CategoryManagementUseCase;
import application.usecase.CategoryManagementUseCase.CreateCategoryRequest;
import application.usecase.CategoryManagementUseCase.UpdateCategoryRequest;
import application.usecase.CategoryManagementUseCase.CreateCategoryResult;
import domain.product.Category;
import domain.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;

@DisplayName("CategoryManagementUseCase Tests")
class CategoryManagementUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryManagementUseCase categoryManagementUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryManagementUseCase = new CategoryManagementUseCase(categoryRepository);
    }

    @Nested
    @DisplayName("Get Categories Tests")
    class GetCategoriesTests {

        @Test
        @DisplayName("Should get all active categories")
        void shouldGetAllActiveCategories() {
            // Given
            List<Category> activeCategories = List.of(
                new Category("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true),
                new Category("FOOD", "Food", "Food items", "FD", 1, 2, true)
            );
            when(categoryRepository.findAllActive()).thenReturn(activeCategories);

            // When
            List<Category> result = categoryManagementUseCase.getAllActiveCategories();

            // Then
            assertEquals(2, result.size());
            assertEquals(activeCategories, result);
            verify(categoryRepository).findAllActive();
        }

        @Test
        @DisplayName("Should get all categories including inactive")
        void shouldGetAllCategoriesIncludingInactive() {
            // Given
            List<Category> allCategories = List.of(
                new Category("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true),
                new Category("BOOK", "Books", "Books and magazines", "BK", 1, 2, false)
            );
            when(categoryRepository.findAll()).thenReturn(allCategories);

            // When
            List<Category> result = categoryManagementUseCase.getAllCategories();

            // Then
            assertEquals(2, result.size());
            assertEquals(allCategories, result);
            verify(categoryRepository).findAll();
        }

        @Test
        @DisplayName("Should find category by code")
        void shouldFindCategoryByCode() {
            // Given
            String code = "ELEC";
            Category category = new Category(code, "Electronics", "Electronic devices", "EL", 1, 1, true);
            when(categoryRepository.findByCode(code)).thenReturn(Optional.of(category));

            // When
            Optional<Category> result = categoryManagementUseCase.findCategory(code);

            // Then
            assertTrue(result.isPresent());
            assertEquals(category, result.get());
            verify(categoryRepository).findByCode(code);
        }

        @Test
        @DisplayName("Should return empty when category not found")
        void shouldReturnEmptyWhenCategoryNotFound() {
            // Given
            String code = "NONEXISTENT";
            when(categoryRepository.findByCode(code)).thenReturn(Optional.empty());

            // When
            Optional<Category> result = categoryManagementUseCase.findCategory(code);

            // Then
            assertFalse(result.isPresent());
            verify(categoryRepository).findByCode(code);
        }
    }

    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create new category successfully")
        void shouldCreateNewCategorySuccessfully() {
            // Given
            CreateCategoryRequest request = new CreateCategoryRequest(
                "ELEC", "Electronics", "Electronic devices", "EL", 1
            );
            when(categoryRepository.findByCode("ELEC")).thenReturn(Optional.empty());

            // When
            CreateCategoryResult result = categoryManagementUseCase.createCategory(request);

            // Then
            assertEquals(CreateCategoryResult.SUCCESS, result);
            verify(categoryRepository).findByCode("ELEC");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should update existing category")
        void shouldUpdateExistingCategory() {
            // Given
            CreateCategoryRequest request = new CreateCategoryRequest(
                "ELEC", "Electronics", "Electronic devices", "EL", 1
            );
            Category existingCategory = new Category("ELEC", "Old Name", "Old desc", "EL", 1, 1, true);
            when(categoryRepository.findByCode("ELEC")).thenReturn(Optional.of(existingCategory));

            // When
            CreateCategoryResult result = categoryManagementUseCase.createCategory(request);

            // Then
            assertEquals(CreateCategoryResult.UPDATED, result);
            verify(categoryRepository).findByCode("ELEC");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should create category with correct initial values")
        void shouldCreateCategoryWithCorrectInitialValues() {
            // Given
            CreateCategoryRequest request = new CreateCategoryRequest(
                "ELEC", "Electronics", "Electronic devices", "EL", 1
            );
            when(categoryRepository.findByCode("ELEC")).thenReturn(Optional.empty());

            // When
            categoryManagementUseCase.createCategory(request);

            // Then
            verify(categoryRepository).save(argThat(category ->
                category.code().equals("ELEC") &&
                category.name().equals("Electronics") &&
                category.description().equals("Electronic devices") &&
                category.prefix().equals("EL") &&
                category.nextSequence() == 1 &&
                category.displayOrder() == 1 &&
                category.active()
            ));
        }
    }

    @Nested
    @DisplayName("Update Category Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category with all fields")
        void shouldUpdateCategoryWithAllFields() {
            // Given
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                "ELEC", "New Electronics", "New description", 5, false
            );
            Category existingCategory = new Category("ELEC", "Electronics", "Electronic devices", "EL", 3, 1, true);
            when(categoryRepository.findByCode("ELEC")).thenReturn(Optional.of(existingCategory));

            // When
            categoryManagementUseCase.updateCategory(request);

            // Then
            verify(categoryRepository).save(argThat(category ->
                category.code().equals("ELEC") &&
                category.name().equals("New Electronics") &&
                category.description().equals("New description") &&
                category.prefix().equals("EL") && // Should preserve prefix
                category.nextSequence() == 3 && // Should preserve sequence
                category.displayOrder() == 5 &&
                !category.active()
            ));
        }

        @Test
        @DisplayName("Should update category with partial fields")
        void shouldUpdateCategoryWithPartialFields() {
            // Given
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                "ELEC", "New Name", null, null, null
            );
            Category existingCategory = new Category("ELEC", "Electronics", "Electronic devices", "EL", 3, 1, true);
            when(categoryRepository.findByCode("ELEC")).thenReturn(Optional.of(existingCategory));

            // When
            categoryManagementUseCase.updateCategory(request);

            // Then
            verify(categoryRepository).save(argThat(category ->
                category.code().equals("ELEC") &&
                category.name().equals("New Name") &&
                category.description().equals("Electronic devices") && // Should preserve
                category.displayOrder() == 1 && // Should preserve
                category.active() // Should preserve
            ));
        }

        @Test
        @DisplayName("Should throw exception when category not found for update")
        void shouldThrowExceptionWhenCategoryNotFoundForUpdate() {
            // Given
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                "NONEXISTENT", "New Name", null, null, null
            );
            when(categoryRepository.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryManagementUseCase.updateCategory(request));
            assertEquals("Category not found: NONEXISTENT", exception.getMessage());

            verify(categoryRepository).findByCode("NONEXISTENT");
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category")
        void shouldDeleteCategory() {
            // Given
            String code = "ELEC";

            // When
            categoryManagementUseCase.deleteCategory(code);

            // Then
            verify(categoryRepository).delete(code);
        }
    }

    @Nested
    @DisplayName("Generate Product Code Tests")
    class GenerateProductCodeTests {

        @Test
        @DisplayName("Should generate product code successfully")
        void shouldGenerateProductCodeSuccessfully() {
            // Given
            String categoryCode = "ELEC";
            Category category = new Category("ELEC", "Electronics", "Electronic devices", "EL", 5, 1, true);
            when(categoryRepository.findByCode(categoryCode)).thenReturn(Optional.of(category));

            // When
            String productCode = categoryManagementUseCase.generateProductCode(categoryCode);

            // Then
            assertEquals("EL005", productCode);
            verify(categoryRepository).findByCode(categoryCode);
            verify(categoryRepository).incrementSequenceAndSave(categoryCode);
        }

        @Test
        @DisplayName("Should throw exception when category not found for product code generation")
        void shouldThrowExceptionWhenCategoryNotFoundForProductCodeGeneration() {
            // Given
            String categoryCode = "NONEXISTENT";
            when(categoryRepository.findByCode(categoryCode)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryManagementUseCase.generateProductCode(categoryCode));
            assertEquals("Category not found: NONEXISTENT", exception.getMessage());

            verify(categoryRepository).findByCode(categoryCode);
            verify(categoryRepository, never()).incrementSequenceAndSave(anyString());
        }
    }

    @Nested
    @DisplayName("Request Record Tests")
    class RequestRecordTests {

        @Test
        @DisplayName("Should create CreateCategoryRequest correctly")
        void shouldCreateCreateCategoryRequestCorrectly() {
            // When
            CreateCategoryRequest request = new CreateCategoryRequest(
                "ELEC", "Electronics", "Electronic devices", "EL", 1
            );

            // Then
            assertEquals("ELEC", request.code());
            assertEquals("Electronics", request.name());
            assertEquals("Electronic devices", request.description());
            assertEquals("EL", request.prefix());
            assertEquals(1, request.displayOrder());
        }

        @Test
        @DisplayName("Should create UpdateCategoryRequest correctly")
        void shouldCreateUpdateCategoryRequestCorrectly() {
            // When
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                "ELEC", "New Name", "New desc", 5, false
            );

            // Then
            assertEquals("ELEC", request.code());
            assertEquals("New Name", request.name());
            assertEquals("New desc", request.description());
            assertEquals(5, request.displayOrder());
            assertEquals(false, request.active());
        }

        @Test
        @DisplayName("Should handle null values in UpdateCategoryRequest")
        void shouldHandleNullValuesInUpdateCategoryRequest() {
            // When
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                "ELEC", null, null, null, null
            );

            // Then
            assertEquals("ELEC", request.code());
            assertNull(request.name());
            assertNull(request.description());
            assertNull(request.displayOrder());
            assertNull(request.active());
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("Should have correct CreateCategoryResult values")
        void shouldHaveCorrectCreateCategoryResultValues() {
            // Then
            assertEquals(2, CreateCategoryResult.values().length);
            assertEquals(CreateCategoryResult.SUCCESS, CreateCategoryResult.valueOf("SUCCESS"));
            assertEquals(CreateCategoryResult.UPDATED, CreateCategoryResult.valueOf("UPDATED"));
        }
    }
}
