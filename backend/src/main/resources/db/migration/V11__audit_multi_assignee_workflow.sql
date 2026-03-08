CREATE TABLE IF NOT EXISTS audit_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assignment_role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (audit_id, user_id, assignment_role),
    FOREIGN KEY (audit_id) REFERENCES audits(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_assignments_audit_id ON audit_assignments(audit_id);
CREATE INDEX IF NOT EXISTS idx_audit_assignments_user_id ON audit_assignments(user_id);
