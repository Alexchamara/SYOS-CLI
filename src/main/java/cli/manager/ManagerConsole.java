package cli.manager;

import application.services.ReportService;
import cli.manager.screens.*;
import cli.manager.screens.DailySalesScreen;
import infrastructure.persistence.JdbcReportRepository;

import javax.sql.DataSource;
import java.util.Scanner;

public class ManagerConsole {
    private final ReportService reportService;
    private final Scanner scanner;

    public ManagerConsole(DataSource dataSource) {
        this.reportService = new ReportService(new JdbcReportRepository(dataSource));
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Manager Console ===");
            System.out.println("1) Daily Sales");
            System.out.println("2) End-of-Day Reshelve List");
            System.out.println("3) Reorder (< 50) Report");
            System.out.println("4) Stock Report (Batch-wise)");
            System.out.println("5) Bill Report (SHELF Sales)");
            System.out.println("6) Order Report (WEB Sales)");
            System.out.println("0) Back");
            System.out.print("Pick: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> new DailySalesScreen(reportService, scanner).run();
                case "2" -> new ReshelveScreen(reportService, scanner).run();
                case "3" -> new ReorderScreen(reportService, scanner).run();
                case "4" -> new StockBatchScreen(reportService, scanner).run();
                case "5" -> new BillReportScreen(reportService, scanner).run();
                case "6" -> new OrderReportScreen(reportService, scanner).run();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice. Please select 0-6.");
            }
        }
    }
}
