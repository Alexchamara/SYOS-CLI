package domain.repository;

import domain.pricing.Discount;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing discounts
 */
public interface DiscountRepository {

    /**
     * Save a new discount
     */
    Discount save(Discount discount);

    /**
     * Update an existing discount
     */
    Discount update(Discount discount);

    /**
     * Find discount by ID
     */
    Optional<Discount> findById(long id);

    /**
     * Find all discounts for a specific batch
     */
    List<Discount> findByBatchId(long batchId);

    /**
     * Find active discounts for a batch on a specific date
     */
    List<Discount> findActiveDiscountsForBatch(long batchId, LocalDate date);

    /**
     * Find all active discounts within a date range
     */
    List<Discount> findActiveDiscountsInDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Delete a discount by ID
     */
    boolean delete(long id);

    /**
     * Find all discounts (for management interface)
     */
    List<Discount> findAll();

    /**
     * Find discounts by product code (through batch relationship)
     */
    List<Discount> findByProductCode(String productCode);
}
