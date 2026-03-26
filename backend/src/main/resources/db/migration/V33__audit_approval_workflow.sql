CREATE TABLE audit_approval_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    assigned_user_id BIGINT NOT NULL,
    step_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    decision_notes VARCHAR(4000),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_audit_approval_audit FOREIGN KEY (audit_id) REFERENCES audits(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_approval_user FOREIGN KEY (assigned_user_id) REFERENCES users(id),
    CONSTRAINT uk_audit_approval_order UNIQUE (audit_id, step_order)
);

CREATE INDEX idx_audit_approval_audit ON audit_approval_steps(audit_id, step_order);

UPDATE audits SET status = 'PENDING_APPROVAL' WHERE status = 'SUBMITTED';
