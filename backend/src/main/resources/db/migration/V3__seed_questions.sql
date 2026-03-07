-- Plain-English questions mapping to controls (sample set)
-- Control id 1 = AC-1
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(1, 'Do you have a written policy that describes who can access your systems and data?', 0, 'Look for an access control or security policy document.'),
(1, 'Is this policy reviewed and updated at least annually?', 1, NULL);
-- Control id 2 = AC-2
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(2, 'When someone joins or leaves the team, are their system accounts created or disabled promptly?', 0, 'Typically within 24-48 hours.'),
(2, 'Do you review who has access to this application at least once a year?', 1, NULL);
-- Control id 3 = AC-3
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(3, 'Can users only see and do what they need for their job (no extra access)?', 0, 'This is sometimes called least privilege.');
-- Control id 7 = AT-1
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(7, 'Does your organization have a security awareness and training policy?', 0, NULL),
(7, 'Do you complete required security training at least once a year?', 1, NULL);
-- Control id 10 = AU-2
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(10, 'Are important events (logins, changes to data, errors) recorded in logs?', 0, NULL),
(10, 'Are these logs kept for at least 90 days?', 1, NULL);
-- Control id 12 = IA-2
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(12, 'Do users sign in with more than just a password (e.g. a code from a phone app or token)?', 0, 'Multi-factor or two-factor authentication.');
-- Control id 15 = IR-1
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(15, 'Is there a documented process for what to do when a security incident occurs?', 0, NULL),
(15, 'Do staff know who to contact if they suspect a security incident?', 1, NULL);
-- Control id 19 = SI-2
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(19, 'Are security updates (patches) applied to systems in a timely manner?', 0, 'Typically within 30 days for critical patches.');
-- Control id 20 = SI-3
INSERT INTO questions (control_id, question_text, display_order, help_text) VALUES
(20, 'Is antivirus or anti-malware installed and kept up to date on workstations and servers?', 0, NULL);
