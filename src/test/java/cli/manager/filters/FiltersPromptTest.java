package cli.manager.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import cli.manager.filters.FiltersPrompt;
import cli.manager.filters.ReportFilters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;

@DisplayName("FiltersPrompt Tests")
class FiltersPromptTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Nested
    @DisplayName("Date Filter Prompt Tests")
    class DateFilterPromptTests {

        @Test
        @DisplayName("Should prompt for single day filter")
        void shouldPromptForSingleDayFilter() {
            // Given
            String input = "1\n2025-09-22\n"; // Single day option, specific date
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            ReportFilters result = FiltersPrompt.promptForDateFilter(scanner);

            // Then
            assertEquals(ReportFilters.DateMode.SINGLE_DAY, result.dateMode());
            assertEquals(LocalDate.of(2025, 9, 22), result.day());
            assertNull(result.fromDate());
            assertNull(result.toDate());

            String output = outputStream.toString();
            assertTrue(output.contains("Date Filter Options:"));
            assertTrue(output.contains("1. Single Day"));
            assertTrue(output.contains("2. Date Range"));
            scanner.close();
        }

        @Test
        @DisplayName("Should prompt for date range filter")
        void shouldPromptForDateRangeFilter() {
            // Given
            String input = "2\n2025-09-15\n2025-09-22\n"; // Date range option, from, to
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            ReportFilters result = FiltersPrompt.promptForDateFilter(scanner);

            // Then
            assertEquals(ReportFilters.DateMode.DATE_RANGE, result.dateMode());
            assertEquals(LocalDate.of(2025, 9, 15), result.fromDate());
            assertEquals(LocalDate.of(2025, 9, 22), result.toDate());
            assertNull(result.day());

            String output = outputStream.toString();
            assertTrue(output.contains("Enter from date"));
            assertTrue(output.contains("Enter to date"));
            scanner.close();
        }

        @Test
        @DisplayName("Should handle today option for single day")
        void shouldHandleTodayOptionForSingleDay() {
            // Given
            String input = "1\ntoday\n"; // Single day option, today keyword
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            ReportFilters result = FiltersPrompt.promptForDateFilter(scanner);

            // Then
            assertEquals(ReportFilters.DateMode.SINGLE_DAY, result.dateMode());
            assertEquals(LocalDate.now(), result.day());
            scanner.close();
        }

        @Test
        @DisplayName("Should handle invalid date filter choice")
        void shouldHandleInvalidDateFilterChoice() {
            // Given
            String input = "99\n1\ntoday\n"; // Invalid choice, then valid
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            ReportFilters result = FiltersPrompt.promptForDateFilter(scanner);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid choice"));
            assertEquals(ReportFilters.DateMode.SINGLE_DAY, result.dateMode());
            scanner.close();
        }

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String input = "1\ninvalid-date\n2025-09-22\n"; // Invalid then valid date
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            ReportFilters result = FiltersPrompt.promptForDateFilter(scanner);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format"));
            assertEquals(LocalDate.of(2025, 9, 22), result.day());
            scanner.close();
        }

        @Test
        @DisplayName("Should handle invalid date range")
        void shouldHandleInvalidDateRange() {
            // Given
            String input = "2\n2025-09-22\n2025-09-15\n2025-09-15\n2025-09-22\n"; // End before start, then valid
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            ReportFilters result = FiltersPrompt.promptForDateFilter(scanner);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("End date must be after start date"));
            assertEquals(LocalDate.of(2025, 9, 15), result.fromDate());
            assertEquals(LocalDate.of(2025, 9, 22), result.toDate());
            scanner.close();
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("Should parse date correctly")
        void shouldParseDateCorrectly() {
            // Given
            String dateString = "2025-09-22";

            // When
            LocalDate result = FiltersPrompt.parseDate(dateString);

            // Then
            assertEquals(LocalDate.of(2025, 9, 22), result);
        }

        @Test
        @DisplayName("Should handle today keyword")
        void shouldHandleTodayKeyword() {
            // Given
            String todayString = "today";

            // When
            LocalDate result = FiltersPrompt.parseDate(todayString);

            // Then
            assertEquals(LocalDate.now(), result);
        }

        @Test
        @DisplayName("Should handle yesterday keyword")
        void shouldHandleYesterdayKeyword() {
            // Given
            String yesterdayString = "yesterday";

            // When
            LocalDate result = FiltersPrompt.parseDate(yesterdayString);

            // Then
            assertEquals(LocalDate.now().minusDays(1), result);
        }

        @Test
        @DisplayName("Should throw exception for invalid date format")
        void shouldThrowExceptionForInvalidDateFormat() {
            // Given
            String invalidDate = "invalid-date";

            // When & Then
            assertThrows(java.time.format.DateTimeParseException.class,
                () -> FiltersPrompt.parseDate(invalidDate));
        }

        @Test
        @DisplayName("Should handle null date string")
        void shouldHandleNullDateString() {
            // Given
            String nullDate = null;

            // When & Then
            assertThrows(NullPointerException.class,
                () -> FiltersPrompt.parseDate(nullDate));
        }

        @Test
        @DisplayName("Should handle empty date string")
        void shouldHandleEmptyDateString() {
            // Given
            String emptyDate = "";

            // When & Then
            assertThrows(java.time.format.DateTimeParseException.class,
                () -> FiltersPrompt.parseDate(emptyDate));
        }
    }

    @Nested
    @DisplayName("User Experience Tests")
    class UserExperienceTests {

        @Test
        @DisplayName("Should provide clear prompts")
        void shouldProvideClearPrompts() {
            // Given
            String input = "1\ntoday\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            FiltersPrompt.promptForDateFilter(scanner);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Date Filter Options:"));
            assertTrue(output.contains("Choose filter type"));
            assertTrue(output.contains("Enter date"));
            scanner.close();
        }

        @Test
        @DisplayName("Should show date format examples")
        void shouldShowDateFormatExamples() {
            // Given
            String input = "1\ntoday\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            FiltersPrompt.promptForDateFilter(scanner);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("yyyy-mm-dd") || output.contains("today") || output.contains("yesterday"));
            scanner.close();
        }

        @Test
        @DisplayName("Should provide helpful error messages")
        void shouldProvideHelpfulErrorMessages() {
            // Given
            String input = "1\nwrong-format\n2025-09-22\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            Scanner scanner = new Scanner(System.in);

            // When
            FiltersPrompt.promptForDateFilter(scanner);

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Invalid date format") || output.contains("Please use"));
            scanner.close();
        }
    }

    @Nested
    @DisplayName("Static Utility Method Tests")
    class StaticUtilityMethodTests {

        @Test
        @DisplayName("Should be static utility methods")
        void shouldBeStaticUtilityMethods() {
            // Given
            Class<FiltersPrompt> filtersPromptClass = FiltersPrompt.class;

            // When
            java.lang.reflect.Method[] methods = filtersPromptClass.getDeclaredMethods();

            // Then
            boolean hasStaticMethods = java.util.Arrays.stream(methods)
                .anyMatch(method -> java.lang.reflect.Modifier.isStatic(method.getModifiers()));
            assertTrue(hasStaticMethods);
        }

        @Test
        @DisplayName("Should not be instantiable")
        void shouldNotBeInstantiable() {
            // Given
            Class<FiltersPrompt> filtersPromptClass = FiltersPrompt.class;

            // When
            java.lang.reflect.Constructor<?>[] constructors = filtersPromptClass.getDeclaredConstructors();

            // Then
            boolean hasPublicConstructor = java.util.Arrays.stream(constructors)
                .anyMatch(constructor -> java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
            assertFalse(hasPublicConstructor);
        }
    }
}
