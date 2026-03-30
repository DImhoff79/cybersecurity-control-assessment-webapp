-- Align legacy NIST 800-53 seed questions (V3) with current plain-language style.
-- Audits read live question rows via question_control_mappings; this updates remaining V3 strings
-- not covered by V19 so owner/auditor views match the intent of the maintained library.

UPDATE questions
SET question_text = 'Do you have a written policy or standard that explains who may access systems and data, and how access is approved?',
    help_text = 'Look for an access control or security policy, or a short team agreement that names approvers and reviewers.'
WHERE question_text = 'Do you have a written policy that describes who can access your systems and data?';

UPDATE questions
SET question_text = 'Is that policy or standard reviewed at least once a year and updated when roles or systems change?',
    help_text = NULL
WHERE question_text = 'Is this policy reviewed and updated at least annually?';

UPDATE questions
SET question_text = 'When people join, move, or leave, are their accounts created, changed, or disabled in a timely way?',
    help_text = 'Typically within one to two business days for routine changes.'
WHERE question_text = 'When someone joins or leaves the team, are their system accounts created or disabled promptly?';

UPDATE questions
SET question_text = 'Do you regularly review who can access this application and adjust access when roles change?',
    help_text = 'A simple periodic access review is enough if it is documented.'
WHERE question_text = 'Do you review who has access to this application at least once a year?';

UPDATE questions
SET question_text = 'Can people only access what they need for their job (least privilege)?',
    help_text = 'This means no unnecessary admin rights or broad data access.'
WHERE question_text = 'Can users only see and do what they need for their job (no extra access)?';

UPDATE questions
SET question_text = 'Does your organization have a policy or standard for security awareness and training?',
    help_text = NULL
WHERE question_text = 'Does your organization have a security awareness and training policy?';

UPDATE questions
SET question_text = 'Do people complete required security awareness training at least once a year?',
    help_text = NULL
WHERE question_text = 'Do you complete required security training at least once a year?';

UPDATE questions
SET question_text = 'Are important security-related events (sign-ins, privilege changes, errors) recorded in logs?',
    help_text = NULL
WHERE question_text = 'Are important events (logins, changes to data, errors) recorded in logs?';

UPDATE questions
SET question_text = 'Are audit or security logs kept long enough for investigations (for example, at least 90 days)?',
    help_text = NULL
WHERE question_text = 'Are these logs kept for at least 90 days?';

UPDATE questions
SET question_text = 'Do users sign in with multi-factor authentication where required (for example, admin or remote access)?',
    help_text = 'Examples: authenticator app, hardware token, or passkey in addition to a password.'
WHERE question_text = 'Do users sign in with more than just a password (e.g. a code from a phone app or token)?';

UPDATE questions
SET question_text = 'Is there a documented incident response plan that your team can follow?',
    help_text = NULL
WHERE question_text = 'Is there a documented process for what to do when a security incident occurs?';

UPDATE questions
SET question_text = 'Do people know how to report a suspected incident and who is responsible for the response?',
    help_text = NULL
WHERE question_text = 'Do staff know who to contact if they suspect a security incident?';

UPDATE questions
SET question_text = 'Are security patches and updates applied within your documented timeframe?',
    help_text = 'Include operating systems, infrastructure, and critical application dependencies.'
WHERE question_text = 'Are security updates (patches) applied to systems in a timely manner?';

UPDATE questions
SET question_text = 'Is anti-malware or equivalent protection installed and kept current on user devices and servers where applicable?',
    help_text = NULL
WHERE question_text = 'Is antivirus or anti-malware installed and kept up to date on workstations and servers?';
