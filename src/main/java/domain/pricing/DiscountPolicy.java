package domain.pricing;

import domain.billing.BillLine;
import domain.shared.Money;

import java.util.List;

/**
 * Strategy interface for calculating discounts on bill lines.
 */
public interface DiscountPolicy {
    /**
     * Return how much to subtract from subtotal for these lines.
     */
    Money discountFor(List<BillLine> lines);
}