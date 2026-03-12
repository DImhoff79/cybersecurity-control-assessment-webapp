CREATE TABLE control_exceptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL,
    audit_control_id BIGINT,
    requested_by_user_id BIGINT NOT NULL,
    decided_by_user_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(2000) NOT NULL,
    compensating_control VARCHAR(2000),
    decision_notes VARCHAR(2000),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_control_exceptions_audit FOREIGN KEY (audit_id) REFERENCES audits(id),
    CONSTRAINT fk_control_exceptions_audit_control FOREIGN KEY (audit_control_id) REFERENCES audit_controls(id),
    CONSTRAINT fk_control_exceptions_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_control_exceptions_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_control_exceptions_audit_id ON control_exceptions(audit_id);
CREATE INDEX idx_control_exceptions_status ON control_exceptions(status);
CREATE INDEX idx_control_exceptions_expires_at ON control_exceptions(expires_at);
