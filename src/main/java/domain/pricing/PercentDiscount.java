package domain.pricing;

import domain.billing.BillLine;
import domain.shared.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Discount policy that applies a percentage discount to the total.
 */
public final class PercentDiscount implements DiscountPolicy {
    private final int percent; // e.g. 5 = 5%

    public PercentDiscount(int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("percent 0..100");
        this.percent = percent;
    }

    @Override
    public Money discountFor(List<BillLine> lines) {
        var subtotal = lines.stream().map(BillLine::lineTotal).reduce(Money.of(0), Money::plus).amount();
        long cents = subtotal.movePointRight(2).longValueExact();
        long discCents = Math.round(cents * (percent / 100.0));
        return Money.of(BigDecimal.valueOf(discCents, 2));
    }

    /**
     * Get the discount percentage
     */
    public int getPercentage() {
        return percent;
    }
}
