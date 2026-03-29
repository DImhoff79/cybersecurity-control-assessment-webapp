-- Consolidate owner-facing Kroger CCF attestations into one plain-English question per control category
-- (mirrors branching-demo simplicity: few readable prompts, many controls behind each answer).
-- Organization-level *-1 policy controls stay on their existing security/compliance questions (V45).

-- ---------------------------------------------------------------------------
-- 1) Bucket questions (ask_owner = TRUE). Each question_text is unique for stable lookup.
-- ---------------------------------------------------------------------------
INSERT INTO questions (control_id, question_text, display_order, help_text, ask_owner) VALUES
(NULL, 'Access Control — For this application, how do you handle user accounts, approvals, least-privilege access, reviews, and timely removal of access (including remote or partner access where relevant)?', 0, 'One answer applies to all Kroger CCF access-control requirements mapped to this application in this category.', TRUE),
(NULL, 'Assessment, Authorization & Monitoring — How do you track system changes, approvals, and ongoing monitoring so this application stays within expected risk tolerance?', 0, 'Covers assessment / authorization / monitoring expectations for this app.', TRUE),
(NULL, 'Audit & Accountability — How does this application record important activity, protect logs, review them, and retain them for investigations or audits?', 0, 'Covers logging, review, and retention expectations for this app.', TRUE),
(NULL, 'Awareness & Training — How do teams that support this application stay trained on security expectations that apply to their role?', 0, 'Covers security awareness and role-based training as they apply to this app.', TRUE),
(NULL, 'Configuration Management — How do you control and track changes to this application (including baselines, testing, and approvals)?', 0, 'Covers configuration and change-management expectations for this app.', TRUE),
(NULL, 'Contingency Planning — How would this application recover after an outage or disaster, and how often is that plan exercised or validated?', 0, 'Covers backup, recovery, and contingency expectations for this app.', TRUE),
(NULL, 'Identification & Authentication — How do users prove who they are to this application (including MFA and session protections where required)?', 0, 'Covers identity and authentication expectations for this app.', TRUE),
(NULL, 'Incident Response — If something goes wrong, how does the team detect, report, and respond for this application?', 0, 'Covers incident handling expectations for this app.', TRUE),
(NULL, 'Maintenance — How is maintenance performed safely for this application (including remote maintenance and tool controls)?', 0, 'Covers maintenance expectations for this app.', TRUE),
(NULL, 'Media Protection — How is sensitive data protected when stored or moved (including disposal or sanitization) for this application?', 0, 'Covers media protection expectations for this app.', TRUE),
(NULL, 'Personnel Security — How are personnel risk considerations handled for people who operate or access this application?', 0, 'Covers personnel-security expectations for this app.', TRUE),
(NULL, 'Physical & Environmental Protection — What physical or environmental protections apply where this application runs (e.g., data centers or offices)?', 0, 'Covers physical and environmental expectations for this app.', TRUE),
(NULL, 'Planning — How is security planning aligned for this application (including resources and responsibilities)?', 0, 'Covers planning expectations for this app.', TRUE),
(NULL, 'Risk Assessment — How does the team understand and treat risks for this application over time?', 0, 'Covers risk assessment expectations for this app.', TRUE),
(NULL, 'Supply Chain Risk Management — How do you manage vendor or supply-chain risk for this application?', 0, 'Covers supply-chain risk expectations for this app.', TRUE),
(NULL, 'System & Communications Protection — How is data protected in transit and at rest, and how are communications secured for this application?', 0, 'Covers SC-related expectations for this app.', TRUE),
(NULL, 'System & Information Integrity — How do you protect against malware and unauthorized changes, and validate integrity for this application?', 0, 'Covers SI integrity expectations for this app.', TRUE),
(NULL, 'System & Services Acquisition — How are security requirements handled when acquiring or enhancing capabilities for this application?', 0, 'Covers acquisition and development security expectations for this app.', TRUE);

-- ---------------------------------------------------------------------------
-- 2) Point template + audit snapshot rows at bucket questions for non-policy KCF controls.
-- ---------------------------------------------------------------------------
UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Access Control —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Access Control —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Access Control —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Access Control'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Assessment, Authorization & Monitoring'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Audit & Accountability'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Awareness & Training'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Configuration Management —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Configuration Management —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Configuration Management —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Configuration Management'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Contingency Planning'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Identification & Authentication'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Incident Response —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Incident Response —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Incident Response —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Incident Response'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Maintenance —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Maintenance —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Maintenance —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Maintenance'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Media Protection —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Media Protection —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Media Protection —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Media Protection'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Personnel Security —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Personnel Security —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Personnel Security —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Personnel Security'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Physical & Environmental Protection'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Planning —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Planning —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Planning —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Planning'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Risk Assessment'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Supply Chain Risk Management'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Communications Protection'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Information Integrity'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE questionnaire_template_items qti
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%'),
    ask_owner = TRUE
WHERE qti.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Services Acquisition'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

DELETE FROM questionnaire_template_items qti
WHERE qti.id > (
    SELECT MIN(x.id) FROM questionnaire_template_items x
    WHERE x.template_id = qti.template_id AND x.control_id = qti.control_id AND x.question_id = qti.question_id
);

-- Audit questionnaire items (same bucket mapping)
UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Access Control —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Access Control —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Access Control —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Access Control'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Assessment, Authorization & Monitoring'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Audit & Accountability'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Awareness & Training'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Configuration Management —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Configuration Management —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Configuration Management —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Configuration Management'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Contingency Planning'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Identification & Authentication'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Incident Response —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Incident Response —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Incident Response —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Incident Response'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Maintenance —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Maintenance —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Maintenance —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Maintenance'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Media Protection —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Media Protection —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Media Protection —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Media Protection'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Personnel Security —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Personnel Security —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Personnel Security —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Personnel Security'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Physical & Environmental Protection'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Planning —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Planning —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Planning —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Planning'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Risk Assessment'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Supply Chain Risk Management'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Communications Protection'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Information Integrity'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

