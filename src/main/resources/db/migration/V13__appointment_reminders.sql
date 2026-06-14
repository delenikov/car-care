ALTER TABLE appointments ADD COLUMN reminder_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_appointments_reminder_candidates ON appointments(status, reminder_sent_at, scheduled_at);
