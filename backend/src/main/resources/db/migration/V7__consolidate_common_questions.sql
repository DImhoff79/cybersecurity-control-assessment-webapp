-- Consolidate overlapping controls to reduce repetitive owner questions.

-- Group 1: Access review / least privilege governance
INSERT INTO questions (control_id, question_text, display_order, help_text)
SELECT
    (SELECT id FROM controls WHERE control_id = 'AC-2'),
    'At least once a year, do you review who has access to this application and remove access people no longer need?',
    0,
    'Include joiner/mover/leaver updates and role-based access reviews.'
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE question_text = 'At least once a year, do you review who has access to this application and remove access people no longer need?'
);

INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, c.id
FROM questions q
JOIN controls c ON c.control_id IN ('AC-2', 'PCI-7', 'HIPAA-164.308(a)(4)', 'SOX-ACCESS')
WHERE q.question_text = 'At least once a year, do you review who has access to this application and remove access people no longer need?'
AND NOT EXISTS (
    SELECT 1 FROM question_control_mappings m
    WHERE m.question_id = q.id AND m.control_id = c.id
);

DELETE FROM question_control_mappings
WHERE control_id IN (SELECT id FROM controls WHERE control_id IN ('AC-2', 'PCI-7', 'HIPAA-164.308(a)(4)', 'SOX-ACCESS'))
AND question_id <> (
    SELECT id FROM questions
    WHERE question_text = 'At least once a year, do you review who has access to this application and remove access people no longer need?'
);

-- Group 2: MFA / strong authentication
INSERT INTO questions (control_id, question_text, display_order, help_text)
SELECT
    (SELECT id FROM controls WHERE control_id = 'IA-2'),
    'Do users need multi-factor authentication when accessing this application, especially for admin or remote access?',
    0,
    'For example: password plus authenticator app, hardware token, or passkey.'
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE question_text = 'Do users need multi-factor authentication when accessing this application, especially for admin or remote access?'
);

INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, c.id
FROM questions q
JOIN controls c ON c.control_id IN ('IA-2', 'AC-17', 'PCI-8', 'HIPAA-164.312(d)')
WHERE q.question_text = 'Do users need multi-factor authentication when accessing this application, especially for admin or remote access?'
AND NOT EXISTS (
    SELECT 1 FROM question_control_mappings m
    WHERE m.question_id = q.id AND m.control_id = c.id
);

DELETE FROM question_control_mappings
WHERE control_id IN (SELECT id FROM controls WHERE control_id IN ('IA-2', 'AC-17', 'PCI-8', 'HIPAA-164.312(d)'))
AND question_id <> (
    SELECT id FROM questions
    WHERE question_text = 'Do users need multi-factor authentication when accessing this application, especially for admin or remote access?'
);

-- Group 3: Logging and monitoring
INSERT INTO questions (control_id, question_text, display_order, help_text)
SELECT
    (SELECT id FROM controls WHERE control_id = 'AU-2'),
    'Do you log important security events and review logs or alerts regularly for suspicious activity?',
    0,
    'Examples: sign-in events, privilege changes, failed access attempts, and critical system changes.'
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE question_text = 'Do you log important security events and review logs or alerts regularly for suspicious activity?'
);

INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, c.id
FROM questions q
JOIN controls c ON c.control_id IN ('AU-2', 'AU-3', 'PCI-10', 'HIPAA-164.312(b)', 'SOX-OPS')
WHERE q.question_text = 'Do you log important security events and review logs or alerts regularly for suspicious activity?'
AND NOT EXISTS (
    SELECT 1 FROM question_control_mappings m
    WHERE m.question_id = q.id AND m.control_id = c.id
);

DELETE FROM question_control_mappings
WHERE control_id IN (SELECT id FROM controls WHERE control_id IN ('AU-2', 'AU-3', 'PCI-10', 'HIPAA-164.312(b)', 'SOX-OPS'))
AND question_id <> (
    SELECT id FROM questions
    WHERE question_text = 'Do you log important security events and review logs or alerts regularly for suspicious activity?'
);

-- Group 4: Patch and vulnerability management
INSERT INTO questions (control_id, question_text, display_order, help_text)
SELECT
    (SELECT id FROM controls WHERE control_id = 'SI-2'),
    'Do you apply critical security updates within a defined timeframe and track remediation to completion?',
    0,
    'Include operating systems, application dependencies, and infrastructure components.'
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE question_text = 'Do you apply critical security updates within a defined timeframe and track remediation to completion?'
);

INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, c.id
FROM questions q
JOIN controls c ON c.control_id IN ('SI-2', 'PCI-6', 'SOX-LOGICAL')
WHERE q.question_text = 'Do you apply critical security updates within a defined timeframe and track remediation to completion?'
AND NOT EXISTS (
    SELECT 1 FROM question_control_mappings m
    WHERE m.question_id = q.id AND m.control_id = c.id
);

DELETE FROM question_control_mappings
WHERE control_id IN (SELECT id FROM controls WHERE control_id IN ('SI-2', 'PCI-6', 'SOX-LOGICAL'))
AND question_id <> (
    SELECT id FROM questions
    WHERE question_text = 'Do you apply critical security updates within a defined timeframe and track remediation to completion?'
);

-- Group 5: Incident response readiness
INSERT INTO questions (control_id, question_text, display_order, help_text)
SELECT
    (SELECT id FROM controls WHERE control_id = 'IR-1'),
    'If a security incident happens, do people know the response process and who to contact right away?',
    0,
    'Look for a documented response plan and evidence that the team is aware of it.'
WHERE NOT EXISTS (
    SELECT 1 FROM questions
    WHERE question_text = 'If a security incident happens, do people know the response process and who to contact right away?'
);

INSERT INTO question_control_mappings (question_id, control_id)
SELECT q.id, c.id
FROM questions q
JOIN controls c ON c.control_id IN ('IR-1', 'IR-2', 'HIPAA-164.308(a)(6)')
WHERE q.question_text = 'If a security incident happens, do people know the response process and who to contact right away?'
AND NOT EXISTS (
    SELECT 1 FROM question_control_mappings m
    WHERE m.question_id = q.id AND m.control_id = c.id
);

DELETE FROM question_control_mappings
WHERE control_id IN (SELECT id FROM controls WHERE control_id IN ('IR-1', 'IR-2', 'HIPAA-164.308(a)(6)'))
AND question_id <> (
    SELECT id FROM questions
    WHERE question_text = 'If a security incident happens, do people know the response process and who to contact right away?'
);
