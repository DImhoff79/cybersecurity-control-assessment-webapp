-- Replace previously generic fallback wording with control-specific plain-language prompts.
UPDATE questions q
SET
    question_text = (
        SELECT
            'For your application, do you currently do this consistently: ' ||
            SUBSTRING(COALESCE(c.description, c.name), 1, 1400) || '?'
        FROM controls c
        WHERE c.id = q.control_id
    ),
    help_text = 'Think about how your team actually works day to day. If this is consistently in place, answer Yes; if only some systems or teams follow it, answer Partially.'
WHERE q.question_text = 'In plain terms, is this control in place and working for your application?';
