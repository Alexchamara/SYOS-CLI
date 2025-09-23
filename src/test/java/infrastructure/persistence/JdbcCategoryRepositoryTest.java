package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcCategoryRepository;
import domain.product.Category;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Optional;

@DisplayName("JdbcCategoryRepository Tests")
class JdbcCategoryRepositoryTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcCategoryRepository categoryRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        categoryRepository = new JdbcCategoryRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Nested
    @DisplayName("Find All Active Tests")
    class FindAllActiveTests {

        @Test
        @DisplayName("Should find all active categories")
        void shouldFindAllActiveCategories() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            setupMockResultSet("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true);
            setupMockResultSet("FOOD", "Food", "Food items", "FD", 1, 2, true);

            // When
            List<Category> result = categoryRepository.findAllActive();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(connection).prepareStatement(contains("WHERE active = TRUE"));
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Should return empty list when no active categories")
        void shouldReturnEmptyListWhenNoActiveCategories() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            List<Category> result = categoryRepository.findAllActive();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should find all categories including inactive")
        void shouldFindAllCategoriesIncludingInactive() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            setupMockResultSet("ELEC", "Electronics", "Electronic devices", "EL", 1, 1, true);
            setupMockResultSet("OLD", "Discontinued", "Old category", "OL", 1, 3, false);

            // When
            List<Category> result = categoryRepository.findAll();

            // Then
            assertEquals(2, result.size());
            verify(connection).prepareStatement(contains("ORDER BY display_order"));
        }
    }

    @Nested
    @DisplayName("Find By Code Tests")
    class FindByCodeTests {

        @Test
        @DisplayName("Should find category by code successfully")
        void shouldFindCategoryByCodeSuccessfully() throws SQLException {
            // Given
            String code = "ELEC";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            setupMockResultSet("ELEC", "Electronics", "Electronic devices", "EL", 5, 1, true);

            // When
            Optional<Category> result = categoryRepository.findByCode(code);

            // Then
            assertTrue(result.isPresent());
            Category category = result.get();
            assertEquals("ELEC", category.code());
            assertEquals("Electronics", category.name());
            assertEquals(5, category.nextSequence());

            verify(preparedStatement).setString(1, code);
        }

        @Test
        @DisplayName("Should return empty when category not found")
        void shouldReturnEmptyWhenCategoryNotFound() throws SQLException {
            // Given
            String code = "NONEXISTENT";
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            Optional<Category> result = categoryRepository.findByCode(code);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during find by code")
        void shouldHandleSQLExceptionsDuringFindByCode() throws SQLException {
            // Given
            String code = "ELEC";
            SQLException sqlException = new SQLException("Query failed");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryRepository.findByCode(code));
            assertTrue(exception.getMessage().contains("Failed to find category by code"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Save Category Tests")
    class SaveCategoryTests {

        @Test
        @DisplayName("Should save category successfully")
        void shouldSaveCategorySuccessfully() throws SQLException {
            // Given
            Category category = new Category("BOOK", "Books", "Books and magazines", "BK", 1, 3, true);

            // When
            categoryRepository.save(category);

            // Then
            verify(connection).prepareStatement(contains("INSERT INTO category"));
            verify(preparedStatement).setString(1, "BOOK");
            verify(preparedStatement).setString(2, "Books");
            verify(preparedStatement).setString(3, "Books and magazines");
            verify(preparedStatement).setString(4, "BK");
            verify(preparedStatement).setInt(5, 1);
            verify(preparedStatement).setInt(6, 3);
            verify(preparedStatement).setBoolean(7, true);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should handle null description in save")
        void shouldHandleNullDescriptionInSave() throws SQLException {
            // Given
            Category category = new Category("MISC", "Miscellaneous", null, "MS", 1, 4, true);

            // When
            categoryRepository.save(category);

            // Then
            verify(preparedStatement).setString(3, null);
        }

        @Test
        @DisplayName("Should handle SQL exceptions during save")
        void shouldHandleSQLExceptionsDuringSave() throws SQLException {
            // Given
            Category category = new Category("TEST", "Test", "Description", "TS", 1, 1, true);
            SQLException sqlException = new SQLException("Constraint violation");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryRepository.save(category));
            assertTrue(exception.getMessage().contains("Failed to save category"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategorySuccessfully() throws SQLException {
            // Given
            String code = "ELEC";
            when(preparedStatement.executeUpdate()).thenReturn(1); // One row updated

            // When
            categoryRepository.delete(code);

            // Then
            verify(connection).prepareStatement(contains("UPDATE category SET active = FALSE"));
            verify(preparedStatement).setString(1, code);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Should throw exception when category not found for delete")
        void shouldThrowExceptionWhenCategoryNotFoundForDelete() throws SQLException {
            // Given
            String code = "NONEXISTENT";
            when(preparedStatement.executeUpdate()).thenReturn(0); // No rows updated

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryRepository.delete(code));
            assertEquals("Category not found: NONEXISTENT", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during delete")
        void shouldHandleSQLExceptionsDuringDelete() throws SQLException {
            // Given
            String code = "ELEC";
            SQLException sqlException = new SQLException("Update failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryRepository.delete(code));
            assertTrue(exception.getMessage().contains("Failed to delete category"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Increment Sequence Tests")
    class IncrementSequenceTests {

        @Test
        @DisplayName("Should increment sequence and return updated category")
        void shouldIncrementSequenceAndReturnUpdatedCategory() throws SQLException {
            // Given
            String categoryCode = "ELEC";
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Mock the findByCode call that happens after increment
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            setupMockResultSet("ELEC", "Electronics", "Electronic devices", "EL", 6, 1, true);

            // When
            Category result = categoryRepository.incrementSequenceAndSave(categoryCode);

            // Then
            assertNotNull(result);
            assertEquals("ELEC", result.code());
            assertEquals(6, result.nextSequence()); // Should be incremented

            verify(connection, times(2)).prepareStatement(anyString()); // Update + Select
        }

        @Test
        @DisplayName("Should throw exception when category not found for increment")
        void shouldThrowExceptionWhenCategoryNotFoundForIncrement() throws SQLException {
            // Given
            String categoryCode = "NONEXISTENT";
            when(preparedStatement.executeUpdate()).thenReturn(0); // No rows updated

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryRepository.incrementSequenceAndSave(categoryCode));
            assertEquals("Category not found: NONEXISTENT", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle SQL exceptions during increment")
        void shouldHandleSQLExceptionsDuringIncrement() throws SQLException {
            // Given
            String categoryCode = "ELEC";
            SQLException sqlException = new SQLException("Update failed");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryRepository.incrementSequenceAndSave(categoryCode));
            assertTrue(exception.getMessage().contains("Failed to increment sequence"));
            assertEquals(sqlException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly in find operations")
        void shouldCloseResourcesProperlyInFindOperations() throws SQLException {
            // Given
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // When
            categoryRepository.findAll();

            // Then
            verify(resultSet).close();
            verify(preparedStatement).close();
            verify(connection).close();
        }
    }

    // Helper method to setup mock ResultSet
    private void setupMockResultSet(String code, String name, String description, String prefix,
                                   int nextSequence, int displayOrder, boolean active) throws SQLException {
        when(resultSet.getString("code")).thenReturn(code);
        when(resultSet.getString("name")).thenReturn(name);
        when(resultSet.getString("description")).thenReturn(description);
        when(resultSet.getString("prefix")).thenReturn(prefix);
        when(resultSet.getInt("next_sequence")).thenReturn(nextSequence);
        when(resultSet.getInt("display_order")).thenReturn(displayOrder);
        when(resultSet.getBoolean("active")).thenReturn(active);
    }
}
