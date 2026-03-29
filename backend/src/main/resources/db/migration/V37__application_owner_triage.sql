-- Owner triage: routing metadata + data-type scope (PHI/PII/PCI) for assessments and follow-ups.
ALTER TABLE applications ADD COLUMN IF NOT EXISTS application_purpose VARCHAR(40);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS hosting_model VARCHAR(40);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS user_audience_scope VARCHAR(40);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS integration_scope VARCHAR(40);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_scope_pii BOOLEAN;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_scope_phi BOOLEAN;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_scope_pci BOOLEAN;
