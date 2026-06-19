UPDATE offers
SET status = 'SENT'
WHERE status IN ('ACCEPTED', 'REJECTED', 'EXPIRED');

UPDATE offers
SET status = 'PENDING_DELIVERY'
WHERE status = 'DRAFT';
