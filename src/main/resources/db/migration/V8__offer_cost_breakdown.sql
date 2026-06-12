ALTER TABLE offers ADD COLUMN parts_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE offers ADD COLUMN labor_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE offers
SET labor_cost = amount
WHERE parts_cost = 0 AND labor_cost = 0;
