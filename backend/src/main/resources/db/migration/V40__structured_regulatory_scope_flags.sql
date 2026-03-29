-- Reporting: PII, PCI, SOX, HIPAA as boolean columns (not free text). HIPAA supersedes legacy PHI flag in the API.

ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_scope_sox BOOLEAN;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_scope_hipaa BOOLEAN;

UPDATE applications SET data_scope_hipaa = TRUE WHERE data_scope_phi IS TRUE;

-- Demo branching: four Yes/No steps for PII, PCI, SOX, HIPAA (no free-text capture)

DELETE FROM demo_branching_edge WHERE version_id = 1;
DELETE FROM demo_branching_node WHERE version_id = 1;

UPDATE demo_branching_workflow_version SET start_node_id = NULL WHERE id = 1;

UPDATE demo_branching_workflow SET
    description = 'Mirrors structured intake: application metadata, then separate Yes/No steps for PII, PCI, SOX, and HIPAA scope before the Kroger CCF assessment.'
WHERE id = 1;

INSERT INTO demo_branching_node (id, version_id, stable_key, title, body, question_type, choices_json, pos_x, pos_y) VALUES
(1, 1, 'app_name', 'Application name',
 'What do you call this application? (The live intake also captures a short description.)',
 'TEXT', NULL, 40, 140),
(2, 1, 'purpose', 'Primary purpose',
 'How would you describe the application''s primary role?',
 'CHOICE',
 '[{"id":"INTERNAL_OPERATIONAL","label":"Internal / back-office"},{"id":"CUSTOMER_FACING","label":"Customer or shopper-facing"},{"id":"PARTNER_FACING","label":"Partner / B2B"},{"id":"MIXED","label":"Mixed"}]',
 220, 140),
(3, 1, 'hosting', 'Where it runs',
 'Where is the application hosted or operated?',
 'CHOICE',
 '[{"id":"KROGER_MANAGED","label":"Enterprise-managed (data center / cloud)"},{"id":"VENDOR_SAAS","label":"Vendor SaaS / vendor-hosted"},{"id":"HYBRID","label":"Hybrid"}]',
 400, 140),
(4, 1, 'audience', 'Who uses it',
 'Who is the primary user audience?',
 'CHOICE',
 '[{"id":"WORKFORCE_ONLY","label":"Workforce only"},{"id":"WORKFORCE_AND_CONTRACTORS","label":"Workforce and contractors"},{"id":"CUSTOMER_OR_PUBLIC","label":"Customers or the public"},{"id":"BUSINESS_PARTNERS","label":"External business partners"},{"id":"MIXED","label":"Mixed audiences"}]',
 580, 140),
(5, 1, 'integration', 'Integrations & data flows',
 'How does data move in and out?',
 'CHOICE',
 '[{"id":"STANDALONE","label":"Mostly standalone"},{"id":"KROGER_INTEGRATED","label":"Connects to other enterprise systems"},{"id":"EXTERNAL_EXCHANGE","label":"Sends/receives data with external orgs"},{"id":"BOTH","label":"Enterprise and external"}]',
 760, 140),
(6, 1, 'classification', 'Data classification',
 'What is the highest classification of data this application handles?',
 'CHOICE',
 '[{"id":"PUBLIC","label":"PUBLIC"},{"id":"INTERNAL","label":"INTERNAL"},{"id":"CONFIDENTIAL","label":"CONFIDENTIAL"},{"id":"RESTRICTED","label":"RESTRICTED"}]',
 940, 140),
(7, 1, 'scope_pii', 'PII in scope?',
 'Does this application store, process, or transmit personal information about individuals (PII)?',
 'YES_NO', NULL, 1120, 100),
(8, 1, 'scope_pci', 'PCI in scope?',
 'Does this application store, process, or transmit payment card data (PCI DSS scope)?',
 'YES_NO', NULL, 1120, 160),
(9, 1, 'scope_sox', 'SOX in scope?',
 'Does this application support financial reporting, disclosure controls, or SOX IT general controls?',
 'YES_NO', NULL, 1120, 220),
(10, 1, 'scope_hipaa', 'HIPAA in scope?',
 'Does this application create, receive, maintain, or transmit HIPAA-regulated health information (PHI)?',
 'YES_NO', NULL, 1120, 280),
(11, 1, 'end', 'Intake path complete',
 'Structured PII / PCI / SOX / HIPAA flags are stored for reporting. Next: complete the Kroger CCF control questionnaire.',
 'END', NULL, 1300, 190);

UPDATE demo_branching_workflow_version SET start_node_id = 1 WHERE id = 1;

INSERT INTO demo_branching_edge (version_id, from_node_id, to_node_id, sort_order, condition_kind, condition_value) VALUES
(1, 1, 2, 0, 'ALWAYS', NULL),
(1, 2, 3, 0, 'OPTION', 'INTERNAL_OPERATIONAL'),
(1, 2, 3, 1, 'OPTION', 'CUSTOMER_FACING'),
(1, 2, 3, 2, 'OPTION', 'PARTNER_FACING'),
(1, 2, 3, 3, 'OPTION', 'MIXED'),
(1, 2, 3, 99, 'ALWAYS', NULL),
(1, 3, 4, 0, 'OPTION', 'KROGER_MANAGED'),
(1, 3, 4, 1, 'OPTION', 'VENDOR_SAAS'),
(1, 3, 4, 2, 'OPTION', 'HYBRID'),
(1, 3, 4, 99, 'ALWAYS', NULL),
(1, 4, 5, 0, 'OPTION', 'WORKFORCE_ONLY'),
(1, 4, 5, 1, 'OPTION', 'WORKFORCE_AND_CONTRACTORS'),
(1, 4, 5, 2, 'OPTION', 'CUSTOMER_OR_PUBLIC'),
(1, 4, 5, 3, 'OPTION', 'BUSINESS_PARTNERS'),
(1, 4, 5, 4, 'OPTION', 'MIXED'),
(1, 4, 5, 99, 'ALWAYS', NULL),
(1, 5, 6, 0, 'OPTION', 'STANDALONE'),
(1, 5, 6, 1, 'OPTION', 'KROGER_INTEGRATED'),
(1, 5, 6, 2, 'OPTION', 'EXTERNAL_EXCHANGE'),
(1, 5, 6, 3, 'OPTION', 'BOTH'),
(1, 5, 6, 99, 'ALWAYS', NULL),
(1, 6, 7, 0, 'OPTION', 'PUBLIC'),
(1, 6, 7, 1, 'OPTION', 'INTERNAL'),
(1, 6, 7, 2, 'OPTION', 'CONFIDENTIAL'),
(1, 6, 7, 3, 'OPTION', 'RESTRICTED'),
(1, 6, 7, 99, 'ALWAYS', NULL),
(1, 7, 8, 0, 'YES', NULL),
(1, 7, 8, 1, 'NO', NULL),
(1, 8, 9, 0, 'YES', NULL),
(1, 8, 9, 1, 'NO', NULL),
(1, 9, 10, 0, 'YES', NULL),
(1, 9, 10, 1, 'NO', NULL),
(1, 10, 11, 0, 'YES', NULL),
(1, 10, 11, 1, 'NO', NULL);
