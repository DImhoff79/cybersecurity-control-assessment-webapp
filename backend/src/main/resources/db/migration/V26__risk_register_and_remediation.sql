CREATE TABLE risk_register_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(4000),
    business_impact VARCHAR(3000),
    likelihood_score INT NOT NULL,
    impact_score INT NOT NULL,
    inherent_risk_score INT NOT NULL,
    residual_risk_score INT,
    owner_user_id BIGINT,
    application_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    target_close_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_risk_application FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_risk_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE risk_control_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id BIGINT NOT NULL,
    control_id BIGINT NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_control_risk FOREIGN KEY (risk_id) REFERENCES risk_register_items(id),
    CONSTRAINT fk_risk_control_control FOREIGN KEY (control_id) REFERENCES controls(id)
);

CREATE TABLE risk_finding_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id BIGINT NOT NULL,
    finding_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_finding_risk FOREIGN KEY (risk_id) REFERENCES risk_register_items(id),
    CONSTRAINT fk_risk_finding_finding FOREIGN KEY (finding_id) REFERENCES findings(id)
);

CREATE TABLE risk_exception_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id BIGINT NOT NULL,
    exception_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_exception_risk FOREIGN KEY (risk_id) REFERENCES risk_register_items(id),
    CONSTRAINT fk_risk_exception_exception FOREIGN KEY (exception_id) REFERENCES control_exceptions(id)
);

CREATE TABLE remediation_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id BIGINT,
    title VARCHAR(300) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    target_complete_at TIMESTAMP,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_remediation_plan_risk FOREIGN KEY (risk_id) REFERENCES risk_register_items(id),
    CONSTRAINT fk_remediation_plan_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE remediation_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    action_title VARCHAR(300) NOT NULL,
    action_detail VARCHAR(4000),
    owner_user_id BIGINT,
    due_at TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    sequence_no INT NOT NULL DEFAULT 1,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_remediation_action_plan FOREIGN KEY (plan_id) REFERENCES remediation_plans(id),
    CONSTRAINT fk_remediation_action_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE remediation_action_dependencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    predecessor_action_id BIGINT NOT NULL,
    successor_action_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_remediation_dependency_plan FOREIGN KEY (plan_id) REFERENCES remediation_plans(id),
    CONSTRAINT fk_remediation_dependency_predecessor FOREIGN KEY (predecessor_action_id) REFERENCES remediation_actions(id),
    CONSTRAINT fk_remediation_dependency_successor FOREIGN KEY (successor_action_id) REFERENCES remediation_actions(id)
);

CREATE INDEX idx_risk_status_residual ON risk_register_items(status, residual_risk_score);
CREATE INDEX idx_remediation_action_due_status ON remediation_actions(status, due_at);

INSERT INTO user_permissions (user_id, permission)
SELECT u.id, 'RISK_MANAGEMENT'
FROM users u
WHERE u.role IN ('ADMIN', 'AUDIT_MANAGER')
  AND NOT EXISTS (
    SELECT 1 FROM user_permissions up
    WHERE up.user_id = u.id AND up.permission = 'RISK_MANAGEMENT'
);

INSERT INTO user_permissions (user_id, permission)
SELECT u.id, 'REMEDIATION_MANAGEMENT'
FROM users u
WHERE u.role IN ('ADMIN', 'AUDIT_MANAGER', 'AUDITOR')
  AND NOT EXISTS (
    SELECT 1 FROM user_permissions up
    WHERE up.user_id = u.id AND up.permission = 'REMEDIATION_MANAGEMENT'
);
