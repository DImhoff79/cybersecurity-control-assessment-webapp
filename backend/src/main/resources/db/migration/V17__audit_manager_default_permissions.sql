INSERT INTO user_permissions (user_id, permission)
SELECT id, 'USER_MANAGEMENT' FROM users u
WHERE u.role = 'AUDIT_MANAGER'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'USER_MANAGEMENT');

INSERT INTO user_permissions (user_id, permission)
SELECT id, 'AUDIT_MANAGEMENT' FROM users u
WHERE u.role = 'AUDIT_MANAGER'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'AUDIT_MANAGEMENT');

INSERT INTO user_permissions (user_id, permission)
SELECT id, 'REPORT_VIEW' FROM users u
WHERE u.role = 'AUDIT_MANAGER'
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission = 'REPORT_VIEW');
