package domain.pricing;

import application.services.DiscountService;
import domain.billing.BillLine;
import domain.shared.Money;

import java.time.LocalDate;
import java.util.List;

/**
 * Enhanced discount policy that applies batch-specific discounts automatically
 * This policy integrates with the discount management system to apply
 * the best available discounts for each product based on batch information
 */
public final class BatchDiscountPolicy implements DiscountPolicy {
    private final DiscountService discountService;

    public BatchDiscountPolicy(DiscountService discountService) {
        this.discountService = discountService;
    }

    @Override
    public Money discountFor(List<BillLine> lines) {
        Money totalDiscount = Money.of(0);
        LocalDate today = LocalDate.now();

        for (BillLine line : lines) {

            var activeDiscounts = discountService.getActiveDiscountsForProduct(
                line.productCode().value(), today);

            if (!activeDiscounts.isEmpty()) {
                var bestDiscount = activeDiscounts.stream()
                    .max((d1, d2) -> {
                        Money discount1 = d1.calculateDiscountAmount(line.unitPrice());
                        Money discount2 = d2.calculateDiscountAmount(line.unitPrice());
                        return discount1.amount().compareTo(discount2.amount());
                    })
                    .orElse(null);

                if (bestDiscount != null) {
                    Money lineDiscountAmount = bestDiscount.calculateDiscountAmount(line.unitPrice());
                    Money totalLineDiscount = lineDiscountAmount.times(line.qty().value());
                    totalDiscount = totalDiscount.plus(totalLineDiscount);
                }
            }
        }

        return totalDiscount;
    }

    /**
     * Get discount description for display purposes
     */
    public String getDiscountDescription(List<BillLine> lines) {
        LocalDate today = LocalDate.now();
        StringBuilder description = new StringBuilder();

        for (BillLine line : lines) {
            var activeDiscounts = discountService.getActiveDiscountsForProduct(
                line.productCode().value(), today);

            if (!activeDiscounts.isEmpty()) {
                var bestDiscount = activeDiscounts.stream()
                    .max((d1, d2) -> {
                        Money discount1 = d1.calculateDiscountAmount(line.unitPrice());
                        Money discount2 = d2.calculateDiscountAmount(line.unitPrice());
                        return discount1.amount().compareTo(discount2.amount());
                    })
                    .orElse(null);

                if (bestDiscount != null) {
                    if (description.length() > 0) {
                        description.append("; ");
                    }

                    String discountType = bestDiscount.getType() == Discount.DiscountType.PERCENTAGE
                        ? bestDiscount.getValue() + "% off"
                        : "$" + bestDiscount.getValue() + " off";

                    description.append(line.productCode().value())
                              .append(": ")
                              .append(discountType);

                    if (bestDiscount.getDescription() != null) {
                        description.append(" (").append(bestDiscount.getDescription()).append(")");
                    }
                }
            }
        }

        return description.length() > 0 ? description.toString() : "No discounts applied";
    }
}
