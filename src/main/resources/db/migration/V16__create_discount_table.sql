-- Create discount table linked to batches with date range support
CREATE TABLE discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    description VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (batch_id) REFERENCES batch(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_discount_batch (batch_id),
    INDEX idx_discount_dates (start_date, end_date),
    INDEX idx_discount_active (is_active)
);

-- Add constraint to ensure discount value is positive
ALTER TABLE discounts
ADD CONSTRAINT chk_discount_value_positive
CHECK (discount_value > 0);

-- Add constraint to ensure end_date is after start_date
ALTER TABLE discounts
ADD CONSTRAINT chk_discount_date_range
CHECK (end_date >= start_date);

-- Add constraint to ensure percentage discounts don't exceed 100%
ALTER TABLE discounts
ADD CONSTRAINT chk_percentage_discount
CHECK (discount_type != 'PERCENTAGE' OR discount_value <= 100);
