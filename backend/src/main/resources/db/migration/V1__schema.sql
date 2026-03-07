CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    role VARCHAR(50) NOT NULL
);

CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    owner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE controls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    control_id VARCHAR(100) NOT NULL,
    name VARCHAR(500) NOT NULL,
    description VARCHAR(4000),
    framework VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    category VARCHAR(255)
);

CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    control_id BIGINT NOT NULL,
    question_text VARCHAR(2000) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    help_text VARCHAR(2000),
    FOREIGN KEY (control_id) REFERENCES controls(id)
);

CREATE TABLE audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    audit_year INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    assigned_to_user_id BIGINT,
    assigned_at TIMESTAMP,
    sent_at TIMESTAMP,
    UNIQUE (application_id, audit_year),
    FOREIGN KEY (application_id) REFERENCES applications(id),
    FOREIGN KEY (assigned_to_user_id) REFERENCES users(id)
);

CREATE TABLE audit_controls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id BIGINT NOT NULL,
    control_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    evidence VARCHAR(4000),
    notes VARCHAR(2000),
    assessed_at TIMESTAMP,
    UNIQUE (audit_id, control_id),
    FOREIGN KEY (audit_id) REFERENCES audits(id),
    FOREIGN KEY (control_id) REFERENCES controls(id)
);

CREATE TABLE audit_control_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_control_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text VARCHAR(4000),
    answered_at TIMESTAMP NOT NULL,
    UNIQUE (audit_control_id, question_id),
    FOREIGN KEY (audit_control_id) REFERENCES audit_controls(id),
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE INDEX idx_controls_framework ON controls(framework);
CREATE INDEX idx_controls_enabled ON controls(enabled);
CREATE INDEX idx_questions_control_id ON questions(control_id);
CREATE INDEX idx_audits_application_id ON audits(application_id);
CREATE INDEX idx_audits_assigned_to ON audits(assigned_to_user_id);
CREATE INDEX idx_audit_controls_audit_id ON audit_controls(audit_id);
