package domain.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import static org.junit.jupiter.api.Assertions.*;

import domain.product.Category;

@DisplayName("Category Domain Entity Tests")
class CategoryTest {

    @Test
    @DisplayName("Should create Category with valid parameters")
    void shouldCreateCategoryWithValidParameters() {
        // Given
        String code = "ELEC";
        String name = "Electronics";
        String description = "Electronic devices and gadgets";
        String prefix = "EL";
        int nextSequence = 1;
        int displayOrder = 1;
        boolean active = true;

        // When
        Category category = new Category(code, name, description, prefix, nextSequence, displayOrder, active);

        // Then
        assertEquals(code, category.code());
        assertEquals(name, category.name());
        assertEquals(description, category.description());
        assertEquals(prefix, category.prefix());
        assertEquals(nextSequence, category.nextSequence());
        assertEquals(displayOrder, category.displayOrder());
        assertEquals(active, category.active());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid category codes")
    void shouldThrowExceptionForInvalidCategoryCodes(String invalidCode) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Category(invalidCode, "Electronics", "Description", "EL", 1, 1, true));
        assertEquals("code is null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid category names")
    void shouldThrowExceptionForInvalidCategoryNames(String invalidName) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Category("ELEC", invalidName, "Description", "EL", 1, 1, true));
        assertEquals("name is null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid prefixes")
    void shouldThrowExceptionForInvalidPrefixes(String invalidPrefix) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Category("ELEC", "Electronics", "Description", invalidPrefix, 1, 1, true));
        assertEquals("prefix is null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow null description")
    void shouldAllowNullDescription() {
        // When
        Category category = new Category("ELEC", "Electronics", null, "EL", 1, 1, true);

        // Then
        assertEquals("ELEC", category.code());
        assertEquals("Electronics", category.name());
        assertNull(category.description());
    }

    @Test
    @DisplayName("Should generate next product code")
    void shouldGenerateNextProductCode() {
        // Given
        Category category = new Category("ELEC", "Electronics", "Description", "EL", 5, 1, true);

        // When
        String productCode = category.generateNextProductCode();

        // Then
        assertEquals("EL005", productCode);
    }

    @Test
    @DisplayName("Should increment sequence")
    void shouldIncrementSequence() {
        // Given
        Category category = new Category("ELEC", "Electronics", "Description", "EL", 5, 1, true);

        // When
        Category incrementedCategory = category.withIncrementedSequence();

        // Then
        assertEquals(5, category.nextSequence()); // Original unchanged
        assertEquals(6, incrementedCategory.nextSequence()); // New instance incremented
        assertEquals(category.code(), incrementedCategory.code());
        assertEquals(category.name(), incrementedCategory.name());
    }

    @Test
    @DisplayName("Should convert prefix to uppercase")
    void shouldConvertPrefixToUppercase() {
        // When
        Category category = new Category("ELEC", "Electronics", "Description", "el", 1, 1, true);

        // Then
        assertEquals("EL", category.prefix());
    }

    @Test
    @DisplayName("Should trim whitespace from inputs")
    void shouldTrimWhitespaceFromInputs() {
        // When
        Category category = new Category("  ELEC  ", "  Electronics  ", "  Description  ", "  EL  ", 1, 1, true);

        // Then
        assertEquals("ELEC", category.code());
        assertEquals("Electronics", category.name());
        assertEquals("Description", category.description());
        assertEquals("EL", category.prefix());
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        Category category = new Category("ELEC", "Electronics", "Description", "EL", 1, 1, true);

        // When
        String result = category.toString();

        // Then
        assertEquals("ELEC - Electronics", result);
    }
}
