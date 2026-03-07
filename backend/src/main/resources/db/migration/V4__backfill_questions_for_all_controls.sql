-- Ensure every control has at least one plain-language question.
-- This keeps self-service user flows fully question-driven.
INSERT INTO questions (control_id, question_text, display_order, help_text)
SELECT
    c.id,
    'In plain terms, is this control in place and working for your application?',
    0,
    'Control ' || c.control_id || ' - ' || c.name ||
    '. Answer Yes if implemented, Partially if some parts exist, No if not implemented, or Not applicable if this does not apply.'
FROM controls c
WHERE NOT EXISTS (
    SELECT 1
    FROM questions q
    WHERE q.control_id = c.id
);
