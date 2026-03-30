-- One-time purge of all audit-program rows so the next server start can re-seed from DemoDatasetSeeder
-- using the current control framework filter (default KROGER_CCF) and question_control_mappings.
-- Order respects FKs (H2 + PostgreSQL).

DELETE FROM notifications WHERE audit_id IS NOT NULL;
DELETE FROM report_schedules WHERE project_id_filter IS NOT NULL;

DELETE FROM risk_finding_links;
DELETE FROM risk_exception_links;
-- control_exceptions.finding_id references findings; delete before findings
DELETE FROM control_exceptions;
DELETE FROM findings;

DELETE FROM audit_control_answers;
DELETE FROM audit_evidences;
DELETE FROM audit_control_assignments;
DELETE FROM audit_questionnaire_snapshots;
DELETE FROM audit_approval_steps;
DELETE FROM audit_activity_logs;
DELETE FROM audit_assignments;
DELETE FROM audit_controls;

DELETE FROM audits;

DELETE FROM audit_project_applications;
DELETE FROM audit_projects;
