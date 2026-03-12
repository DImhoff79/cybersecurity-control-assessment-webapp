ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS evidence_version INT NOT NULL DEFAULT 1;
ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS retention_until TIMESTAMP;
ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS legal_hold BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;
ALTER TABLE audit_evidences
    ADD COLUMN IF NOT EXISTS disposed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_audit_evidence_lifecycle_status ON audit_evidences(lifecycle_status);
CREATE INDEX IF NOT EXISTS idx_audit_evidence_retention_until ON audit_evidences(retention_until);
