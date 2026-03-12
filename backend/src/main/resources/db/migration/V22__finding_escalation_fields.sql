ALTER TABLE findings
    ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP;

ALTER TABLE findings
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_findings_due_at ON findings(due_at);
