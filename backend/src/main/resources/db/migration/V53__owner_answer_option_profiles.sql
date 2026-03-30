-- Reusable owner-response option sets for guided audit questions (labels for YES/PARTIAL/NO/NOT_APPLICABLE + UNANSWERED).

CREATE TABLE owner_answer_option_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    field_label VARCHAR(500),
    field_hint VARCHAR(2000),
    options_json CLOB NOT NULL
);

ALTER TABLE questions ADD COLUMN owner_answer_option_profile_id BIGINT NULL;

ALTER TABLE questions ADD CONSTRAINT fk_questions_owner_answer_profile
    FOREIGN KEY (owner_answer_option_profile_id) REFERENCES owner_answer_option_profiles (id);

-- Seed profiles (options_json: array of {value,label}; must include UNANSWERED first for UX parity with legacy UI)
INSERT INTO owner_answer_option_profiles (code, display_name, field_label, field_hint, options_json) VALUES
('DEFAULT', 'Default (yes / partial / no)', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — fully or in practice"},{"value":"PARTIAL","label":"Partially — in progress or with known gaps"},{"value":"NO","label":"No — not in place yet"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('BUCKET', 'KCF category narrative', 'Overall, how does this read for your application?', 'Pick the option that best matches your overall posture for this category. Add specifics under Notes & supporting files if you need to explain trade-offs or scope.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Our practices align well with expectations for this control family"},{"value":"PARTIAL","label":"We partially align — gaps are documented and being addressed"},{"value":"NO","label":"We have significant gaps relative to expectations"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('POLICY_OR_STANDARD', 'Written policy / standard', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — we have a documented policy or standard that covers this"},{"value":"PARTIAL","label":"Partially — draft, informal, or being formalized"},{"value":"NO","label":"No — not yet documented"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('REVIEW_CADENCE', 'Annual / scheduled review', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — reviewed on schedule (for example at least annually)"},{"value":"PARTIAL","label":"Sometimes — schedule is informal or inconsistent"},{"value":"NO","label":"No — not reviewed on a regular cadence"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('JOIN_MOVE_LEAVE', 'Join / move / leave timeliness', 'How timely is this today?', 'Think about joiners, movers, and leavers — including contractors or partners if they get access to this app.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — accounts are created, changed, or disabled in a timely way"},{"value":"PARTIAL","label":"Mostly — occasional delays or manual exceptions"},{"value":"NO","label":"No — not consistently timely"},{"value":"NOT_APPLICABLE","label":"Not applicable (for example, no HR-driven access changes)"}]'),
('ACCESS_REVIEW', 'Access reviews', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — we review access regularly and adjust when roles change"},{"value":"PARTIAL","label":"Partially — reviews happen but not on a steady rhythm"},{"value":"NO","label":"No — we rarely review who has access"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('LEAST_PRIVILEGE', 'Least privilege', 'Does this describe your environment?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — people only get access needed for their job"},{"value":"PARTIAL","label":"Partially — some exceptions or legacy access remain"},{"value":"NO","label":"No — broad or standing access is common"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('AWARENESS_POLICY', 'Awareness / training policy', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — we have a defined awareness / training policy or standard"},{"value":"PARTIAL","label":"Partially — expectations exist but are not fully documented"},{"value":"NO","label":"No — not defined"},{"value":"NOT_APPLICABLE","label":"Not applicable"}]'),
('TRAINING_CADENCE', 'Training completion cadence', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — required training is completed at least annually (or as required)"},{"value":"PARTIAL","label":"Partially — some roles are current; others are catching up"},{"value":"NO","label":"No — training is not completed on schedule"},{"value":"NOT_APPLICABLE","label":"Not applicable to this team or application"}]'),
('LOGGING_EVENTS', 'Security logging events', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — important security-related events are logged"},{"value":"PARTIAL","label":"Partially — some events are logged; gaps remain"},{"value":"NO","label":"No — logging is missing or very limited"},{"value":"NOT_APPLICABLE","label":"Not applicable to this application"}]'),
('LOG_RETENTION', 'Log retention', 'How does retention compare to policy?', 'If retention differs by system, choose the best fit and describe it in the notes below.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — retention meets or exceeds our policy (for example 90+ days where required)"},{"value":"PARTIAL","label":"Partially — retention varies by system or is being improved"},{"value":"NO","label":"No — retention is shorter than policy or not defined"},{"value":"NOT_APPLICABLE","label":"Not applicable"}]'),
('MFA', 'Multi-factor authentication', 'How is MFA enforced?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — MFA is enforced where required (for example admin or remote access)"},{"value":"PARTIAL","label":"Partially — MFA is rolling out or has exceptions"},{"value":"NO","label":"No — MFA is not consistently required"},{"value":"NOT_APPLICABLE","label":"Not applicable (for example, no interactive user sign-in)"}]'),
('INCIDENT_PLAN', 'Incident response plan', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — we have a documented incident response plan we can follow"},{"value":"PARTIAL","label":"Partially — informal runbooks or work in progress"},{"value":"NO","label":"No — not documented"},{"value":"NOT_APPLICABLE","label":"Not applicable"}]'),
('INCIDENT_CONTACT', 'Incident reporting / contacts', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — people know how to report and who owns the response"},{"value":"PARTIAL","label":"Partially — some teams know; communication gaps remain"},{"value":"NO","label":"No — unclear who to contact or what to do"},{"value":"NOT_APPLICABLE","label":"Not applicable"}]'),
('PATCHES', 'Patching SLAs', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — patches are applied within our documented timeframe"},{"value":"PARTIAL","label":"Partially — critical items prioritized; some backlog"},{"value":"NO","label":"No — patches often run late or without a clear SLA"},{"value":"NOT_APPLICABLE","label":"Not applicable (for example, fully managed by a vendor with no local patching)"}]'),
('MALWARE', 'Anti-malware', 'Which option best describes your situation?', 'If none of these fit perfectly, choose the closest option and add context under Notes & supporting files.',
 '[{"value":"UNANSWERED","label":"Choose an option…"},{"value":"YES","label":"Yes — protection is deployed and kept current where applicable"},{"value":"PARTIAL","label":"Partially — some gaps (for example certain device types)"},{"value":"NO","label":"No — not in place or not current"},{"value":"NOT_APPLICABLE","label":"Not applicable (for example, serverless or locked-down endpoints only)"}]');

UPDATE questions q
SET owner_answer_option_profile_id = (SELECT id FROM owner_answer_option_profiles p WHERE p.code = 'DEFAULT')
WHERE q.owner_answer_option_profile_id IS NULL;

-- Kroger CCF bucket prompts (category — how …)
UPDATE questions q
SET owner_answer_option_profile_id = (SELECT id FROM owner_answer_option_profiles p WHERE p.code = 'BUCKET')
WHERE q.question_text LIKE '% — %'
  AND (q.question_text LIKE '%how do you%' OR q.question_text LIKE '%how does%' OR q.question_text LIKE '%how would%' OR q.question_text LIKE '%how is%' OR q.question_text LIKE '%how are%');
