ALTER TABLE offers DROP CONSTRAINT IF EXISTS offers_status_check;

ALTER TABLE offers
  ADD CONSTRAINT offers_status_check
  CHECK (status IN ('DRAFT', 'PENDING_DELIVERY', 'SENT', 'DELIVERY_FAILED'));
