UPDATE customers
SET first_name = split_part(trim(full_name), ' ', 1)
WHERE first_name IS NULL OR trim(first_name) = '';

UPDATE customers
SET last_name = CASE
    WHEN position(' ' in trim(full_name)) > 0 THEN substring(trim(full_name) from position(' ' in trim(full_name)) + 1)
    ELSE trim(full_name)
  END
WHERE last_name IS NULL OR trim(last_name) = '';
