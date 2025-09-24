package cli.manager.screens;

import application.services.ReportService;
import application.reports.dto.ReshelveRow;
import cli.manager.filters.ReportFilters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ReshelveScreen {
    private final ReportService reportService;
    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReshelveScreen(ReportService reportService, Scanner scanner) {
        this.reportService = reportService;
        this.scanner = scanner;
    }

    public void run() {
        while (true) {
            displayMenu();

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> displayTodaysMovements();
                case "2" -> displayMovementsByDate();
                case "3" -> displayMovementsByDateRange();
                case "0" -> { return; }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n=== RESHELVE REPORT ===");
        System.out.println("1. Today's Movements");
        System.out.println("2. Movements by Date");
        System.out.println("3. Movements by Date Range");
        System.out.println("0. Back");
        System.out.print("Choose an option: ");
    }

    private void displayTodaysMovements() {
        try {
            ReportFilters filters = new ReportFilters(ReportFilters.DateMode.SINGLE_DAY, LocalDate.now(), null, null);
            List<ReshelveRow> data = reportService.reshelve(filters);
            displayMovements(data, "Today's Reshelve Movements");
        } catch (Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
    }

    private void displayMovementsByDate() {
        try {
            LocalDate date = promptForDate("Enter date (yyyy-MM-dd): ");
            ReportFilters filters = new ReportFilters(ReportFilters.DateMode.SINGLE_DAY, date, null, null);
            List<ReshelveRow> data = reportService.reshelve(filters);
            displayMovements(data, "Reshelve Movements for " + date.format(DATE_FORMAT));
        } catch (Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
    }

    private void displayMovementsByDateRange() {
        try {
            LocalDate fromDate = promptForDate("Enter from date (yyyy-MM-dd): ");
            LocalDate toDate = promptForDate("Enter to date (yyyy-MM-dd): ");
            ReportFilters filters = new ReportFilters(ReportFilters.DateMode.DATE_RANGE, null, fromDate, toDate);
            List<ReshelveRow> data = reportService.reshelve(filters);
            displayMovements(data, "Reshelve Movements from " + fromDate.format(DATE_FORMAT) + " to " + toDate.format(DATE_FORMAT));
        } catch (Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
    }

    private LocalDate promptForDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine().trim();
                return LocalDate.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd format.");
            }
        }
    }

    private void displayMovements(List<ReshelveRow> data, String title) {
        System.out.println("\n=== " + title + " ===");

        if (data.isEmpty()) {
            System.out.println("No movements found for the specified criteria.");
            return;
        }

        System.out.printf("%-8s %-12s %-12s %-20s %-25s %-8s %-25s%n",
            "ID", "Time", "Product", "Name", "Movement", "Qty", "Note");
        System.out.println("=".repeat(120));

        for (ReshelveRow row : data) {
            String time = row.happenedAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String movement = row.fromLocation() + " -> " + row.toLocation();
            System.out.printf("%-8d %-12s %-12s %-20s %-25s %-8d %-25s%n",
                row.id(),
                time,
                row.productCode(),
                truncate(row.productName(), 20),
                movement,
                row.quantity(),
                truncate(row.note(), 25));
        }

        System.out.println("\nTotal movements: " + data.size());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}
