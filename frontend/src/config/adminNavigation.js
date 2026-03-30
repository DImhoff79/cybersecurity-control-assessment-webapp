/** Sidebar config; keep in sync with router meta and docs/persona-route-matrix.md */
export const adminSections = [
  {
    key: 'workspace',
    label: 'My workspace',
    items: [
      { label: 'Home', to: '/start' },
      { label: 'My Audits', to: '/admin/my-audits' },
      { label: 'My Exceptions', to: '/admin/my-exceptions' },
      { label: 'Profile', to: '/admin/profile' }
    ]
  },
  {
    key: 'audit-program',
    label: 'Audit Program',
    items: [
      { label: 'Program home', to: '/admin/program-home', permission: 'REPORT_VIEW' },
      { label: 'Audit Queue', to: '/admin/operations', permission: 'REPORT_VIEW' },
      { label: 'Audit Projects', to: '/admin/audit-projects', permission: 'REPORT_VIEW' },
      { label: 'Audits', to: '/admin/audits', permission: 'AUDIT_MANAGEMENT' },
      { label: 'Questionnaire', to: '/admin/questionnaire', permission: 'AUDIT_MANAGEMENT' },
      {
        label: 'Mapping studio',
        to: '/admin/question-control-mapping-studio',
        permission: 'AUDIT_MANAGEMENT'
      },
      {
        label: 'Branching demo',
        to: '/admin/branching-workflow-demo',
        permission: 'AUDIT_MANAGEMENT'
      }
    ]
  },
  {
    key: 'governance',
    label: 'Governance & Compliance',
    items: [
      { label: 'Policies', to: '/admin/policies', permission: 'POLICY_MANAGEMENT' },
      { label: 'Compliance Obligations', to: '/admin/compliance-obligations', permission: 'COMPLIANCE_MANAGEMENT' },
    ]
  },
  {
    key: 'risk',
    label: 'Risk & Remediation',
    items: [
      { label: 'Issue Program Hub', to: '/admin/issue-program', permission: 'REPORT_VIEW' },
      { label: 'Findings', to: '/admin/findings', permission: 'REPORT_VIEW' },
      { label: 'Control Exceptions', to: '/admin/control-exceptions', permission: 'REPORT_VIEW' },
      {
        label: 'My control exceptions',
        to: '/admin/workspace-exceptions',
        roles: ['AUDITOR']
      },
      { label: 'Risk Register', to: '/admin/risk-register', permission: 'REPORT_VIEW' },
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
      },
      {
        label: 'Approval delegates',
        to: '/admin/approval-delegates',
        permission: 'USER_MANAGEMENT',
        roles: ['ADMIN', 'AUDIT_MANAGER']
      }
    ]
  }
]
