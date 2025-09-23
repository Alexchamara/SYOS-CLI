package application.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillHeaderRow(
    int rowNo,
    long orderId,
    String serial,  // Changed from long to String to handle values like 'C-000001'
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
