-- Visual designer demo: same triage path as application owner intake (before Kroger CCF attestation)

DELETE FROM demo_branching_edge WHERE version_id = 1;
DELETE FROM demo_branching_node WHERE version_id = 1;

UPDATE demo_branching_workflow_version SET start_node_id = NULL WHERE id = 1;

UPDATE demo_branching_workflow SET
    name = 'New application triage (demo)',
    description = 'Mirrors the application owner intake: name, how the app fits (purpose, hosting, audience, integrations), data classification, then whether PII/PHI/PCI are in scope—matching the live flow into the Kroger CCF assessment.'
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
(7, 1, 'sensitive_scope', 'Sensitive data in scope?',
 'Will PII, PHI, or PCI be in scope for this application?',
 'YES_NO', NULL, 1120, 140),
(8, 1, 'sensitive_note', 'Sensitive data detail',
 'Briefly note which types apply (PII, PHI, PCI) or any nuance for assessors.',
 'TEXT', NULL, 1300, 80),
(9, 1, 'end', 'Intake path complete',
 'Next step in production: answer Kroger Common Controls Framework (CCF) questions for this application''s assessment.',
 'END', NULL, 1300, 220);

UPDATE demo_branching_workflow_version SET start_node_id = 1 WHERE id = 1;

-- Edges: linear triage, then Yes/No branch before END
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
(1, 7, 9, 1, 'NO', NULL),
(1, 8, 9, 0, 'ALWAYS', NULL);
