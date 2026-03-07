-- NIST 800-53 Low baseline (representative subset)
INSERT INTO controls (control_id, name, description, framework, enabled, category) VALUES
('AC-1', 'Access Control Policy and Procedures', 'Develop, document, and disseminate access control policy and procedures.', 'NIST_800_53_LOW', TRUE, 'Access Control'),
('AC-2', 'Account Management', 'Manage system accounts and access authorizations.', 'NIST_800_53_LOW', TRUE, 'Access Control'),
('AC-3', 'Access Enforcement', 'Enforce approved authorizations for logical access to information and system resources.', 'NIST_800_53_LOW', TRUE, 'Access Control'),
('AC-7', 'Unsuccessful Logon Attempts', 'Enforce a limit on consecutive invalid logon attempts.', 'NIST_800_53_LOW', TRUE, 'Access Control'),
('AC-11', 'Session Lock', 'Prevent further access to the system by initiating a session lock after a defined period of inactivity.', 'NIST_800_53_LOW', TRUE, 'Access Control'),
('AC-17', 'Remote Access', 'Authorize, monitor, and control remote access methods.', 'NIST_800_53_LOW', TRUE, 'Access Control'),
('AT-1', 'Security Awareness and Training Policy', 'Develop, document, and disseminate security awareness and training policy.', 'NIST_800_53_LOW', TRUE, 'Awareness and Training'),
('AT-2', 'Security Awareness Training', 'Provide security awareness training to personnel.', 'NIST_800_53_LOW', TRUE, 'Awareness and Training'),
('AU-1', 'Audit and Accountability Policy', 'Develop, document, and disseminate audit and accountability policy.', 'NIST_800_53_LOW', TRUE, 'Audit and Accountability'),
('AU-2', 'Audit Events', 'Determine which events need to be audited.', 'NIST_800_53_LOW', TRUE, 'Audit and Accountability'),
('AU-3', 'Content of Audit Records', 'Ensure audit records contain information that establishes what, when, where, who, and how.', 'NIST_800_53_LOW', TRUE, 'Audit and Accountability'),
('IA-2', 'Identification and Authentication', 'Uniquely identify and authenticate users and devices.', 'NIST_800_53_LOW', TRUE, 'Identification and Authentication'),
('IA-5', 'Authenticator Management', 'Manage system authenticators such as passwords and tokens.', 'NIST_800_53_LOW', TRUE, 'Identification and Authentication'),
('IR-1', 'Incident Response Policy', 'Develop, document, and disseminate incident response policy and procedures.', 'NIST_800_53_LOW', TRUE, 'Incident Response'),
('IR-2', 'Incident Response Training', 'Provide incident response training to personnel.', 'NIST_800_53_LOW', TRUE, 'Incident Response'),
('SC-1', 'System and Communications Protection Policy', 'Develop, document, and disseminate system and communications protection policy.', 'NIST_800_53_LOW', TRUE, 'System and Communications Protection'),
('SC-7', 'Boundary Protection', 'Monitor and control communications at system boundaries.', 'NIST_800_53_LOW', TRUE, 'System and Communications Protection'),
('SC-8', 'Transmission Confidentiality and Integrity', 'Protect the confidentiality and integrity of transmitted information.', 'NIST_800_53_LOW', TRUE, 'System and Communications Protection'),
('SI-1', 'System and Information Integrity Policy', 'Develop, document, and disseminate system and information integrity policy.', 'NIST_800_53_LOW', TRUE, 'System and Information Integrity'),
('SI-2', 'Flaw Remediation', 'Identify, report, and correct system flaws in a timely manner.', 'NIST_800_53_LOW', TRUE, 'System and Information Integrity'),
('SI-3', 'Malicious Code Protection', 'Implement malicious code protection mechanisms.', 'NIST_800_53_LOW', TRUE, 'System and Information Integrity');

-- PCI DSS v4 (representative requirements)
INSERT INTO controls (control_id, name, description, framework, enabled, category) VALUES
('PCI-1', 'Install and maintain network security controls', 'Networks must be protected with firewalls and other controls.', 'PCI_DSS_V4', TRUE, 'Network Security'),
('PCI-2', 'Apply secure configurations', 'Apply secure configuration to all system components.', 'PCI_DSS_V4', TRUE, 'Secure Configuration'),
('PCI-3', 'Protect stored account data', 'Protect stored account data including cardholder data.', 'PCI_DSS_V4', TRUE, 'Data Protection'),
('PCI-4', 'Protect cardholder data with strong cryptography', 'Use strong cryptography for transmission and storage.', 'PCI_DSS_V4', TRUE, 'Cryptography'),
('PCI-5', 'Protect all systems and networks from malicious software', 'Deploy antivirus and anti-malware.', 'PCI_DSS_V4', TRUE, 'Malware'),
('PCI-6', 'Develop and maintain secure systems and software', 'Develop software and systems securely.', 'PCI_DSS_V4', TRUE, 'Secure Development'),
('PCI-7', 'Restrict access to system components and cardholder data', 'Restrict access by business need to know.', 'PCI_DSS_V4', TRUE, 'Access Control'),
('PCI-8', 'Identify users and authenticate access', 'Identify and authenticate access to system components.', 'PCI_DSS_V4', TRUE, 'Identification and Authentication'),
('PCI-9', 'Restrict physical access to cardholder data', 'Restrict physical access to facilities and media.', 'PCI_DSS_V4', TRUE, 'Physical Security'),
('PCI-10', 'Log and monitor all access', 'Log, monitor, and test access to network and cardholder data.', 'PCI_DSS_V4', TRUE, 'Logging and Monitoring'),
('PCI-11', 'Test security of systems and networks regularly', 'Test security systems and networks regularly.', 'PCI_DSS_V4', TRUE, 'Testing'),
('PCI-12', 'Support information security with organizational policies', 'Maintain information security policy and programs.', 'PCI_DSS_V4', TRUE, 'Security Policy');

