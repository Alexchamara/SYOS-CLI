package application.reports.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StockBatchRow(
    String code,
    String name,
    String batchId,
    LocalDate expiry,
    LocalDateTime receivedAt,
    int qty,
    String location
) {
    public enum Sort {
        EXPIRY_ASC_THEN_RECEIVED_ASC, RECEIVED_ASC, QTY_DESC
    }
}
