package infrastructure.persistence;

import application.reports.dto.*;
import cli.manager.filters.ReportFilters;
import domain.repository.ReportRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcReportRepository implements ReportRepository {
    private final DataSource dataSource;

    public JdbcReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<DailySalesRow> dailySales(ReportFilters filters) {
        // Show all sales from both WEB and SHELF without filtering by store type
        String sql = """
            SELECT 
                product_code,
                name,
                location,
                SUM(total_qty) as total_qty,
                SUM(gross) as gross,
                SUM(discount) as discount,
                SUM(net) as net
            FROM (
                SELECT 
                    ol.product_code,
                    ol.name,
                    'WEB' as location,
                    SUM(ol.qty) as total_qty,
                    SUM(ol.qty * ol.unit_price) as gross,
                    SUM(CASE 
                        WHEN o.total_gross > 0 THEN (ol.line_total / o.total_gross) * o.discount
                        ELSE 0 
                    END) as discount,
                    SUM(ol.line_total) as net
                FROM order_lines ol
                JOIN orders o ON ol.order_id = o.id
                WHERE DATE(o.created_at) BETWEEN ? AND ?
                GROUP BY ol.product_code, ol.name
                
                UNION ALL
                
                SELECT 
                    bl.product_code,
                    bl.name,
                    'SHELF' as location,
                    SUM(bl.qty) as total_qty,
                    SUM(bl.qty * bl.unit_price_cents) / 100.0 as gross,
                    SUM(CASE 
                        WHEN b.subtotal_cents > 0 THEN (bl.line_total_cents / b.subtotal_cents) * (b.discount_cents / 100.0)
                        ELSE 0 
                    END) as discount,
                    SUM(bl.line_total_cents) / 100.0 as net
                FROM bill_line bl
                JOIN bill b ON bl.bill_id = b.id
                WHERE DATE(b.date_time) BETWEEN ? AND ?
                GROUP BY bl.product_code, bl.name
            ) combined_sales
            GROUP BY product_code, name, location
            ORDER BY location ASC, name ASC
            """;

        LocalDate startDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.fromDate();
        LocalDate endDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.toDate();

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, startDate);
            stmt.setObject(2, endDate);
            stmt.setObject(3, startDate);
            stmt.setObject(4, endDate);

            try (var rs = stmt.executeQuery()) {
                List<DailySalesRow> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new DailySalesRow(
                        rs.getString("product_code"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getInt("total_qty"),
                        rs.getBigDecimal("gross"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("net")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching daily sales", e);
        }
    }

    @Override
    public List<ReshelveRow> reshelve(ReportFilters filters) {
        // Show inventory movements from MAIN_STORE to SHELF or WEB for the specified date
        String sql = """
            SELECT 
                im.id,
                im.happened_at,
                im.product_code,
                p.name as product_name,
                im.from_location,
                im.to_location,
                im.quantity,
                im.note
            FROM inventory_movement im
            JOIN product p ON im.product_code = p.code
            WHERE DATE(im.happened_at) BETWEEN ? AND ?
              AND im.from_location = 'MAIN_STORE'
              AND im.to_location IN ('SHELF', 'WEB')
            ORDER BY im.happened_at DESC
            """;

        LocalDate startDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.fromDate();
        LocalDate endDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.toDate();

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, startDate);
            stmt.setObject(2, endDate);

            try (var rs = stmt.executeQuery()) {
                List<ReshelveRow> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new ReshelveRow(
                        rs.getLong("id"),
                        rs.getObject("happened_at", LocalDateTime.class),
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getString("from_location"),
                        rs.getString("to_location"),
                        rs.getInt("quantity"),
                        rs.getString("note")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reshelve data", e);
        }
    }

    @Override
    public List<ReorderRow> reorder(ReportFilters filters, int threshold) {
        // Show individual batches with quantities below threshold from all locations
        String sql = """
            SELECT 
                b.product_code,
                p.name,
                b.id as batch_id,
                b.location,
                b.quantity,
                b.expiry,
                CASE 
                    WHEN b.quantity < 20 THEN 'CRITICAL'
                    WHEN b.quantity < ? THEN 'LOW'
                    ELSE 'OK'
                END as status
            FROM batch b
            JOIN product p ON b.product_code = p.code
            WHERE b.quantity <= ? AND b.quantity > 0
            ORDER BY b.location ASC, b.quantity ASC, b.expiry ASC
            """;

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, threshold);
            stmt.setInt(2, threshold);

            try (var rs = stmt.executeQuery()) {
                List<ReorderRow> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new ReorderRow(
                        rs.getString("product_code"),
                        rs.getString("name"),
                        String.valueOf(rs.getLong("batch_id")),
                        rs.getString("location"),
                        rs.getInt("quantity"),
                        rs.getObject("expiry", LocalDate.class),
                        rs.getString("status")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reorder data", e);
        }
    }

    @Override
    public List<StockBatchRow> stockBatches(ReportFilters filters) {
        // Show all stock batches from all locations
        String sql = """
            SELECT 
                b.product_code,
                p.name,
                b.id as batch_id,
                b.expiry,
                b.received_at,
                b.quantity,
                b.location
            FROM batch b
            JOIN product p ON b.product_code = p.code
            WHERE b.quantity > 0
            ORDER BY b.expiry ASC, b.received_at ASC
            """;

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            try (var rs = stmt.executeQuery()) {
                List<StockBatchRow> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new StockBatchRow(
                        rs.getString("product_code"),
                        rs.getString("name"),
                        String.valueOf(rs.getLong("batch_id")),
                        rs.getObject("expiry", LocalDate.class),
                        rs.getObject("received_at", LocalDateTime.class),
                        rs.getInt("quantity"),
                        rs.getString("location")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching stock batch data", e);
        }
    }

    @Override
    public List<BillHeaderRow> bills(ReportFilters filters) {
        // Show all bills without transaction type filtering
        String sql = """
            SELECT 
                b.id,
                b.serial,
                'COUNTER' as type,
                'SHELF' as store,
                b.date_time,
                b.total_cents / 100.0 as net_total,
                CONCAT('Cash: ', FORMAT(b.cash_cents / 100.0, 2)) as payment_summary
            FROM bill b
            WHERE DATE(b.date_time) BETWEEN ? AND ?
            ORDER BY b.serial ASC
            """;

        LocalDate startDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.fromDate();
        LocalDate endDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.toDate();

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, startDate);
            stmt.setObject(2, endDate);

            try (var rs = stmt.executeQuery()) {
                List<BillHeaderRow> results = new ArrayList<>();
                int rowNo = 1;
                while (rs.next()) {
                    results.add(new BillHeaderRow(
                        rowNo++,
                        rs.getLong("id"),
                        rs.getString("serial"),  // Changed from getLong to getString
                        rs.getString("type"),
                        rs.getString("store"),
                        rs.getObject("date_time", LocalDateTime.class),
                        rs.getBigDecimal("net_total"),
                        rs.getString("payment_summary")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching bills", e);
        }
    }

    @Override
    public List<BillLineRow> billLines(long orderId) {
        String sql = """
            SELECT 
                bl.product_code,
                bl.name,
                bl.qty,
                bl.unit_price_cents / 100.0 as unit_price,
                bl.line_total_cents / 100.0 as line_total
            FROM bill_line bl
            WHERE bl.bill_id = ?
            ORDER BY bl.product_code
            """;

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, orderId);

            try (var rs = stmt.executeQuery()) {
                List<BillLineRow> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new BillLineRow(
                        rs.getString("product_code"),
                        rs.getString("name"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("qty"),
                        rs.getBigDecimal("line_total")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching bill lines", e);
        }
    }

    @Override
    public List<OrderHeaderRow> orders(ReportFilters filters) {
        // Show all orders without transaction type filtering
        String sql = """
            SELECT 
                o.id,
                o.bill_serial,
                'ONLINE' as type,
                'WEB' as store,
                o.created_at,
                o.total_net as net_total,
                CONCAT('Online Payment: ', FORMAT(o.total_net, 2)) as payment_summary
            FROM orders o
            WHERE DATE(o.created_at) BETWEEN ? AND ?
            ORDER BY o.bill_serial ASC
            """;

        LocalDate startDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.fromDate();
        LocalDate endDate = filters.dateMode() == ReportFilters.DateMode.SINGLE_DAY ?
            filters.day() : filters.toDate();

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, startDate);
            stmt.setObject(2, endDate);

            try (var rs = stmt.executeQuery()) {
                List<OrderHeaderRow> results = new ArrayList<>();
                int rowNo = 1;
                while (rs.next()) {
                    results.add(new OrderHeaderRow(
                        rowNo++,
                        rs.getLong("id"),
                        rs.getLong("bill_serial"),
                        rs.getString("type"),
                        rs.getString("store"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getBigDecimal("net_total"),
                        rs.getString("payment_summary")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching orders", e);
        }
    }

    @Override
    public List<OrderLineRow> orderLines(long orderId) {
        String sql = """
            SELECT 
                ol.product_code,
                ol.name,
                ol.qty,
                ol.unit_price,
                ol.line_total
            FROM order_lines ol
            WHERE ol.order_id = ?
            ORDER BY ol.product_code
            """;

        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, orderId);

            try (var rs = stmt.executeQuery()) {
                List<OrderLineRow> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new OrderLineRow(
                        rs.getString("product_code"),
                        rs.getString("name"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("qty"),
                        rs.getBigDecimal("line_total")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching order lines", e);
        }
    }
}
