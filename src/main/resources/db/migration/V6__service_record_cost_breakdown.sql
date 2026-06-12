ALTER TABLE service_records ADD COLUMN parts_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE service_records ADD COLUMN labor_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE service_records ADD COLUMN replaced_parts VARCHAR(2000);

UPDATE service_records
SET labor_cost = total_amount
WHERE parts_cost = 0 AND labor_cost = 0;

CREATE INDEX idx_service_records_customer ON service_records(customer_id);
