package domain.repository;

import domain.product.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> findAllActive();
    List<Category> findAll();
    Optional<Category> findByCode(String code);
    void save(Category category);
    void delete(String code);
    Category incrementSequenceAndSave(String categoryCode);
}