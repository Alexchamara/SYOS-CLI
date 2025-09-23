package application.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderHeaderRow(
    int rowNo,
    long orderId,
    long serial,
    String type,
    String store,
    LocalDateTime createdAt,
    BigDecimal netTotal,
    String paymentSummary
) {
    public enum Sort {
        SERIAL_ASC, SERIAL_DESC, DATE_ASC, DATE_DESC, NET_DESC
    }
}
