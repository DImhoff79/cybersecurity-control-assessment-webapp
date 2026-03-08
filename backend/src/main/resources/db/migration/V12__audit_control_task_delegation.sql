CREATE TABLE IF NOT EXISTS audit_control_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_control_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assignment_role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (audit_control_id, user_id, assignment_role),
    FOREIGN KEY (audit_control_id) REFERENCES audit_controls(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_control_assignments_control_id ON audit_control_assignments(audit_control_id);
CREATE INDEX IF NOT EXISTS idx_audit_control_assignments_user_id ON audit_control_assignments(user_id);
