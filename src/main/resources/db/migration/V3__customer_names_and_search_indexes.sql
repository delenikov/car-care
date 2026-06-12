ALTER TABLE customers ADD COLUMN first_name VARCHAR(80);
ALTER TABLE customers ADD COLUMN last_name VARCHAR(80);

UPDATE customers
SET first_name = split_part(full_name, ' ', 1),
    last_name = CASE
      WHEN position(' ' in full_name) > 0 THEN substring(full_name from position(' ' in full_name) + 1)
      ELSE full_name
    END;

ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE customers ALTER COLUMN last_name SET NOT NULL;

CREATE INDEX idx_customers_first_name ON customers(first_name);
CREATE INDEX idx_customers_last_name ON customers(last_name);
