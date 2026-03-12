CREATE TABLE IF NOT EXISTS automation_execution_locks (
    lock_name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL
);

ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500);

ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS file_content BLOB;

CREATE INDEX IF NOT EXISTS idx_audit_evidences_storage_key ON audit_evidences(storage_key);
