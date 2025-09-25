package cli.manager.filters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class FiltersPrompt {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private FiltersPrompt() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    public static ReportFilters promptForDateFilter(Scanner scanner) {
        while (true) {
            System.out.println("Date Filter Options:");
            System.out.println("1. Single Day");
            System.out.println("2. Date Range");
            System.out.print("Choose filter type: ");

            String choice = scanner.nextLine().trim();
            if ("1".equals(choice)) {
                LocalDate day = promptForSingleDate(scanner);
                return ReportFilters.singleDay(day);
            } else if ("2".equals(choice)) {
                LocalDate fromDate = promptForFromDate(scanner);
                LocalDate toDate = promptForToDate(scanner, fromDate);
                return ReportFilters.dateRange(fromDate, toDate);
            } else {
                System.out.println("Invalid choice. Please choose 1 or 2");
            }
        }
    }

    public static LocalDate parseDate(String input) {
        if (input == null) {
            throw new NullPointerException("date string is null");
        }
        String s = input.trim().toLowerCase();
        if (s.equals("today")) {
            return LocalDate.now();
        }
        if (s.equals("yesterday")) {
            return LocalDate.now().minusDays(1);
        }
        return LocalDate.parse(s, DATE_FORMAT);
    }

    private static LocalDate promptForSingleDate(Scanner scanner) {
        while (true) {
            System.out.print("Enter date (yyyy-mm-dd) or 'today'/'yesterday': ");
            if (!scanner.hasNextLine()) {
                return LocalDate.now();
            }
            String input = scanner.nextLine();
            try {
                return parseDate(input);
            } catch (DateTimeParseException | NullPointerException e) {
                System.out.println("Invalid date format. Please use yyyy-mm-dd");
            }
        }
    }

    private static LocalDate promptForFromDate(Scanner scanner) {
        while (true) {
            System.out.print("Enter from date (yyyy-mm-dd): ");
            if (!scanner.hasNextLine()) {
                return LocalDate.now();
            }
            String input = scanner.nextLine();
            try {
                return parseDate(input);
            } catch (DateTimeParseException | NullPointerException e) {
                System.out.println("Invalid date format. Please use yyyy-mm-dd");
            }
        }
    }

    private static LocalDate promptForToDate(Scanner scanner, LocalDate fromDate) {
        while (true) {
            System.out.print("Enter to date (yyyy-mm-dd): ");
            if (!scanner.hasNextLine()) {
                return fromDate;
            }
            String input = scanner.nextLine();
            try {
                LocalDate toDate = parseDate(input);
                if (toDate.isBefore(fromDate)) {
                    System.out.println("End date must be after start date");
                    continue;
                }
                return toDate;
            } catch (DateTimeParseException | NullPointerException e) {
                System.out.println("Invalid date format. Please use yyyy-mm-dd");
            }
        }
    }

    private static LocalDate promptForToDate(Scanner scanner) {
        while (true) {
            System.out.print("Enter to date (yyyy-mm-dd): ");
            if (!scanner.hasNextLine()) {
                return LocalDate.now();
            }
            String input = scanner.nextLine();
            try {
                return parseDate(input);
            } catch (DateTimeParseException | NullPointerException e) {
                System.out.println("Invalid date format. Please use yyyy-mm-dd");
            }
        }
    }
}
