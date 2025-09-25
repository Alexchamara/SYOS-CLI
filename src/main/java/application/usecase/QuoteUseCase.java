package application.usecase;


import domain.billing.BillLine;
import domain.pricing.DiscountPolicy;
import domain.repository.ProductRepository;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;
import domain.product.Product;

import java.util.ArrayList;
import java.util.List;

public class QuoteUseCase {
    public record Quote(List<BillLine> lines, Money subtotal, Money discount, Money total) {}

    private final ProductRepository products;

    public QuoteUseCase(ProductRepository products) { this.products = products; }

    public boolean productExists(String code) {
        if (code == null || code.isBlank()) return false;
        return products.findByCode(new Code(code)).isPresent();
    }

    public Quote preview(List<CheckoutUseCase.CashItem> cart, DiscountPolicy discountPolicy) {
        List<BillLine> lines = new ArrayList<>();
        for (var it : cart) {
            Product p = products.findByCode(new Code(it.code()))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + it.code()));
            lines.add(new BillLine(p.code(), p.name(), new Quantity(it.qty()), p.price()));
        }
        var subtotal = lines.stream().map(BillLine::lineTotal).reduce(Money.of(0), Money::plus);
        var discount = discountPolicy.discountFor(lines);
        var total = subtotal.minus(discount);
        if (total.amount().signum() < 0) throw new IllegalArgumentException("Discount > subtotal");
        return new Quote(lines, subtotal, discount, total);
    }

    public Quote quote(List<Item> items, DiscountPolicy discountPolicy) {
        List<BillLine> lines = new ArrayList<>();
        for (var it : items) {
            Product p = products.findByCode(new Code(it.code()))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + it.code()));
            lines.add(new BillLine(p.code(), p.name(), new Quantity(it.qty()), p.price()));
        }
        var subtotal = lines.stream().map(BillLine::lineTotal).reduce(Money.of(0), Money::plus);
        var discount = discountPolicy.discountFor(lines);
        var total = subtotal.minus(discount);
        if (total.amount().signum() < 0) throw new IllegalArgumentException("Discount > subtotal");
        return new Quote(lines, subtotal, discount, total);
    }

    public static record Item(String code, String name, Money unitPrice, int qty) {}
}
