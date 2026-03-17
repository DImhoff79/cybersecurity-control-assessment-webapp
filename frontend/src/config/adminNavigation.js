export const adminSections = [
  {
    key: 'audit-program',
    label: 'Audit Program',
    items: [
      { label: 'Operations Queue', to: '/admin/operations', permission: 'REPORT_VIEW' },
      { label: 'Audit Projects', to: '/admin/audit-projects', permission: 'REPORT_VIEW' },
      { label: 'Audits', to: '/admin/audits', permission: 'AUDIT_MANAGEMENT' },
      { label: 'Questionnaire', to: '/admin/questionnaire', permission: 'AUDIT_MANAGEMENT' }
    ]
  },
  {
    key: 'governance',
    label: 'Governance & Compliance',
    items: [
      { label: 'Policies', to: '/admin/policies', permission: 'POLICY_MANAGEMENT' },
      { label: 'Obligations', to: '/admin/compliance-obligations', permission: 'COMPLIANCE_MANAGEMENT' },
      { label: 'Policy Attestations', to: '/admin/policy-attestations', permission: 'REPORT_VIEW' }
    ]
  },
  {
    key: 'risk',
    label: 'Risk & Remediation',
    items: [
      { label: 'Findings', to: '/admin/findings', permission: 'AUDIT_MANAGEMENT' },
      { label: 'Control Exceptions', to: '/admin/control-exceptions', permission: 'AUDIT_MANAGEMENT' },
      { label: 'Risk Register', to: '/admin/risk-register', permission: 'RISK_MANAGEMENT' },
      { label: 'Remediation Plans', to: '/admin/remediation-plans', permission: 'REMEDIATION_MANAGEMENT' }
    ]
  },
  {
    key: 'reporting',
    label: 'Reporting',
    items: [
      { label: 'Reports', to: '/admin/reports', permission: 'REPORT_VIEW' }
    ]
  },
  {
    key: 'admin-ops',
    label: 'Admin Ops',
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
