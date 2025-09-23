package application.reports.dto;

import java.time.LocalDateTime;

public record ReshelveRow(
    long id,
    LocalDateTime happenedAt,
    String productCode,
    String productName,
    String fromLocation,
    String toLocation,
    int quantity,
    String note
) {
    public enum Sort {
        TIME_DESC, TIME_ASC, PRODUCT_ASC, QUANTITY_DESC
    }
}