UPDATE audit_questionnaire_items aqi
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%'),
    question_text = (SELECT q.question_text FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%'),
    help_text = (SELECT q.help_text FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%'),
    ask_owner = TRUE
WHERE aqi.control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Services Acquisition'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

DELETE FROM audit_questionnaire_items aqi
WHERE aqi.id > (
    SELECT MIN(x.id) FROM audit_questionnaire_items x
    WHERE x.snapshot_id = aqi.snapshot_id AND x.audit_control_id = aqi.audit_control_id AND x.question_id = aqi.question_id
);

-- Answers follow consolidated question ids
UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Access Control —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Access Control'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Assessment, Authorization & Monitoring —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Assessment, Authorization & Monitoring'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Audit & Accountability —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Audit & Accountability'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Awareness & Training —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Awareness & Training'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Configuration Management —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Configuration Management'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Contingency Planning —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Contingency Planning'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Identification & Authentication —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Identification & Authentication'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Incident Response —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Incident Response'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Maintenance —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Maintenance'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Media Protection —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Media Protection'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Personnel Security —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Personnel Security'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Physical & Environmental Protection —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Physical & Environmental Protection'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Planning —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Planning'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Risk Assessment —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Risk Assessment'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'Supply Chain Risk Management —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'Supply Chain Risk Management'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Communications Protection —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Communications Protection'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Information Integrity —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Information Integrity'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

UPDATE audit_control_answers aca
SET question_id = (SELECT q.id FROM questions q WHERE q.question_text LIKE 'System & Services Acquisition —%')
WHERE aca.audit_control_id IN (
    SELECT id FROM audit_controls WHERE control_id IN (
        SELECT id FROM controls WHERE framework = 'KROGER_CCF' AND category = 'System & Services Acquisition'
          AND control_id NOT IN (
              'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
              'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
              'KCF_SI-1', 'KCF_SR-1'
          )
    )
);

DELETE FROM audit_control_answers aca
WHERE aca.id > (
    SELECT MIN(x.id) FROM audit_control_answers x
    WHERE x.audit_control_id = aca.audit_control_id AND x.question_id = aca.question_id
);

-- ---------------------------------------------------------------------------
-- 3) Replace live library mappings for non-policy KCF controls with one mapping per control to the bucket question.
-- ---------------------------------------------------------------------------
DELETE FROM question_control_mappings
WHERE control_id IN (
    SELECT id FROM controls WHERE framework = 'KROGER_CCF'
      AND control_id NOT IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);

INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, c.id
FROM controls c
JOIN questions q ON (
    (c.category = 'Access Control' AND q.question_text LIKE 'Access Control —%')
    OR (c.category = 'Assessment, Authorization & Monitoring' AND q.question_text LIKE 'Assessment, Authorization & Monitoring —%')
    OR (c.category = 'Audit & Accountability' AND q.question_text LIKE 'Audit & Accountability —%')
    OR (c.category = 'Awareness & Training' AND q.question_text LIKE 'Awareness & Training —%')
    OR (c.category = 'Configuration Management' AND q.question_text LIKE 'Configuration Management —%')
    OR (c.category = 'Contingency Planning' AND q.question_text LIKE 'Contingency Planning —%')
    OR (c.category = 'Identification & Authentication' AND q.question_text LIKE 'Identification & Authentication —%')
    OR (c.category = 'Incident Response' AND q.question_text LIKE 'Incident Response —%')
    OR (c.category = 'Maintenance' AND q.question_text LIKE 'Maintenance —%')
    OR (c.category = 'Media Protection' AND q.question_text LIKE 'Media Protection —%')
    OR (c.category = 'Personnel Security' AND q.question_text LIKE 'Personnel Security —%')
    OR (c.category = 'Physical & Environmental Protection' AND q.question_text LIKE 'Physical & Environmental Protection —%')
    OR (c.category = 'Planning' AND q.question_text LIKE 'Planning —%')
    OR (c.category = 'Risk Assessment' AND q.question_text LIKE 'Risk Assessment —%')
    OR (c.category = 'Supply Chain Risk Management' AND q.question_text LIKE 'Supply Chain Risk Management —%')
    OR (c.category = 'System & Communications Protection' AND q.question_text LIKE 'System & Communications Protection —%')
    OR (c.category = 'System & Information Integrity' AND q.question_text LIKE 'System & Information Integrity —%')
    OR (c.category = 'System & Services Acquisition' AND q.question_text LIKE 'System & Services Acquisition —%')
)
WHERE c.framework = 'KROGER_CCF'
  AND c.control_id NOT IN (
      'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
      'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
      'KCF_SI-1', 'KCF_SR-1'
  );

-- Point primary question.control_id at merged control for policy rows (unchanged); for buckets control_id stays NULL.

-- ---------------------------------------------------------------------------
-- 4) Remove obsolete attest question rows (no remaining references).
-- ---------------------------------------------------------------------------
DELETE FROM questions q
WHERE q.id NOT IN (SELECT question_id FROM question_control_mappings)
  AND q.id NOT IN (SELECT question_id FROM questionnaire_template_items)
  AND q.id NOT IN (SELECT question_id FROM audit_questionnaire_items)
  AND q.id NOT IN (SELECT question_id FROM audit_control_answers);
