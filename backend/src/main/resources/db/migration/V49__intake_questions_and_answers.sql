-- Library-backed application intake: stable keys align with branching demo / canonical flow.

ALTER TABLE questions ADD COLUMN IF NOT EXISTS intake_step_key VARCHAR(64);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS intake_choices_json VARCHAR(4000);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS intake_input_type VARCHAR(24);

CREATE UNIQUE INDEX IF NOT EXISTS uq_questions_intake_step_key ON questions(intake_step_key);

CREATE TABLE application_intake_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_intake_answer UNIQUE (application_id, question_id),
    CONSTRAINT fk_aia_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_aia_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Intake-only questions (no control): same semantics as NewApplicationIntake.vue
INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Application name', 0, 'What do you call this application?', FALSE, 'app_name', 'TEXT', NULL);

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Short description', 1, 'What it does for the business', FALSE, 'app_description', 'TEXT', NULL);

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Primary purpose', 2, 'How would you describe the application''s primary role?', FALSE, 'purpose', 'CHOICE',
'[{"id":"INTERNAL_OPERATIONAL","label":"Internal / back-office"},{"id":"CUSTOMER_FACING","label":"Customer or shopper-facing"},{"id":"PARTNER_FACING","label":"Partner / B2B"},{"id":"MIXED","label":"Mixed"}]');

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Where it runs', 3, 'Where is the application hosted or operated?', FALSE, 'hosting', 'CHOICE',
'[{"id":"KROGER_MANAGED","label":"Enterprise-managed (data center / cloud)"},{"id":"VENDOR_SAAS","label":"Vendor SaaS / vendor-hosted"},{"id":"HYBRID","label":"Hybrid"}]');

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Who uses it', 4, 'Who is the primary user audience?', FALSE, 'audience', 'CHOICE',
'[{"id":"WORKFORCE_ONLY","label":"Workforce only"},{"id":"WORKFORCE_AND_CONTRACTORS","label":"Workforce and contractors"},{"id":"CUSTOMER_OR_PUBLIC","label":"Customers or the public"},{"id":"BUSINESS_PARTNERS","label":"External business partners"},{"id":"MIXED","label":"Mixed audiences"}]');

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Integrations & data flows', 5, 'How does data move in and out?', FALSE, 'integration', 'CHOICE',
'[{"id":"STANDALONE","label":"Mostly standalone"},{"id":"KROGER_INTEGRATED","label":"Connects to other enterprise systems"},{"id":"EXTERNAL_EXCHANGE","label":"Sends/receives data with external orgs"},{"id":"BOTH","label":"Enterprise and external"}]');

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'Data classification', 6, 'What is the highest classification of data this application handles?', FALSE, 'classification', 'CHOICE',
'[{"id":"PUBLIC","label":"PUBLIC"},{"id":"INTERNAL","label":"INTERNAL"},{"id":"CONFIDENTIAL","label":"CONFIDENTIAL"},{"id":"RESTRICTED","label":"RESTRICTED"}]');

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'PII in scope?', 7, 'Does this application store, process, or transmit personal information about individuals (PII)?', FALSE, 'scope_pii', 'YES_NO', NULL);

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'PCI in scope?', 8, 'Does this application store, process, or transmit payment card data (PCI DSS scope)?', FALSE, 'scope_pci', 'YES_NO', NULL);

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'SOX in scope?', 9, 'Does this application support financial reporting, disclosure controls, or SOX IT general controls?', FALSE, 'scope_sox', 'YES_NO', NULL);

INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner, intake_step_key, intake_input_type, intake_choices_json)
VALUES (NULL, 'HIPAA in scope?', 10, 'Does this application create, receive, maintain, or transmit HIPAA-regulated health information (PHI)?', FALSE, 'scope_hipaa', 'YES_NO', NULL);
