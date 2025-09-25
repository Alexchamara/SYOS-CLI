package application.services;

import application.usecase.DiscountManagementUseCase;
import domain.pricing.Discount;
import domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for applying discounts to products and calculating discount amounts
 */
public class DiscountService {
    private final DiscountManagementUseCase discountManagementUseCase;

    public DiscountService(DiscountManagementUseCase discountManagementUseCase) {
        this.discountManagementUseCase = discountManagementUseCase;
    }

    /**
     * Get the best available discount for a batch on a specific date
     */
    public Optional<Discount> getBestDiscountForBatch(long batchId, LocalDate date) {
        List<Discount> activeDiscounts = discountManagementUseCase.getActiveDiscountsForBatch(batchId, date);

        if (activeDiscounts.isEmpty()) {
            return Optional.empty();
        }

        Money standardPrice = Money.of(100); // $100 as reference

        return activeDiscounts.stream()
            .max((d1, d2) -> {
                Money discount1Amount = d1.calculateDiscountAmount(standardPrice);
                Money discount2Amount = d2.calculateDiscountAmount(standardPrice);
                return discount1Amount.amount().compareTo(discount2Amount.amount());
            });
    }

    /**
     * Apply the best available discount to a price
     */
    public PricingResult applyBestDiscount(long batchId, Money originalPrice, LocalDate date) {
        Optional<Discount> bestDiscount = getBestDiscountForBatch(batchId, date);

        if (bestDiscount.isEmpty()) {
            return new PricingResult(originalPrice, Money.of(0), null);
        }

        Discount discount = bestDiscount.get();
        Money discountAmount = discount.calculateDiscountAmount(originalPrice);
        Money finalPrice = discount.applyDiscount(originalPrice);

        return new PricingResult(finalPrice, discountAmount, discount);
    }

    /**
     * Get all active discounts for a product code (for cart display)
     */
    public List<Discount> getActiveDiscountsForProduct(String productCode, LocalDate date) {
        List<Discount> allDiscounts = discountManagementUseCase.getDiscountsByProductCode(productCode);

        return allDiscounts.stream()
            .filter(discount -> discount.isValidForDate(date))
            .toList();
    }

    /**
     * Result of pricing calculation with discount information
     */
    public static class PricingResult {
        private final Money finalPrice;
        private final Money discountAmount;
        private final Discount appliedDiscount;

        public PricingResult(Money finalPrice, Money discountAmount, Discount appliedDiscount) {
            this.finalPrice = finalPrice;
            this.discountAmount = discountAmount;
            this.appliedDiscount = appliedDiscount;
        }

        public Money getFinalPrice() { return finalPrice; }
        public Money getDiscountAmount() { return discountAmount; }
        public Discount getAppliedDiscount() { return appliedDiscount; }
        public boolean hasDiscount() { return appliedDiscount != null; }

        public String getDiscountDescription() {
            if (appliedDiscount == null) {
                return "No discount";
            }

            String typeDescription;
            if (appliedDiscount.getType() == Discount.DiscountType.PERCENTAGE) {
                BigDecimal value = appliedDiscount.getValue();
                String formattedValue = value.stripTrailingZeros().toPlainString();
                if (!formattedValue.contains(".")) {
                    formattedValue += ".0";
                }
                typeDescription = formattedValue + "% off";
            } else {
                typeDescription = "$" + appliedDiscount.getValue() + " off";
            }

            return typeDescription + (appliedDiscount.getDescription() != null
                ? " (" + appliedDiscount.getDescription() + ")"
                : "");
        }
    }
}
