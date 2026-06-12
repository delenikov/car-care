ALTER TABLE appointments ADD COLUMN ends_at TIMESTAMP WITH TIME ZONE;
UPDATE appointments SET ends_at = scheduled_at + INTERVAL '1 hour';
ALTER TABLE appointments ALTER COLUMN ends_at SET NOT NULL;

ALTER TABLE appointments ADD COLUMN cancellation_token VARCHAR(80);
ALTER TABLE appointments ADD COLUMN cancellation_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE appointments ADD COLUMN cancellation_used_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX idx_appointments_cancellation_token ON appointments(cancellation_token);
CREATE INDEX idx_appointments_schedule_window ON appointments(scheduled_at, ends_at);
