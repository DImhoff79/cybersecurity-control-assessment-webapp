-- Support canonical questions mapped to multiple controls.
CREATE TABLE question_control_mappings (
    question_id BIGINT NOT NULL,
    control_id BIGINT NOT NULL,
    PRIMARY KEY (question_id, control_id),
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (control_id) REFERENCES controls(id)
);

-- Backfill one mapping per existing question/control association.
INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, q.control_id
FROM questions q
WHERE NOT EXISTS (
    SELECT 1
    FROM question_control_mappings m
    WHERE m.question_id = q.id
      AND m.control_id = q.control_id
);

CREATE INDEX idx_qcm_control_id ON question_control_mappings(control_id);
CREATE INDEX idx_qcm_question_id ON question_control_mappings(question_id);
