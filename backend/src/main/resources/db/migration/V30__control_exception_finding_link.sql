-- Optional link from a control exception to a finding (same audit; control aligned when finding is control-scoped).
ALTER TABLE control_exceptions
    ADD COLUMN finding_id BIGINT NULL;

ALTER TABLE control_exceptions
    ADD CONSTRAINT fk_control_exceptions_finding
        FOREIGN KEY (finding_id) REFERENCES findings(id);

CREATE INDEX idx_control_exceptions_finding_id ON control_exceptions(finding_id);
