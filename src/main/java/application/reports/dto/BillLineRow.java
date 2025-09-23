package application.reports.dto;

import java.math.BigDecimal;

public record BillLineRow(
    String productCode,
    String name,
    BigDecimal unitPrice,
    int qty,
    BigDecimal lineTotal
) {
}
