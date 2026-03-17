ALTER TABLE remediation_plans ADD COLUMN proposed_plan VARCHAR(6000);
ALTER TABLE remediation_plans ADD COLUMN timeframe_text VARCHAR(1000);
ALTER TABLE remediation_plans ADD COLUMN compensating_controls VARCHAR(4000);
ALTER TABLE remediation_plans ADD COLUMN plan_rationale VARCHAR(6000);
ALTER TABLE remediation_plans ADD COLUMN approval_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE remediation_plans ADD COLUMN approval_notes VARCHAR(3000);
ALTER TABLE remediation_plans ADD COLUMN approved_by_user_id BIGINT;
ALTER TABLE remediation_plans ADD COLUMN approved_at TIMESTAMP;

ALTER TABLE remediation_plans
    ADD CONSTRAINT fk_remediation_plan_approved_by
        FOREIGN KEY (approved_by_user_id) REFERENCES users(id);
