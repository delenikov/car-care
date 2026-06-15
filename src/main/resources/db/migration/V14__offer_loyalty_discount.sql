ALTER TABLE offers ADD COLUMN subtotal_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE offers ADD COLUMN discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE offers ADD COLUMN discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE offers
SET subtotal_amount = amount
WHERE subtotal_amount = 0;
