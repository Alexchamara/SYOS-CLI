package application.reports;

import application.reports.dto.DailySalesRow;
import application.reports.dto.ReshelveRow;
import application.reports.dto.ReorderRow;
import application.reports.dto.StockBatchRow;
import application.reports.dto.BillHeaderRow;
import application.reports.dto.BillLineRow;
import application.reports.dto.OrderHeaderRow;
import application.reports.dto.OrderLineRow;
import cli.manager.filters.ReportFilters;
import domain.repository.ReportRepository;

import java.util.List;

public class ReportService {
    private final ReportRepository repository;

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    public List<DailySalesRow> dailySales(ReportFilters filters) {
        return repository.dailySales(filters);
    }

    public List<ReshelveRow> reshelve(ReportFilters filters) {
        return repository.reshelve(filters);
    }

    public List<ReorderRow> reorder(ReportFilters filters, int threshold) {
        return repository.reorder(filters, threshold);
    }

    public List<StockBatchRow> stockBatches(ReportFilters filters) {
        return repository.stockBatches(filters);
    }

    // SHELF sales (bill-based)
    public List<BillHeaderRow> bills(ReportFilters filters) {
        return repository.bills(filters);
    }

    public List<BillLineRow> billLines(long orderId) {
        return repository.billLines(orderId);
    }

    // WEB sales (order-based)
    public List<OrderHeaderRow> orders(ReportFilters filters) {
        return repository.orders(filters);
    }

    public List<OrderLineRow> orderLines(long orderId) {
        return repository.orderLines(orderId);
    }
}
