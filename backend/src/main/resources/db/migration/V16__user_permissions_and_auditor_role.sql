CREATE TABLE IF NOT EXISTS user_permissions (
    user_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO user_permissions (user_id, permission)
SELECT id, 'USER_MANAGEMENT' FROM users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'USER_MANAGEMENT');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'APPLICATION_MANAGEMENT' FROM users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'APPLICATION_MANAGEMENT');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'AUDIT_MANAGEMENT' FROM users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'AUDIT_MANAGEMENT');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'AUDIT_EXECUTION' FROM users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'AUDIT_EXECUTION');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'REPORT_VIEW' FROM users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'REPORT_VIEW');

INSERT INTO user_permissions (user_id, permission)
SELECT id, 'APPLICATION_MANAGEMENT' FROM users u
WHERE u.role = 'APPLICATION_OWNER'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'APPLICATION_MANAGEMENT');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'AUDIT_EXECUTION' FROM users u
WHERE u.role = 'APPLICATION_OWNER'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'AUDIT_EXECUTION');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'REPORT_VIEW' FROM users u
WHERE u.role = 'APPLICATION_OWNER'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'REPORT_VIEW');

INSERT INTO user_permissions (user_id, permission)
SELECT id, 'AUDIT_EXECUTION' FROM users u
WHERE u.role = 'AUDITOR'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'AUDIT_EXECUTION');
INSERT INTO user_permissions (user_id, permission)
SELECT id, 'REPORT_VIEW' FROM users u
WHERE u.role = 'AUDITOR'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'REPORT_VIEW');
