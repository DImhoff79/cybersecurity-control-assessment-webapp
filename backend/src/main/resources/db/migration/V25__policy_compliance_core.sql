CREATE TABLE policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(240) NOT NULL,
    description VARCHAR(2000),
    owner_user_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_version_id BIGINT,
    effective_at TIMESTAMP,
    next_review_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_policies_code UNIQUE (code),
    CONSTRAINT fk_policies_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE policy_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(240) NOT NULL,
    body_markdown VARCHAR(20000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id BIGINT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_versions_policy FOREIGN KEY (policy_id) REFERENCES policies(id),
    CONSTRAINT fk_policy_versions_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT uk_policy_versions_policy_version UNIQUE (policy_id, version_number)
);

ALTER TABLE policies
    ADD CONSTRAINT fk_policies_published_version
        FOREIGN KEY (published_version_id) REFERENCES policy_versions(id);

CREATE TABLE policy_acknowledgements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    policy_version_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    due_at TIMESTAMP,
    acknowledged_at TIMESTAMP,
    reminder_sent_at TIMESTAMP,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_ack_policy FOREIGN KEY (policy_id) REFERENCES policies(id),
    CONSTRAINT fk_policy_ack_policy_version FOREIGN KEY (policy_version_id) REFERENCES policy_versions(id),
    CONSTRAINT fk_policy_ack_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_policy_ack_unique UNIQUE (policy_version_id, user_id)
);

CREATE TABLE regulations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(240) NOT NULL,
    version VARCHAR(120),
    description VARCHAR(3000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_regulations_code UNIQUE (code)
);

CREATE TABLE compliance_requirements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    regulation_id BIGINT NOT NULL,
    requirement_code VARCHAR(120) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(4000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_requirements_regulation FOREIGN KEY (regulation_id) REFERENCES regulations(id),
    CONSTRAINT uk_regulation_requirement_code UNIQUE (regulation_id, requirement_code)
);

CREATE TABLE requirement_control_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_id BIGINT NOT NULL,
    control_id BIGINT NOT NULL,
    coverage_pct INT NOT NULL DEFAULT 100,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_control_requirement FOREIGN KEY (requirement_id) REFERENCES compliance_requirements(id),
    CONSTRAINT fk_req_control_control FOREIGN KEY (control_id) REFERENCES controls(id)
);

CREATE TABLE policy_requirement_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_requirement_policy FOREIGN KEY (policy_id) REFERENCES policies(id),
    CONSTRAINT fk_policy_requirement_requirement FOREIGN KEY (requirement_id) REFERENCES compliance_requirements(id),
    CONSTRAINT uk_policy_requirement UNIQUE (policy_id, requirement_id)
);

CREATE INDEX idx_policy_status ON policies(status);
CREATE INDEX idx_policy_ack_user ON policy_acknowledgements(user_id, status, due_at);
CREATE INDEX idx_requirement_mapping_requirement ON requirement_control_mappings(requirement_id);
CREATE INDEX idx_policy_requirement_policy ON policy_requirement_mappings(policy_id);

INSERT INTO user_permissions (user_id, permission)
SELECT u.id, 'POLICY_MANAGEMENT'
FROM users u
WHERE u.role IN ('ADMIN', 'AUDIT_MANAGER')
  AND NOT EXISTS (
    SELECT 1 FROM user_permissions up
    WHERE up.user_id = u.id AND up.permission = 'POLICY_MANAGEMENT'
);

INSERT INTO user_permissions (user_id, permission)
SELECT u.id, 'COMPLIANCE_MANAGEMENT'
FROM users u
WHERE u.role IN ('ADMIN', 'AUDIT_MANAGER', 'AUDITOR')
  AND NOT EXISTS (
    SELECT 1 FROM user_permissions up
    WHERE up.user_id = u.id AND up.permission = 'COMPLIANCE_MANAGEMENT'
);