-- HIPAA Security Rule (representative safeguards)
INSERT INTO controls (control_id, name, description, framework, enabled, category) VALUES
('HIPAA-164.308(a)(1)', 'Security Management Process', 'Implement policies and procedures to prevent, detect, contain, and correct security violations.', 'HIPAA', TRUE, 'Administrative Safeguards'),
('HIPAA-164.308(a)(3)', 'Workforce Security', 'Implement policies and procedures to ensure appropriate access to ePHI.', 'HIPAA', TRUE, 'Administrative Safeguards'),
('HIPAA-164.308(a)(4)', 'Information Access Management', 'Implement policies and procedures for authorizing access to ePHI.', 'HIPAA', TRUE, 'Administrative Safeguards'),
('HIPAA-164.308(a)(5)', 'Security Awareness and Training', 'Implement a security awareness and training program for workforce.', 'HIPAA', TRUE, 'Administrative Safeguards'),
('HIPAA-164.308(a)(6)', 'Security Incident Procedures', 'Identify, respond to, and document security incidents.', 'HIPAA', TRUE, 'Administrative Safeguards'),
('HIPAA-164.310(a)(1)', 'Facility Access Controls', 'Implement policies and procedures to limit physical access to ePHI.', 'HIPAA', TRUE, 'Physical Safeguards'),
('HIPAA-164.310(d)(1)', 'Device and Media Controls', 'Implement policies and procedures for disposal and re-use of hardware and media.', 'HIPAA', TRUE, 'Physical Safeguards'),
('HIPAA-164.312(a)(1)', 'Access Control', 'Implement technical policies and procedures for electronic access to ePHI.', 'HIPAA', TRUE, 'Technical Safeguards'),
('HIPAA-164.312(b)', 'Audit Controls', 'Implement hardware, software, and/or procedural mechanisms that record and examine activity.', 'HIPAA', TRUE, 'Technical Safeguards'),
('HIPAA-164.312(c)(1)', 'Integrity', 'Implement policies and procedures to protect ePHI from improper alteration or destruction.', 'HIPAA', TRUE, 'Technical Safeguards'),
('HIPAA-164.312(d)', 'Person or Entity Authentication', 'Implement procedures to verify that a person or entity seeking access to ePHI is the one claimed.', 'HIPAA', TRUE, 'Technical Safeguards'),
('HIPAA-164.312(e)(1)', 'Transmission Security', 'Implement technical security measures to guard against unauthorized access to ePHI in transit.', 'HIPAA', TRUE, 'Technical Safeguards');

-- SOX IT General Controls (representative)
INSERT INTO controls (control_id, name, description, framework, enabled, category) VALUES
('SOX-ACCESS', 'Access Management', 'User provisioning, deprovisioning, least privilege, and access reviews.', 'SOX', TRUE, 'IT General Controls'),
('SOX-CHANGE', 'Change Management', 'Change request, approval, documentation, and segregation of duties.', 'SOX', TRUE, 'IT General Controls'),
('SOX-BACKUP', 'Backup and Recovery', 'Scheduled backups, encryption, recovery testing, and offsite storage.', 'SOX', TRUE, 'IT General Controls'),
('SOX-OPS', 'System Operations', 'Job scheduling, error handling, log monitoring, and incident response.', 'SOX', TRUE, 'IT General Controls'),
('SOX-LOGICAL', 'Logical Security', 'Firewall, endpoint protection, vulnerability scanning, patch management.', 'SOX', TRUE, 'IT General Controls'),
('SOX-PHYSICAL', 'Physical Security', 'Data center access, visitor logs, environmental controls.', 'SOX', TRUE, 'IT General Controls'),
('SOX-GOV', 'IT Governance', 'Defined roles, IT risk assessments, formalized IT policies.', 'SOX', TRUE, 'IT General Controls'),
('SOX-DATA', 'Data Integrity and Availability', 'Data classification, encryption, redundancy, high availability.', 'SOX', TRUE, 'IT General Controls');
