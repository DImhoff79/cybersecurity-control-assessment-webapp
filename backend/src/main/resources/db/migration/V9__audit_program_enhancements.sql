ALTER TABLE applications ADD COLUMN IF NOT EXISTS criticality VARCHAR(50);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_classification VARCHAR(50);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS regulatory_scope VARCHAR(500);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS business_owner_name VARCHAR(255);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS technical_owner_name VARCHAR(255);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(50);

ALTER TABLE audits ADD COLUMN IF NOT EXISTS due_at TIMESTAMP;
ALTER TABLE audits ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP;
ALTER TABLE audits ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMP;
ALTER TABLE audits ADD COLUMN IF NOT EXISTS attested_at TIMESTAMP;
ALTER TABLE audits ADD COLUMN IF NOT EXISTS attested_by_user_id BIGINT;
ALTER TABLE audits ADD COLUMN IF NOT EXISTS attestation_statement VARCHAR(2000);

ALTER TABLE audits
    ADD CONSTRAINT IF NOT EXISTS fk_audits_attested_by
        FOREIGN KEY (attested_by_user_id) REFERENCES users(id);

CREATE TABLE IF NOT EXISTS audit_evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_control_id BIGINT NOT NULL,
    evidence_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    uri VARCHAR(2000),
    source VARCHAR(255),
    owner VARCHAR(255),
    notes VARCHAR(2000),
    collected_at TIMESTAMP,
    expires_at TIMESTAMP,
    review_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id BIGINT,
    reviewed_at TIMESTAMP,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (audit_control_id) REFERENCES audit_controls(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_evidences_audit_control_id ON audit_evidences(audit_control_id);
CREATE INDEX IF NOT EXISTS idx_audit_evidences_review_status ON audit_evidences(review_status);

CREATE TABLE IF NOT EXISTS audit_questionnaire_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT,
    FOREIGN KEY (audit_id) REFERENCES audits(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_questionnaire_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    audit_control_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    control_id BIGINT NOT NULL,
    question_text VARCHAR(2000) NOT NULL,
    help_text VARCHAR(2000),
    display_order INT NOT NULL DEFAULT 0,
    ask_owner BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (snapshot_id) REFERENCES audit_questionnaire_snapshots(id) ON DELETE CASCADE,
    FOREIGN KEY (audit_control_id) REFERENCES audit_controls(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (control_id) REFERENCES controls(id),
    UNIQUE (snapshot_id, audit_control_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_questionnaire_items_snapshot ON audit_questionnaire_items(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_audit_questionnaire_items_audit_control ON audit_questionnaire_items(audit_control_id);

CREATE TABLE IF NOT EXISTS audit_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL,
    actor_user_id BIGINT,
    activity_type VARCHAR(100) NOT NULL,
    details VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (audit_id) REFERENCES audits(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_activity_logs_audit_id ON audit_activity_logs(audit_id);
CREATE INDEX IF NOT EXISTS idx_audit_activity_logs_created_at ON audit_activity_logs(created_at);
