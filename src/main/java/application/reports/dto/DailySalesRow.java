package application.reports.dto;

import java.math.BigDecimal;

public record DailySalesRow(
    String code,
    String name,
    String location,
    int qtySold,
    BigDecimal gross,
    BigDecimal discount,
    BigDecimal net
) {
    public enum Sort {
        QTY_DESC, REVENUE_DESC, NAME_ASC, LOCATION_ASC
    }
}
