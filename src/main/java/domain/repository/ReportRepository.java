package domain.repository;

import application.reports.dto.DailySalesRow;
import application.reports.dto.ReshelveRow;
import application.reports.dto.ReorderRow;
import application.reports.dto.StockBatchRow;
import application.reports.dto.BillHeaderRow;
import application.reports.dto.BillLineRow;
import application.reports.dto.OrderHeaderRow;
import application.reports.dto.OrderLineRow;
import cli.manager.filters.ReportFilters;

import java.util.List;

public interface ReportRepository {
    List<DailySalesRow> dailySales(ReportFilters filters);
    List<ReshelveRow> reshelve(ReportFilters filters);
    List<ReorderRow> reorder(ReportFilters filters, int threshold);
    List<StockBatchRow> stockBatches(ReportFilters filters);
    List<BillHeaderRow> bills(ReportFilters filters);
    List<BillLineRow> billLines(long orderId);

    // New methods for order-related reports (WEB sales)
    List<OrderHeaderRow> orders(ReportFilters filters);
    List<OrderLineRow> orderLines(long orderId);
}
