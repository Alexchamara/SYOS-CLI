package application.usecase;

import domain.inventory.StockLocation;
import domain.product.Product;
import domain.repository.ProductRepository;
import domain.shared.Code;
import application.services.AvailabilityService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for searching products on WEB platform with availability info
 */
public final class SearchProductUseCase {
    private final ProductRepository productRepository;
    private final AvailabilityService availabilityService;

    public SearchProductUseCase(ProductRepository productRepository, AvailabilityService availabilityService) {
        this.productRepository = productRepository;
        this.availabilityService = availabilityService;
    }

    public List<ProductSearchResult> searchByName(String searchTerm) {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .filter(p -> p.name().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    public List<ProductSearchResult> searchByCategory(String category) {
        // Since category search isn't directly supported, return all products for now
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    private ProductSearchResult toSearchResult(Product product) {
        int webStock = availabilityService.available(product.code().value(), StockLocation.WEB);
        boolean inStock = webStock > 0;

        return new ProductSearchResult(
            product.code().value(),
            product.name(),
            (int) product.price().cents(),
            webStock,
            inStock
        );
    }

    public static class ProductSearchResult {
        public final String code;
        public final String name;
        public final int priceCents;
        public final int webStock;
        public final boolean inStock;

        public ProductSearchResult(String code, String name, int priceCents, int webStock, boolean inStock) {
            this.code = code;
            this.name = name;
            this.priceCents = priceCents;
            this.webStock = webStock;
            this.inStock = inStock;
        }
    }
}
