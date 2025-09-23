package application.reports.dto;

import java.time.LocalDate;

public record ReorderRow(
    String code,
    String name,
    String batchId,
    String location,
    int quantity,
    LocalDate expiry,
    String status
) {
    public enum Sort {
        QTY_ASC, LOCATION_ASC, NAME_ASC, EXPIRY_ASC
    }
}
