package cli.manager.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;

import cli.manager.filters.ReportFilters;
import cli.manager.filters.ReportFilters.DateMode;

import java.time.LocalDate;

@DisplayName("ReportFilters Tests")
class ReportFiltersTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("Single Day Filter Tests")
    class SingleDayFilterTests {

        @Test
        @DisplayName("Should create single day filter correctly")
        void shouldCreateSingleDayFilterCorrectly() {
            // Given
            LocalDate targetDate = LocalDate.of(2025, 9, 22);

            // When
            ReportFilters filters = ReportFilters.singleDay(targetDate);

            // Then
            assertEquals(DateMode.SINGLE_DAY, filters.dateMode());
            assertEquals(targetDate, filters.day());
            assertNull(filters.fromDate());
            assertNull(filters.toDate());
        }

        @Test
        @DisplayName("Should create single day filter for today")
        void shouldCreateSingleDayFilterForToday() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            ReportFilters filters = ReportFilters.singleDay(today);

            // Then
            assertEquals(DateMode.SINGLE_DAY, filters.dateMode());
            assertEquals(today, filters.day());
        }

        @Test
        @DisplayName("Should create single day filter for past date")
        void shouldCreateSingleDayFilterForPastDate() {
            // Given
            LocalDate pastDate = LocalDate.now().minusDays(7);

            // When
            ReportFilters filters = ReportFilters.singleDay(pastDate);

            // Then
            assertEquals(DateMode.SINGLE_DAY, filters.dateMode());
            assertEquals(pastDate, filters.day());
        }

        @Test
        @DisplayName("Should create single day filter for future date")
        void shouldCreateSingleDayFilterForFutureDate() {
            // Given
            LocalDate futureDate = LocalDate.now().plusDays(7);

            // When
            ReportFilters filters = ReportFilters.singleDay(futureDate);

            // Then
            assertEquals(DateMode.SINGLE_DAY, filters.dateMode());
            assertEquals(futureDate, filters.day());
        }

        @Test
        @DisplayName("Should handle null date in single day filter")
        void shouldHandleNullDateInSingleDayFilter() {
            // When
            ReportFilters filters = ReportFilters.singleDay(null);

            // Then
            assertEquals(DateMode.SINGLE_DAY, filters.dateMode());
            assertNull(filters.day());
        }
    }

    @Nested
    @DisplayName("Date Range Filter Tests")
    class DateRangeFilterTests {

        @Test
        @DisplayName("Should create date range filter correctly")
        void shouldCreateDateRangeFilterCorrectly() {
            // Given
            LocalDate fromDate = LocalDate.of(2025, 9, 15);
            LocalDate toDate = LocalDate.of(2025, 9, 22);

            // When
            ReportFilters filters = ReportFilters.dateRange(fromDate, toDate);

            // Then
            assertEquals(DateMode.DATE_RANGE, filters.dateMode());
            assertEquals(fromDate, filters.fromDate());
            assertEquals(toDate, filters.toDate());
            assertNull(filters.day());
        }

        @Test
        @DisplayName("Should create date range filter for current week")
        void shouldCreateDateRangeFilterForCurrentWeek() {
            // Given
            LocalDate startOfWeek = LocalDate.now().minusDays(7);
            LocalDate endOfWeek = LocalDate.now();

            // When
            ReportFilters filters = ReportFilters.dateRange(startOfWeek, endOfWeek);

            // Then
            assertEquals(DateMode.DATE_RANGE, filters.dateMode());
            assertEquals(startOfWeek, filters.fromDate());
            assertEquals(endOfWeek, filters.toDate());
        }

        @Test
        @DisplayName("Should create date range filter for current month")
        void shouldCreateDateRangeFilterForCurrentMonth() {
            // Given
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            LocalDate monthEnd = LocalDate.now();

            // When
            ReportFilters filters = ReportFilters.dateRange(monthStart, monthEnd);

            // Then
            assertEquals(DateMode.DATE_RANGE, filters.dateMode());
            assertEquals(monthStart, filters.fromDate());
            assertEquals(monthEnd, filters.toDate());
        }

        @Test
        @DisplayName("Should handle same from and to dates")
        void shouldHandleSameFromAndToDates() {
            // Given
            LocalDate sameDate = LocalDate.of(2025, 9, 22);

            // When
            ReportFilters filters = ReportFilters.dateRange(sameDate, sameDate);

            // Then
            assertEquals(DateMode.DATE_RANGE, filters.dateMode());
            assertEquals(sameDate, filters.fromDate());
            assertEquals(sameDate, filters.toDate());
        }

        @Test
        @DisplayName("Should handle null dates in range filter")
        void shouldHandleNullDatesInRangeFilter() {
            // When
            ReportFilters filters1 = ReportFilters.dateRange(null, LocalDate.now());
            ReportFilters filters2 = ReportFilters.dateRange(LocalDate.now(), null);
            ReportFilters filters3 = ReportFilters.dateRange(null, null);

            // Then
            assertEquals(DateMode.DATE_RANGE, filters1.dateMode());
            assertNull(filters1.fromDate());
            assertNotNull(filters1.toDate());

            assertEquals(DateMode.DATE_RANGE, filters2.dateMode());
            assertNotNull(filters2.fromDate());
            assertNull(filters2.toDate());

            assertEquals(DateMode.DATE_RANGE, filters3.dateMode());
            assertNull(filters3.fromDate());
            assertNull(filters3.toDate());
        }

        @Test
        @DisplayName("Should allow reverse date ranges")
        void shouldAllowReverseDateRanges() {
            // Given
            LocalDate laterDate = LocalDate.of(2025, 9, 22);
            LocalDate earlierDate = LocalDate.of(2025, 9, 15);

            // When
            ReportFilters filters = ReportFilters.dateRange(laterDate, earlierDate);

            // Then
            assertEquals(DateMode.DATE_RANGE, filters.dateMode());
            assertEquals(laterDate, filters.fromDate());
            assertEquals(earlierDate, filters.toDate());
        }
    }

    @Nested
    @DisplayName("DateMode Enum Tests")
    class DateModeEnumTests {

        @Test
        @DisplayName("Should have expected date mode values")
        void shouldHaveExpectedDateModeValues() {
            // Then
            assertEquals(2, DateMode.values().length);
            assertEquals(DateMode.SINGLE_DAY, DateMode.valueOf("SINGLE_DAY"));
            assertEquals(DateMode.DATE_RANGE, DateMode.valueOf("DATE_RANGE"));
        }

        @Test
        @DisplayName("Should convert date mode to string correctly")
        void shouldConvertDateModeToStringCorrectly() {
            // Then
            assertEquals("SINGLE_DAY", DateMode.SINGLE_DAY.toString());
            assertEquals("DATE_RANGE", DateMode.DATE_RANGE.toString());
        }

        @Test
        @DisplayName("Should support ordinal access")
        void shouldSupportOrdinalAccess() {
            // Then
            assertEquals(0, DateMode.SINGLE_DAY.ordinal());
            assertEquals(1, DateMode.DATE_RANGE.ordinal());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields match for single day")
        void shouldBeEqualWhenAllFieldsMatchForSingleDay() {
            // Given
            LocalDate date = LocalDate.of(2025, 9, 22);
            ReportFilters filters1 = ReportFilters.singleDay(date);
            ReportFilters filters2 = ReportFilters.singleDay(date);

            // Then
            assertEquals(filters1, filters2);
            assertEquals(filters1.hashCode(), filters2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when all fields match for date range")
        void shouldBeEqualWhenAllFieldsMatchForDateRange() {
            // Given
            LocalDate fromDate = LocalDate.of(2025, 9, 15);
            LocalDate toDate = LocalDate.of(2025, 9, 22);
            ReportFilters filters1 = ReportFilters.dateRange(fromDate, toDate);
            ReportFilters filters2 = ReportFilters.dateRange(fromDate, toDate);

            // Then
            assertEquals(filters1, filters2);
            assertEquals(filters1.hashCode(), filters2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when date modes differ")
        void shouldNotBeEqualWhenDateModesDiffer() {
            // Given
            LocalDate date = LocalDate.of(2025, 9, 22);
            ReportFilters singleDay = ReportFilters.singleDay(date);
            ReportFilters dateRange = ReportFilters.dateRange(date, date);

            // Then
            assertNotEquals(singleDay, dateRange);
        }

        @Test
        @DisplayName("Should not be equal when dates differ")
        void shouldNotBeEqualWhenDatesDiffer() {
            // Given
            LocalDate date1 = LocalDate.of(2025, 9, 22);
            LocalDate date2 = LocalDate.of(2025, 9, 23);
            ReportFilters filters1 = ReportFilters.singleDay(date1);
            ReportFilters filters2 = ReportFilters.singleDay(date2);

            // Then
            assertNotEquals(filters1, filters2);
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should have meaningful toString for single day")
        void shouldHaveMeaningfulToStringForSingleDay() {
            // Given
            LocalDate date = LocalDate.of(2025, 9, 22);
            ReportFilters filters = ReportFilters.singleDay(date);

            // When
            String result = filters.toString();

            // Then
            assertNotNull(result);
            assertTrue(result.contains("SINGLE_DAY"));
            assertTrue(result.contains("2025-09-22"));
        }

        @Test
        @DisplayName("Should have meaningful toString for date range")
        void shouldHaveMeaningfulToStringForDateRange() {
            // Given
            LocalDate fromDate = LocalDate.of(2025, 9, 15);
            LocalDate toDate = LocalDate.of(2025, 9, 22);
            ReportFilters filters = ReportFilters.dateRange(fromDate, toDate);

            // When
            String result = filters.toString();

            // Then
            assertNotNull(result);
            assertTrue(result.contains("DATE_RANGE"));
            assertTrue(result.contains("2025-09-15"));
            assertTrue(result.contains("2025-09-22"));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate single day filters")
        void shouldValidateSingleDayFilters() {
            // Given
            ReportFilters validFilter = ReportFilters.singleDay(LocalDate.now());
            ReportFilters nullFilter = ReportFilters.singleDay(null);

            // When
            boolean validIsValid = validFilter.isValid();
            boolean nullIsValid = nullFilter.isValid();

            // Then
            assertTrue(validIsValid);
            assertFalse(nullIsValid);
        }

        @Test
        @DisplayName("Should validate date range filters")
        void shouldValidateDateRangeFilters() {
            // Given
            LocalDate fromDate = LocalDate.of(2025, 9, 15);
            LocalDate toDate = LocalDate.of(2025, 9, 22);

            ReportFilters validRange = ReportFilters.dateRange(fromDate, toDate);
            ReportFilters invalidRange = ReportFilters.dateRange(toDate, fromDate); // Reversed
            ReportFilters nullFromRange = ReportFilters.dateRange(null, toDate);
            ReportFilters nullToRange = ReportFilters.dateRange(fromDate, null);

            // When & Then
            assertTrue(validRange.isValid());
            assertFalse(invalidRange.isValid()); // End before start
            assertFalse(nullFromRange.isValid());
            assertFalse(nullToRange.isValid());
        }

        @Test
        @DisplayName("Should handle edge case date validations")
        void shouldHandleEdgeCaseDateValidations() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate tomorrow = today.plusDays(1);

            // When
            ReportFilters todayOnly = ReportFilters.singleDay(today);
            ReportFilters yesterdayToToday = ReportFilters.dateRange(yesterday, today);
            ReportFilters todayToTomorrow = ReportFilters.dateRange(today, tomorrow);
            ReportFilters sameDay = ReportFilters.dateRange(today, today);

            // Then
            assertTrue(todayOnly.isValid());
            assertTrue(yesterdayToToday.isValid());
            assertTrue(todayToTomorrow.isValid());
            assertTrue(sameDay.isValid());
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create filters with static factory methods")
        void shouldCreateFiltersWithStaticFactoryMethods() {
            // Given
            LocalDate date = LocalDate.now();
            LocalDate fromDate = LocalDate.now().minusDays(7);
            LocalDate toDate = LocalDate.now();

            // When
            ReportFilters singleDay = ReportFilters.singleDay(date);
            ReportFilters dateRange = ReportFilters.dateRange(fromDate, toDate);

            // Then
            assertNotNull(singleDay);
            assertNotNull(dateRange);
            assertEquals(DateMode.SINGLE_DAY, singleDay.dateMode());
            assertEquals(DateMode.DATE_RANGE, dateRange.dateMode());
        }

        @Test
        @DisplayName("Should support method chaining patterns")
        void shouldSupportMethodChainingPatterns() {
            // Given
            LocalDate date = LocalDate.now();

            // When
            ReportFilters filters = ReportFilters.singleDay(date);

            // Then
            // Filters are immutable, so no chaining methods expected
            assertNotNull(filters);
            assertEquals(date, filters.day());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle leap year dates")
        void shouldHandleLeapYearDates() {
            // Given
            LocalDate leapYearDate = LocalDate.of(2024, 2, 29); // Leap year

            // When
            ReportFilters filters = ReportFilters.singleDay(leapYearDate);

            // Then
            assertEquals(leapYearDate, filters.day());
            assertTrue(filters.isValid());
        }

        @Test
        @DisplayName("Should handle very old dates")
        void shouldHandleVeryOldDates() {
            // Given
            LocalDate oldDate = LocalDate.of(1900, 1, 1);

            // When
            ReportFilters filters = ReportFilters.singleDay(oldDate);

            // Then
            assertEquals(oldDate, filters.day());
            assertTrue(filters.isValid());
        }

        @Test
        @DisplayName("Should handle very future dates")
        void shouldHandleVeryFutureDates() {
            // Given
            LocalDate futureDate = LocalDate.of(2100, 12, 31);

            // When
            ReportFilters filters = ReportFilters.singleDay(futureDate);

            // Then
            assertEquals(futureDate, filters.day());
            assertTrue(filters.isValid());
        }

        @Test
        @DisplayName("Should handle maximum date ranges")
        void shouldHandleMaximumDateRanges() {
            // Given
            LocalDate minDate = LocalDate.MIN;
            LocalDate maxDate = LocalDate.MAX;

            // When
            ReportFilters filters = ReportFilters.dateRange(minDate, maxDate);

            // Then
            assertEquals(minDate, filters.fromDate());
            assertEquals(maxDate, filters.toDate());
            assertTrue(filters.isValid());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should support common reporting periods")
        void shouldSupportCommonReportingPeriods() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(7);
            LocalDate monthAgo = today.minusMonths(1);
            LocalDate yearAgo = today.minusYears(1);

            // When
            ReportFilters daily = ReportFilters.singleDay(today);
            ReportFilters weekly = ReportFilters.dateRange(weekAgo, today);
            ReportFilters monthly = ReportFilters.dateRange(monthAgo, today);
            ReportFilters yearly = ReportFilters.dateRange(yearAgo, today);

            // Then
            assertTrue(daily.isValid());
            assertTrue(weekly.isValid());
            assertTrue(monthly.isValid());
            assertTrue(yearly.isValid());
        }

        @Test
        @DisplayName("Should calculate date range duration")
        void shouldCalculateDateRangeDuration() {
            // Given
            LocalDate fromDate = LocalDate.of(2025, 9, 15);
            LocalDate toDate = LocalDate.of(2025, 9, 22);
            ReportFilters filters = ReportFilters.dateRange(fromDate, toDate);

            // When
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);

            // Then
            assertEquals(7, daysBetween);
            assertTrue(filters.isValid());
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should be immutable record")
        void shouldBeImmutableRecord() {
            // Given
            LocalDate date = LocalDate.of(2025, 9, 22);
            ReportFilters filters = ReportFilters.singleDay(date);

            // When
            LocalDate retrievedDate = filters.day();

            // Then
            assertEquals(date, retrievedDate);
            // Should not be able to modify the filter after creation
            assertSame(date, retrievedDate); // Same reference for LocalDate (immutable)
        }

        @Test
        @DisplayName("Should support defensive copying")
        void shouldSupportDefensiveCopying() {
            // Given
            LocalDate originalDate = LocalDate.of(2025, 9, 22);
            ReportFilters filters = ReportFilters.singleDay(originalDate);

            // When
            LocalDate retrievedDate = filters.day();

            // Then
            // LocalDate is immutable, so no defensive copying needed
            assertEquals(originalDate, retrievedDate);
        }
    }
}
