package application.usecase;

import domain.pricing.Discount;
import domain.repository.DiscountRepository;
import domain.user.User;
import domain.user.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Use case for managing discounts - restricted to managers only
 */
public class DiscountManagementUseCase {
    private final DiscountRepository discountRepository;

    public DiscountManagementUseCase(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    /**
     * Create a new discount - only managers can do this
     */
    public CreateDiscountResult createDiscount(CreateDiscountRequest request, User currentUser) {
        if (!isManager(currentUser)) {
            return CreateDiscountResult.failure("Only managers can create discounts");
        }

        // Validate request
        ValidationResult validation = validateDiscountRequest(request);
        if (!validation.isValid()) {
            return CreateDiscountResult.failure(validation.getErrorMessage());
        }

        try {
            Discount discount = new Discount(
                0,
                request.getBatchId(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.isActive(),
                request.getDescription(),
                currentUser.id()
            );

            Discount savedDiscount = discountRepository.save(discount);
            return CreateDiscountResult.success(savedDiscount);
        } catch (Exception e) {
            return CreateDiscountResult.failure("Failed to create discount: " + e.getMessage());
        }
    }

    /**
     * Update an existing discount - only managers can do this
     */
    public UpdateDiscountResult updateDiscount(UpdateDiscountRequest request, User currentUser) {
        if (!isManager(currentUser)) {
            return UpdateDiscountResult.failure("Only managers can update discounts");
        }

        Optional<Discount> existingDiscount = discountRepository.findById(request.getDiscountId());
        if (existingDiscount.isEmpty()) {
            return UpdateDiscountResult.failure("Discount not found");
        }

        // Validate request
        ValidationResult validation = validateDiscountRequest(request);
        if (!validation.isValid()) {
            return UpdateDiscountResult.failure(validation.getErrorMessage());
        }

        try {
            Discount updatedDiscount = new Discount(
                request.getDiscountId(),
                request.getBatchId(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.isActive(),
                request.getDescription(),
                existingDiscount.get().getCreatedBy()
            );

            Discount saved = discountRepository.update(updatedDiscount);
            return UpdateDiscountResult.success(saved);
        } catch (Exception e) {
            return UpdateDiscountResult.failure("Failed to update discount: " + e.getMessage());
        }
    }

    /**
     * Delete a discount - only managers can do this
     */
    public DeleteDiscountResult deleteDiscount(long discountId, User currentUser) {
        if (!isManager(currentUser)) {
            return DeleteDiscountResult.failure("Only managers can delete discounts");
        }

        try {
            boolean deleted = discountRepository.delete(discountId);
            if (deleted) {
                return DeleteDiscountResult.success();
            } else {
                return DeleteDiscountResult.failure("Discount not found");
            }
        } catch (Exception e) {
            return DeleteDiscountResult.failure("Failed to delete discount: " + e.getMessage());
        }
    }

    /**
     * Get all discounts - only managers can see all discounts
     */
    public List<Discount> getAllDiscounts(User currentUser) {
        if (!isManager(currentUser)) {
            throw new SecurityException("Only managers can view all discounts");
        }
        return discountRepository.findAll();
    }

    /**
     * Get discounts for a specific batch - managers only
     */
    public List<Discount> getDiscountsForBatch(long batchId, User currentUser) {
        if (!isManager(currentUser)) {
            throw new SecurityException("Only managers can view discount details");
        }
        return discountRepository.findByBatchId(batchId);
    }

    /**
     * Get active discounts for a batch on a specific date - used by pricing service
     */
    public List<Discount> getActiveDiscountsForBatch(long batchId, LocalDate date) {
        return discountRepository.findActiveDiscountsForBatch(batchId, date);
    }

    /**
     * Get discounts by product code - used for cart display
     */
    public List<Discount> getDiscountsByProductCode(String productCode) {
        return discountRepository.findByProductCode(productCode);
    }

    private boolean isManager(User user) {
        return user.role() == Role.MANAGER;
    }

    public ValidationResult validateDiscountRequest(DiscountRequestBase request) {
        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Discount value must be positive");
        }

        if (request.getDiscountType() == Discount.DiscountType.PERCENTAGE &&
            request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            return ValidationResult.invalid("Percentage discount cannot exceed 100%");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            return ValidationResult.invalid("Start date and end date are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            return ValidationResult.invalid("End date must be after start date");
        }

        if (request.getBatchId() <= 0) {
            return ValidationResult.invalid("Valid batch ID is required");
        }

        return ValidationResult.valid();
    }

    public static class CreateDiscountRequest extends DiscountRequestBase {
        public CreateDiscountRequest(long batchId, Discount.DiscountType discountType,
                                   BigDecimal discountValue, LocalDate startDate,
                                   LocalDate endDate, boolean isActive, String description) {
            super(batchId, discountType, discountValue, startDate, endDate, isActive, description);
        }
    }

    public static class UpdateDiscountRequest extends DiscountRequestBase {
        private final long discountId;

        public UpdateDiscountRequest(long discountId, long batchId, Discount.DiscountType discountType,
                                   BigDecimal discountValue, LocalDate startDate,
                                   LocalDate endDate, boolean isActive, String description) {
            super(batchId, discountType, discountValue, startDate, endDate, isActive, description);
            this.discountId = discountId;
        }

        public long getDiscountId() { return discountId; }
    }

    public static abstract class DiscountRequestBase {
        private final long batchId;
        private final Discount.DiscountType discountType;
        private final BigDecimal discountValue;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final boolean isActive;
        private final String description;

        protected DiscountRequestBase(long batchId, Discount.DiscountType discountType,
                                    BigDecimal discountValue, LocalDate startDate,
                                    LocalDate endDate, boolean isActive, String description) {
            this.batchId = batchId;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.startDate = startDate;
            this.endDate = endDate;
            this.isActive = isActive;
            this.description = description;
        }

        public long getBatchId() { return batchId; }
        public Discount.DiscountType getDiscountType() { return discountType; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public boolean isActive() { return isActive; }
        public String getDescription() { return description; }
    }

    public static class CreateDiscountResult {
        private final boolean success;
        private final String errorMessage;
        private final Discount discount;

        private CreateDiscountResult(boolean success, String errorMessage, Discount discount) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.discount = discount;
        }

        public static CreateDiscountResult success(Discount discount) {
            return new CreateDiscountResult(true, null, discount);
        }

        public static CreateDiscountResult failure(String errorMessage) {
            return new CreateDiscountResult(false, errorMessage, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Discount getDiscount() { return discount; }
    }

    public static class UpdateDiscountResult {
        private final boolean success;
        private final String errorMessage;
        private final Discount discount;

        private UpdateDiscountResult(boolean success, String errorMessage, Discount discount) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.discount = discount;
        }

        public static UpdateDiscountResult success(Discount discount) {
            return new UpdateDiscountResult(true, null, discount);
        }

        public static UpdateDiscountResult failure(String errorMessage) {
            return new UpdateDiscountResult(false, errorMessage, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Discount getDiscount() { return discount; }
    }

    public static class DeleteDiscountResult {
        private final boolean success;
        private final String errorMessage;

        private DeleteDiscountResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static DeleteDiscountResult success() {
            return new DeleteDiscountResult(true, null);
        }

        public static DeleteDiscountResult failure(String errorMessage) {
            return new DeleteDiscountResult(false, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
