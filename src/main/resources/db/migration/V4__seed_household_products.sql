-- Create category table first
CREATE TABLE IF NOT EXISTS category (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    prefix VARCHAR(10) NOT NULL UNIQUE,
    next_sequence INT NOT NULL DEFAULT 1,
    display_order INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add category_code column to product table (only if it doesn't exist)
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'product'
                      AND COLUMN_NAME = 'category_code');

SET @sql = IF(@column_exists = 0,
              'ALTER TABLE product ADD COLUMN category_code VARCHAR(20)',
              'SELECT "Column already exists" as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add foreign key constraint (only if column was added)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                         WHERE TABLE_SCHEMA = DATABASE()
                         AND TABLE_NAME = 'product'
                         AND CONSTRAINT_NAME = 'fk_product_category');

SET @sql = IF(@constraint_exists = 0 AND @column_exists = 0,
              'ALTER TABLE product ADD CONSTRAINT fk_product_category FOREIGN KEY (category_code) REFERENCES category(code)',
              'SELECT "Constraint already exists or column not added" as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Seed initial categories with meaningful prefixes
INSERT IGNORE INTO category (code, name, description, prefix, display_order) VALUES
('CLEANING', 'Cleaning Products', 'Household cleaning and sanitizing products', 'CLN', 1),
('KITCHEN', 'Kitchen Items', 'Kitchen accessories and disposable items', 'KTC', 2),
('PERSONAL_CARE', 'Personal Care', 'Health and hygiene products', 'PRC', 3),
('HOME_ESSENTIALS', 'Home Essentials', 'Basic household utilities and electronics', 'HME', 4),
('BEVERAGES', 'Beverages', 'Drinks and liquid refreshments', 'BEV', 5),
('SNACKS', 'Snacks & Confectionery', 'Snacks, candies and treats', 'SNK', 6);

-- Seed data for household products (idempotent)
INSERT IGNORE INTO product (code, name, price_cents, category_code) VALUES
-- Cleaning Products
('CLN001', 'All-Purpose Cleaner 500ml', 30000, 'CLEANING'),
('CLN002', 'Dish Soap 750ml', 19900, 'CLEANING'),
('CLN003', 'Laundry Detergent 1L', 59900, 'CLEANING'),
('CLN004', 'Toilet Paper 12-pack', 89900, 'CLEANING'),
('CLN005', 'Paper Towels 6-pack', 69900, 'CLEANING'),
('CLN006', 'Glass Cleaner 500ml', 24900, 'CLEANING'),
('CLN007', 'Floor Cleaner 1L', 39900, 'CLEANING'),
('CLN008', 'Bleach 1L', 29900, 'CLEANING'),
('CLN009', 'Fabric Softener 750ml', 45900, 'CLEANING'),
('CLN010', 'Bathroom Cleaner 500ml', 34900, 'CLEANING'),

-- Kitchen Items
('KTC001', 'Aluminum Foil 25ft', 34900, 'KITCHEN'),
('KTC002', 'Plastic Wrap 100ft', 27900, 'KITCHEN'),
('KTC003', 'Trash Bags 30-count', 79900, 'KITCHEN'),
('KTC004', 'Food Storage Containers Set', 129900, 'KITCHEN'),
('KTC005', 'Paper Plates 50-pack', 59900, 'KITCHEN'),
('KTC006', 'Disposable Cups 100-pack', 44900, 'KITCHEN'),
('KTC007', 'Kitchen Sponges 6-pack', 19900, 'KITCHEN'),
('KTC008', 'Dish Towels 3-pack', 24900, 'KITCHEN'),
('KTC009', 'Freezer Bags 25-count', 39900, 'KITCHEN'),
('KTC010', 'Parchment Paper Roll', 22900, 'KITCHEN'),

-- Personal Care
('PRC001', 'Shampoo 400ml', 69900, 'PERSONAL_CARE'),
('PRC002', 'Body Soap Bar 3-pack', 39900, 'PERSONAL_CARE'),
('PRC003', 'Toothpaste 100ml', 29900, 'PERSONAL_CARE'),
('PRC004', 'Deodorant Stick', 49900, 'PERSONAL_CARE'),
('PRC005', 'Hand Sanitizer 250ml', 19900, 'PERSONAL_CARE'),
('PRC006', 'Body Lotion 300ml', 54900, 'PERSONAL_CARE'),
('PRC007', 'Face Wash 150ml', 39900, 'PERSONAL_CARE'),
('PRC008', 'Toothbrush 2-pack', 19900, 'PERSONAL_CARE'),
('PRC009', 'Razor Disposable 5-pack', 29900, 'PERSONAL_CARE'),
('PRC010', 'Mouthwash 500ml', 34900, 'PERSONAL_CARE'),

-- Home Essentials
('HME001', 'Light Bulbs LED 4-pack', 119900, 'HOME_ESSENTIALS'),
('HME002', 'Batteries AA 8-pack', 89900, 'HOME_ESSENTIALS'),
('HME003', 'Batteries AAA 8-pack', 89900, 'HOME_ESSENTIALS'),
('HME004', 'Extension Cord 6ft', 159900, 'HOME_ESSENTIALS'),
('HME005', 'Air Freshener Spray', 34900, 'HOME_ESSENTIALS'),
('HME006', 'Candles 3-pack', 24900, 'HOME_ESSENTIALS'),
('HME007', 'Matches 10-pack', 9900, 'HOME_ESSENTIALS'),
('HME008', 'Light Switch Covers 5-pack', 14900, 'HOME_ESSENTIALS'),
('HME009', 'Power Strip 6-outlet', 199900, 'HOME_ESSENTIALS'),
('HME010', 'Duct Tape Roll', 19900, 'HOME_ESSENTIALS'),

-- Beverages
('BEV001', 'Bottled Water 24-pack', 99900, 'BEVERAGES'),
('BEV002', 'Orange Juice 1L', 39900, 'BEVERAGES'),
('BEV003', 'Coffee Grounds 500g', 89900, 'BEVERAGES'),
('BEV004', 'Tea Bags 100-count', 29900, 'BEVERAGES'),
('BEV005', 'Energy Drink 4-pack', 79900, 'BEVERAGES'),
('BEV006', 'Soda Cola 12-pack', 69900, 'BEVERAGES'),
('BEV007', 'Sports Drink 6-pack', 49900, 'BEVERAGES'),
('BEV008', 'Milk 1L', 34900, 'BEVERAGES'),
('BEV009', 'Instant Coffee 200g', 59900, 'BEVERAGES'),
('BEV010', 'Fruit Juice Mix 1L', 44900, 'BEVERAGES'),

-- Snacks & Confectionery
('SNK001', 'Potato Chips 150g', 24900, 'SNACKS'),
('SNK002', 'Chocolate Bar 100g', 19900, 'SNACKS'),
('SNK003', 'Cookies Pack 300g', 34900, 'SNACKS'),
('SNK004', 'Nuts Mix 200g', 49900, 'SNACKS'),
('SNK005', 'Candy Assorted 250g', 29900, 'SNACKS'),
('SNK006', 'Crackers Pack 200g', 22900, 'SNACKS'),
('SNK007', 'Granola Bars 6-pack', 39900, 'SNACKS'),
('SNK008', 'Dried Fruits 150g', 44900, 'SNACKS'),
('SNK009', 'Popcorn 3-pack', 19900, 'SNACKS'),
('SNK010', 'Chewing Gum 5-pack', 14900, 'SNACKS');

-- Update next_sequence for categories based on existing products
UPDATE category c SET next_sequence = (
    SELECT COALESCE(MAX(CAST(SUBSTRING(p.code, LENGTH(c.prefix) + 1) AS UNSIGNED)), 0) + 1
    FROM product p
    WHERE p.code LIKE CONCAT(c.prefix, '%')
    AND p.code REGEXP CONCAT('^', c.prefix, '[0-9]+$')
);

-- Seed data for product batches (idempotent with unique index on batch identity)
INSERT IGNORE INTO batch (product_code, location, received_at, expiry, quantity, version) VALUES
-- MAIN_STORE Batches (Initial supplier deliveries)
-- Cleaning Products
('CLN001', 'MAIN_STORE', '2024-07-28 08:00:00', '2026-08-01', 200, 0),
('CLN002', 'MAIN_STORE', '2024-08-01 09:00:00', '2025-12-31', 150, 0),
('CLN003', 'MAIN_STORE', '2024-07-15 07:30:00', '2025-07-20', 180, 0),
('CLN004', 'MAIN_STORE', '2024-08-05 10:00:00', NULL, 300, 0),
('CLN005', 'MAIN_STORE', '2024-08-03 11:00:00', NULL, 250, 0),
('CLN006', 'MAIN_STORE', '2024-07-30 08:30:00', '2026-02-01', 120, 0),
('CLN007', 'MAIN_STORE', '2024-08-02 09:15:00', '2025-11-30', 160, 0),
('CLN008', 'MAIN_STORE', '2024-08-10 10:30:00', '2026-05-15', 140, 0),
('CLN009', 'MAIN_STORE', '2024-08-12 14:20:00', '2025-09-30', 130, 0),
('CLN010', 'MAIN_STORE', '2024-08-15 11:45:00', '2026-03-20', 110, 0),

-- Kitchen Items
('KTC001', 'MAIN_STORE', '2024-08-08 08:45:00', NULL, 220, 0),
('KTC002', 'MAIN_STORE', '2024-08-06 10:20:00', NULL, 200, 0),
('KTC003', 'MAIN_STORE', '2024-08-04 14:30:00', NULL, 280, 0),
('KTC004', 'MAIN_STORE', '2024-07-20 13:00:00', NULL, 100, 0),
('KTC005', 'MAIN_STORE', '2024-08-07 09:00:00', NULL, 350, 0),
('KTC006', 'MAIN_STORE', '2024-08-02 11:30:00', NULL, 500, 0),
('KTC007', 'MAIN_STORE', '2024-08-14 16:15:00', NULL, 400, 0),
('KTC008', 'MAIN_STORE', '2024-08-16 09:30:00', NULL, 180, 0),
('KTC009', 'MAIN_STORE', '2024-08-18 13:45:00', NULL, 250, 0),
('KTC010', 'MAIN_STORE', '2024-08-20 10:15:00', NULL, 160, 0),

-- Personal Care
('PRC001', 'MAIN_STORE', '2024-07-25 08:30:00', '2025-08-02', 180, 0),
('PRC002', 'MAIN_STORE', '2024-07-28 09:15:00', '2026-01-15', 240, 0),
('PRC003', 'MAIN_STORE', '2024-07-22 11:45:00', '2025-07-28', 300, 0),
('PRC004', 'MAIN_STORE', '2024-07-26 14:20:00', '2025-06-01', 150, 0),
('PRC005', 'MAIN_STORE', '2024-08-10 10:10:00', '2025-03-18', 200, 0),
('PRC006', 'MAIN_STORE', '2024-08-12 15:30:00', '2025-12-01', 170, 0),
('PRC007', 'MAIN_STORE', '2024-08-14 12:20:00', '2025-10-15', 190, 0),
('PRC008', 'MAIN_STORE', '2024-08-16 11:40:00', NULL, 220, 0),
('PRC009', 'MAIN_STORE', '2024-08-18 09:25:00', '2026-02-20', 160, 0),
('PRC010', 'MAIN_STORE', '2024-08-20 14:10:00', '2025-08-25', 140, 0),

-- Home Essentials
('HME001', 'MAIN_STORE', '2024-08-05 09:00:00', NULL, 120, 0),
('HME002', 'MAIN_STORE', '2024-07-30 13:15:00', '2027-08-05', 180, 0),
('HME003', 'MAIN_STORE', '2024-07-30 13:20:00', '2027-08-05', 180, 0),
('HME004', 'MAIN_STORE', '2024-07-25 16:30:00', NULL, 80, 0),
('HME005', 'MAIN_STORE', '2024-08-12 11:30:00', '2025-02-17', 160, 0),
('HME006', 'MAIN_STORE', '2024-08-14 13:45:00', NULL, 200, 0),
('HME007', 'MAIN_STORE', '2024-08-16 10:20:00', NULL, 300, 0),
('HME008', 'MAIN_STORE', '2024-08-18 15:10:00', NULL, 150, 0),
('HME009', 'MAIN_STORE', '2024-08-20 12:30:00', NULL, 80, 0),
('HME010', 'MAIN_STORE', '2024-08-22 09:40:00', NULL, 120, 0),

-- Beverages
('BEV001', 'MAIN_STORE', '2024-08-01 07:00:00', '2025-08-01', 500, 0),
('BEV002', 'MAIN_STORE', '2024-08-03 08:15:00', '2025-01-15', 200, 0),
('BEV003', 'MAIN_STORE', '2024-08-05 09:30:00', '2025-12-31', 150, 0),
('BEV004', 'MAIN_STORE', '2024-08-07 10:45:00', '2026-06-30', 250, 0),
('BEV005', 'MAIN_STORE', '2024-08-09 12:00:00', '2025-11-20', 180, 0),
('BEV006', 'MAIN_STORE', '2024-08-11 13:15:00', '2025-10-10', 300, 0),
('BEV007', 'MAIN_STORE', '2024-08-13 14:30:00', '2025-09-05', 220, 0),
('BEV008', 'MAIN_STORE', '2024-08-15 15:45:00', '2025-01-30', 150, 0),
('BEV009', 'MAIN_STORE', '2024-08-17 16:00:00', '2025-12-15', 180, 0),
('BEV010', 'MAIN_STORE', '2024-08-19 17:15:00', '2025-02-28', 160, 0),

-- Snacks
('SNK001', 'MAIN_STORE', '2024-08-02 08:00:00', '2025-02-28', 300, 0),
('SNK002', 'MAIN_STORE', '2024-08-04 09:15:00', '2025-06-30', 250, 0),
('SNK003', 'MAIN_STORE', '2024-08-06 10:30:00', '2025-04-15', 200, 0),
('SNK004', 'MAIN_STORE', '2024-08-08 11:45:00', '2025-12-31', 180, 0),
('SNK005', 'MAIN_STORE', '2024-08-10 13:00:00', '2025-08-20', 220, 0),
('SNK006', 'MAIN_STORE', '2024-08-12 14:15:00', '2025-05-10', 190, 0),
('SNK007', 'MAIN_STORE', '2024-08-14 15:30:00', '2025-11-05', 160, 0),
('SNK008', 'MAIN_STORE', '2024-08-16 16:45:00', '2025-09-25', 140, 0),
('SNK009', 'MAIN_STORE', '2024-08-18 17:00:00', '2025-07-15', 250, 0),
('SNK010', 'MAIN_STORE', '2024-08-20 18:15:00', '2026-01-10', 300, 0),

-- SHELF Batches (Distributed from main store)
-- Cleaning Products
('CLN001', 'SHELF', '2024-08-01 09:00:00', '2026-08-01', 50, 0),
('CLN002', 'SHELF', '2024-08-05 11:00:00', '2025-12-31', 75, 0),
('CLN003', 'SHELF', '2024-07-20 08:00:00', '2025-07-20', 40, 0),
('CLN004', 'SHELF', '2024-08-12 16:00:00', NULL, 100, 0),
('CLN005', 'SHELF', '2024-08-08 12:00:00', NULL, 60, 0),
('CLN006', 'SHELF', '2024-08-03 13:30:00', '2026-02-01', 35, 0),
('CLN007', 'SHELF', '2024-08-06 10:15:00', '2025-11-30', 45, 0),
('CLN008', 'SHELF', '2024-08-15 11:30:00', '2026-05-15', 40, 0),
('CLN009', 'SHELF', '2024-08-17 15:20:00', '2025-09-30', 35, 0),
('CLN010', 'SHELF', '2024-08-20 12:45:00', '2026-03-20', 30, 0),

-- Kitchen Items
('KTC001', 'SHELF', '2024-08-14 09:45:00', NULL, 80, 0),
('KTC002', 'SHELF', '2024-08-11 11:20:00', NULL, 70, 0),
('KTC003', 'SHELF', '2024-08-09 15:30:00', NULL, 90, 0),
('KTC004', 'SHELF', '2024-07-25 14:00:00', NULL, 25, 0),
('KTC005', 'SHELF', '2024-08-13 10:00:00', NULL, 120, 0),
('KTC006', 'SHELF', '2024-08-07 12:30:00', NULL, 200, 0),
('KTC007', 'SHELF', '2024-08-19 17:15:00', NULL, 150, 0),
('KTC008', 'SHELF', '2024-08-21 10:30:00', NULL, 60, 0),
('KTC009', 'SHELF', '2024-08-23 14:45:00', NULL, 80, 0),
('KTC010', 'SHELF', '2024-08-25 11:15:00', NULL, 50, 0),

-- Personal Care
('PRC001', 'SHELF', '2024-08-02 08:30:00', '2025-08-02', 60, 0),
('PRC002', 'SHELF', '2024-08-04 09:15:00', '2026-01-15', 80, 0),
('PRC003', 'SHELF', '2024-07-28 11:45:00', '2025-07-28', 100, 0),
('PRC004', 'SHELF', '2024-08-01 14:20:00', '2025-06-01', 45, 0),
('PRC005', 'SHELF', '2024-08-18 10:10:00', '2025-03-18', 70, 0),
('PRC006', 'SHELF', '2024-08-17 16:30:00', '2025-12-01', 50, 0),
('PRC007', 'SHELF', '2024-08-19 13:20:00', '2025-10-15', 55, 0),
('PRC008', 'SHELF', '2024-08-21 12:40:00', NULL, 70, 0),
('PRC009', 'SHELF', '2024-08-23 10:25:00', '2026-02-20', 45, 0),
('PRC010', 'SHELF', '2024-08-25 15:10:00', '2025-08-25', 40, 0),

-- Home Essentials
('HME001', 'SHELF', '2024-08-10 09:00:00', NULL, 40, 0),
('HME002', 'SHELF', '2024-08-05 13:15:00', '2027-08-05', 60, 0),
('HME003', 'SHELF', '2024-08-05 13:20:00', '2027-08-05', 60, 0),
('HME004', 'SHELF', '2024-07-30 16:30:00', NULL, 20, 0),
('HME005', 'SHELF', '2024-08-17 11:30:00', '2025-02-17', 55, 0),
('HME006', 'SHELF', '2024-08-19 14:45:00', NULL, 70, 0),
('HME007', 'SHELF', '2024-08-21 11:20:00', NULL, 100, 0),
('HME008', 'SHELF', '2024-08-23 16:10:00', NULL, 50, 0),
('HME009', 'SHELF', '2024-08-25 13:30:00', NULL, 25, 0),
('HME010', 'SHELF', '2024-08-27 10:40:00', NULL, 40, 0),

-- Beverages
('BEV001', 'SHELF', '2024-08-06 08:00:00', '2025-08-01', 150, 0),
('BEV002', 'SHELF', '2024-08-08 09:15:00', '2025-01-15', 60, 0),
('BEV003', 'SHELF', '2024-08-10 10:30:00', '2025-12-31', 45, 0),
('BEV004', 'SHELF', '2024-08-12 11:45:00', '2026-06-30', 80, 0),
('BEV005', 'SHELF', '2024-08-14 13:00:00', '2025-11-20', 55, 0),
('BEV006', 'SHELF', '2024-08-16 14:15:00', '2025-10-10', 90, 0),
('BEV007', 'SHELF', '2024-08-18 15:30:00', '2025-09-05', 65, 0),
('BEV008', 'SHELF', '2024-08-20 16:45:00', '2025-01-30', 45, 0),
('BEV009', 'SHELF', '2024-08-22 17:00:00', '2025-12-15', 55, 0),
('BEV010', 'SHELF', '2024-08-24 18:15:00', '2025-02-28', 50, 0),

-- Snacks
('SNK001', 'SHELF', '2024-08-07 09:00:00', '2025-02-28', 100, 0),
('SNK002', 'SHELF', '2024-08-09 10:15:00', '2025-06-30', 80, 0),
('SNK003', 'SHELF', '2024-08-11 11:30:00', '2025-04-15', 65, 0),
('SNK004', 'SHELF', '2024-08-13 12:45:00', '2025-12-31', 55, 0),
('SNK005', 'SHELF', '2024-08-15 14:00:00', '2025-08-20', 70, 0),
('SNK006', 'SHELF', '2024-08-17 15:15:00', '2025-05-10', 60, 0),
('SNK007', 'SHELF', '2024-08-19 16:30:00', '2025-11-05', 50, 0),
('SNK008', 'SHELF', '2024-08-21 17:45:00', '2025-09-25', 45, 0),
('SNK009', 'SHELF', '2024-08-23 18:00:00', '2025-07-15', 80, 0),
('SNK010', 'SHELF', '2024-08-25 19:15:00', '2026-01-10', 90, 0),

-- WEB Batches (For online sales)
-- Cleaning Products
('CLN001', 'WEB', '2024-08-15 10:30:00', '2026-08-01', 25, 0),
('CLN003', 'WEB', '2024-08-10 14:00:00', '2025-07-20', 30, 0),
('CLN005', 'WEB', '2024-08-20 12:00:00', NULL, 35, 0),
('CLN008', 'WEB', '2024-08-22 11:30:00', '2026-05-15', 20, 0),
('CLN009', 'WEB', '2024-08-24 15:20:00', '2025-09-30', 25, 0),

-- Kitchen Items
('KTC003', 'WEB', '2024-08-20 16:45:00', NULL, 40, 0),
('KTC005', 'WEB', '2024-08-22 10:00:00', NULL, 60, 0),
('KTC007', 'WEB', '2024-08-24 17:15:00', NULL, 75, 0),
('KTC009', 'WEB', '2024-08-26 14:45:00', NULL, 45, 0),
('KTC010', 'WEB', '2024-08-28 11:15:00', NULL, 30, 0),

-- Personal Care
('PRC001', 'WEB', '2024-08-16 13:00:00', '2025-08-02', 35, 0),
('PRC005', 'WEB', '2024-08-22 15:00:00', '2025-03-18', 30, 0),
('PRC006', 'WEB', '2024-08-24 16:30:00', '2025-12-01', 25, 0),
('PRC007', 'WEB', '2024-08-26 13:20:00', '2025-10-15', 30, 0),
('PRC009', 'WEB', '2024-08-28 10:25:00', '2026-02-20', 25, 0),

-- Home Essentials
('HME005', 'WEB', '2024-08-19 14:45:00', '2025-02-17', 25, 0),
('HME006', 'WEB', '2024-08-21 14:45:00', NULL, 35, 0),
('HME007', 'WEB', '2024-08-23 11:20:00', NULL, 50, 0),
('HME009', 'WEB', '2024-08-27 13:30:00', NULL, 15, 0),
('HME010', 'WEB', '2024-08-29 10:40:00', NULL, 20, 0),

-- Beverages
('BEV001', 'WEB', '2024-08-11 08:00:00', '2025-08-01', 80, 0),
('BEV003', 'WEB', '2024-08-15 10:30:00', '2025-12-31', 25, 0),
('BEV005', 'WEB', '2024-08-19 13:00:00', '2025-11-20', 30, 0),
('BEV006', 'WEB', '2024-08-21 14:15:00', '2025-10-10', 45, 0),
('BEV009', 'WEB', '2024-08-27 17:00:00', '2025-12-15', 30, 0),

-- Snacks
('SNK001', 'WEB', '2024-08-12 09:00:00', '2025-02-28', 50, 0),
('SNK002', 'WEB', '2024-08-14 10:15:00', '2025-06-30', 40, 0),
('SNK004', 'WEB', '2024-08-18 12:45:00', '2025-12-31', 30, 0),
('SNK007', 'WEB', '2024-08-24 16:30:00', '2025-11-05', 25, 0),
('SNK010', 'WEB', '2024-08-30 19:15:00', '2026-01-10', 45, 0);
