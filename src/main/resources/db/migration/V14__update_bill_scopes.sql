-- Update bill number scopes to use ONLINE instead of WEB
UPDATE bill_number SET scope = 'ONLINE' WHERE scope = 'WEB';

-- Ensure we have both COUNTER and ONLINE scopes
INSERT IGNORE INTO bill_number(scope, next_val) VALUES ('ONLINE', 1);
