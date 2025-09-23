package domain.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.repository.CategoryRepository;
import domain.product.Category;
import java.util.List;
import java.util.Optional;

@DisplayName("CategoryRepository Domain Interface Tests")
class CategoryRepositoryTest {

    private CategoryRepository categoryRepository;
    private Category category;
    private String categoryCode;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        category = mock(Category.class);
        categoryCode = "CAT001";
    }

    @Test
    @DisplayName("Should define contract for saving categories")
    void shouldDefineContractForSavingCategories() {
        // When
        categoryRepository.save(category);

        // Then
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("Should define contract for finding category by code")
    void shouldDefineContractForFindingCategoryByCode() {
        // Given
        when(categoryRepository.findByCode(categoryCode)).thenReturn(Optional.of(category));

        // When
        Optional<Category> foundCategory = categoryRepository.findByCode(categoryCode);

        // Then
        assertTrue(foundCategory.isPresent());
        assertEquals(category, foundCategory.get());
        verify(categoryRepository).findByCode(categoryCode);
    }

    @Test
    @DisplayName("Should define contract for finding all categories")
    void shouldDefineContractForFindingAllCategories() {
        // Given
        List<Category> expectedCategories = List.of(category);
        when(categoryRepository.findAll()).thenReturn(expectedCategories);

        // When
        List<Category> categories = categoryRepository.findAll();

        // Then
        assertEquals(expectedCategories, categories);
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should define contract for finding active categories")
    void shouldDefineContractForFindingActiveCategories() {
        // Given
        List<Category> expectedCategories = List.of(category);
        when(categoryRepository.findAllActive()).thenReturn(expectedCategories);

        // When
        List<Category> categories = categoryRepository.findAllActive();

        // Then
        assertEquals(expectedCategories, categories);
        verify(categoryRepository).findAllActive();
    }

    @Test
    @DisplayName("Should define contract for deleting category by code")
    void shouldDefineContractForDeletingCategoryByCode() {
        // When
        categoryRepository.delete(categoryCode);

        // Then
        verify(categoryRepository).delete(categoryCode);
    }

    @Test
    @DisplayName("Should define contract for incrementing sequence and saving")
    void shouldDefineContractForIncrementSequenceAndSave() {
        // Given
        when(categoryRepository.incrementSequenceAndSave(categoryCode)).thenReturn(category);

        // When
        Category saved = categoryRepository.incrementSequenceAndSave(categoryCode);

        // Then
        assertEquals(category, saved);
        verify(categoryRepository).incrementSequenceAndSave(categoryCode);
    }
}
