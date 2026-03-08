ALTER TABLE question_control_mappings ADD COLUMN IF NOT EXISTS mapping_rationale VARCHAR(2000);
ALTER TABLE question_control_mappings ADD COLUMN IF NOT EXISTS mapping_weight DECIMAL(5,2);
ALTER TABLE question_control_mappings ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE question_control_mappings ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;

ALTER TABLE audit_evidences ADD COLUMN IF NOT EXISTS file_name VARCHAR(500);
ALTER TABLE audit_evidences ADD COLUMN IF NOT EXISTS file_path VARCHAR(2000);
ALTER TABLE audit_evidences ADD COLUMN IF NOT EXISTS mime_type VARCHAR(255);
ALTER TABLE audit_evidences ADD COLUMN IF NOT EXISTS size_bytes BIGINT;

CREATE TABLE IF NOT EXISTS questionnaire_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_no INT NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    created_by_user_id BIGINT,
    published_by_user_id BIGINT,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    FOREIGN KEY (published_by_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS questionnaire_template_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    control_id BIGINT NOT NULL,
    question_text VARCHAR(2000) NOT NULL,
    help_text VARCHAR(2000),
    display_order INT NOT NULL DEFAULT 0,
    ask_owner BOOLEAN NOT NULL DEFAULT TRUE,
    mapping_rationale VARCHAR(2000),
    mapping_weight DECIMAL(5,2),
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES questionnaire_templates(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (control_id) REFERENCES controls(id),
    UNIQUE (template_id, question_id, control_id)
);

CREATE INDEX IF NOT EXISTS idx_questionnaire_template_items_template_id ON questionnaire_template_items(template_id);
