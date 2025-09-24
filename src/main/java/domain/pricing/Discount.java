package domain.pricing;

import domain.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Represents a discount that can be applied to a batch of products
 */
public final class Discount {
    private final long id;
    private final long batchId;
    private final DiscountType type;
    private final BigDecimal value;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean isActive;
    private final String description;
    private final long createdBy;

    public Discount(long id, long batchId, DiscountType type, BigDecimal value,
                   LocalDate startDate, LocalDate endDate, boolean isActive,
                   String description, long createdBy) {
        this.id = id;
        this.batchId = batchId;
        this.type = type;
        this.value = value;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.description = description;
        this.createdBy = createdBy;
    }

    public long getId() { return id; }
    public long getBatchId() { return batchId; }
    public DiscountType getType() { return type; }
    public BigDecimal getValue() { return value; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return isActive; }
    public String getDescription() { return description; }
    public long getCreatedBy() { return createdBy; }

    /**
     * Check if the discount is currently valid (active and within date range)
     */
    public boolean isValidForDate(LocalDate date) {
        return isActive &&
               !date.isBefore(startDate) &&
               !date.isAfter(endDate);
    }

    /**
     * Calculate the discount amount for a given price
     */
    public Money calculateDiscountAmount(Money originalPrice) {
        if (type == DiscountType.PERCENTAGE) {
            BigDecimal discountAmount = originalPrice.amount()
                .multiply(value)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return Money.of(discountAmount);
        } else {
            return Money.of(value);
        }
    }

    /**
     * Apply discount to a price and return the discounted price
     */
    public Money applyDiscount(Money originalPrice) {
        Money discountAmount = calculateDiscountAmount(originalPrice);
        Money result = originalPrice.minus(discountAmount);
        // Ensure price never goes below zero
        return result.amount().compareTo(BigDecimal.ZERO) < 0 ? Money.of(0) : result;
    }

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Discount discount = (Discount) obj;
        return id == discount.id &&
               batchId == discount.batchId &&
               isActive == discount.isActive &&
               createdBy == discount.createdBy &&
               type == discount.type &&
               java.util.Objects.equals(value, discount.value) &&
               java.util.Objects.equals(startDate, discount.startDate) &&
               java.util.Objects.equals(endDate, discount.endDate) &&
               java.util.Objects.equals(description, discount.description);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, batchId, type, value, startDate, endDate, isActive, description, createdBy);
    }
}
