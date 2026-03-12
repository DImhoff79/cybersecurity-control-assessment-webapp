-- Improve question wording for non-security specialists.
-- Keep semantic intent while reducing framework jargon.

UPDATE questions
SET question_text = 'Do you regularly review who can access this application and remove access that is no longer needed?',
    help_text = 'Include joiners, leavers, and role changes. A simple periodic access review is enough if it is documented.'
WHERE question_text = 'At least once a year, do you review who has access to this application and remove access people no longer need?';

UPDATE questions
SET question_text = 'Do users sign in with multi-factor authentication, especially for admin or remote access?',
    help_text = 'Examples: password plus authenticator app, hardware token, or passkey.'
WHERE question_text = 'Do users need multi-factor authentication when accessing this application, especially for admin or remote access?';

UPDATE questions
SET question_text = 'Do you record key security events and review logs or alerts on a regular basis?',
    help_text = 'Examples include sign-ins, failed access attempts, permission changes, and major system changes.'
WHERE question_text = 'Do you log important security events and review logs or alerts regularly for suspicious activity?';

UPDATE questions
SET question_text = 'Do you apply important security updates on time and track remediation until complete?',
    help_text = 'Include operating systems, infrastructure, and application dependencies.'
WHERE question_text = 'Do you apply critical security updates within a defined timeframe and track remediation to completion?';

UPDATE questions
SET question_text = 'If a security incident happens, do people know what to do and who to contact immediately?',
    help_text = 'Look for a documented response process and evidence the team is aware of it.'
WHERE question_text = 'If a security incident happens, do people know the response process and who to contact right away?';
