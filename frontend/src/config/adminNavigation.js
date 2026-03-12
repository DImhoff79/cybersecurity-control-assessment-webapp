export const adminSections = [
  {
    key: 'plan',
    label: 'Plan',
    items: [
      { label: 'Audit Projects', to: '/admin/audit-projects', permission: 'AUDIT_MANAGEMENT' },
      { label: 'Questionnaire', to: '/admin/questionnaire', permission: 'AUDIT_MANAGEMENT' }
    ]
  },
  {
    key: 'execute',
    label: 'Execute',
    items: [
      { label: 'Operations Queue', to: '/admin/operations', permission: 'REPORT_VIEW' },
      { label: 'Audits', to: '/admin/audits', permission: 'AUDIT_MANAGEMENT' }
    ]
  },
  {
    key: 'resolve',
    label: 'Resolve',
    items: [
      { label: 'Findings', to: '/admin/findings', permission: 'AUDIT_MANAGEMENT' },
      { label: 'Control Exceptions', to: '/admin/control-exceptions', permission: 'AUDIT_MANAGEMENT' }
    ]
  },
  {
    key: 'monitor',
    label: 'Monitor',
    items: [
      { label: 'Reports', to: '/admin/reports', permission: 'REPORT_VIEW' }
    ]
  },
  {
    key: 'admin',
    label: 'Admin',
    items: [
      { label: 'Applications', to: '/admin/applications', permission: 'APPLICATION_MANAGEMENT' },
      { label: 'Users', to: '/admin/users', permission: 'USER_MANAGEMENT' },
      {
        label: 'Questionnaire Governance',
        to: '/admin/questionnaire-templates',
        permission: 'AUDIT_MANAGEMENT',
        roles: ['ADMIN', 'AUDIT_MANAGER']
      }
    ]
  }
]
