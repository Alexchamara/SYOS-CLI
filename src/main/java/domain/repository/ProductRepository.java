package domain.repository;

import domain.product.Product;
import domain.shared.Code;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void upsert(Product p);
    Optional<Product> findByCode(Code code);
    List<Product> findAll();
    boolean deleteByCode(Code code);
    boolean existsByCode(Code code);
}