package cli.manager.filters;

import java.time.LocalDate;

public record ReportFilters(
    DateMode dateMode,
    LocalDate day,
    LocalDate fromDate,
    LocalDate toDate
) {
    public static ReportFilters singleDay(LocalDate day) {
        return new ReportFilters(DateMode.SINGLE_DAY, day, null, null);
    }

    public static ReportFilters dateRange(LocalDate fromDate, LocalDate toDate) {
        return new ReportFilters(DateMode.DATE_RANGE, null, fromDate, toDate);
    }

    public boolean isValid() {
        return switch (dateMode) {
            case SINGLE_DAY -> day != null;
            case DATE_RANGE -> fromDate != null && toDate != null && !toDate.isBefore(fromDate);
        };
    }

    public enum DateMode {
        SINGLE_DAY, DATE_RANGE
    }
}
