CREATE TABLE findings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL,
    audit_control_id BIGINT,
    title VARCHAR(500) NOT NULL,
    description VARCHAR(4000),
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    owner_user_id BIGINT,
    due_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_findings_audit FOREIGN KEY (audit_id) REFERENCES audits(id),
    CONSTRAINT fk_findings_audit_control FOREIGN KEY (audit_control_id) REFERENCES audit_controls(id),
    CONSTRAINT fk_findings_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_findings_audit_id ON findings(audit_id);
CREATE INDEX idx_findings_status ON findings(status);
CREATE INDEX idx_findings_severity ON findings(severity);
CREATE INDEX idx_findings_owner_user_id ON findings(owner_user_id);
