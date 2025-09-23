package domain.pricing;

import domain.billing.BillLine;
import domain.shared.Money;

import java.util.List;

/**
 * A discount policy that applies no discount.
 */
public final class NoDiscount implements DiscountPolicy {
    @Override public Money discountFor(List<BillLine> lines) {
        return Money.of(0);
    }
}
