CREATE TABLE IF NOT EXISTS audit_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    framework_tag VARCHAR(100),
    project_year INT NOT NULL,
    notes VARCHAR(2000),
    starts_at TIMESTAMP,
    due_at TIMESTAMP,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_project_applications (
    audit_project_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    PRIMARY KEY (audit_project_id, application_id),
    FOREIGN KEY (audit_project_id) REFERENCES audit_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);

ALTER TABLE audits ADD COLUMN IF NOT EXISTS audit_project_id BIGINT;
ALTER TABLE audits ADD CONSTRAINT fk_audits_project
    FOREIGN KEY (audit_project_id) REFERENCES audit_projects(id);

CREATE INDEX IF NOT EXISTS idx_audit_projects_year ON audit_projects(project_year);
CREATE INDEX IF NOT EXISTS idx_audit_projects_status ON audit_projects(status);
CREATE INDEX IF NOT EXISTS idx_audits_project_id ON audits(audit_project_id);
