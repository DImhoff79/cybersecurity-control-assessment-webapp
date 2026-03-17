## Persona-First Permission Matrix (Phase 1)

This matrix documents frontend route guard requirements and the backend API authorities they map to.

### Audit Program

| Page | Frontend Guard | Primary APIs | Backend Guard |
|---|---|---|---|
| `/admin/operations` | `REPORT_VIEW` | `/api/reports/auditor-dashboard`, `/api/reports/recent-activity.csv` | `PERM_REPORT_VIEW` |
| `/admin/audits/:auditId` | `REPORT_VIEW` | `GET /api/audits/{id}`, `GET /api/audits/{id}/controls` | Authenticated access with audit-level authorization checks |
| `/admin/audit-projects` | `AUDIT_MANAGEMENT` | `/api/audit-projects/**` | `PERM_AUDIT_MANAGEMENT` |
| `/admin/audits` | `AUDIT_MANAGEMENT` | `/api/applications/{id}/audits`, `/api/audits/{id}/assign|send|remind|attest` | `PERM_AUDIT_MANAGEMENT` |

### Governance and Compliance

| Page | Frontend Guard | Primary APIs | Backend Guard |
|---|---|---|---|
| `/admin/policies` | `POLICY_MANAGEMENT` | `/api/policies/**` create/update/publish/version | `PERM_POLICY_MANAGEMENT` |
| `/admin/policy-attestations` | `REPORT_VIEW` | `/api/policies/acknowledgements` | `PERM_POLICY_MANAGEMENT` or `PERM_REPORT_VIEW` |
| `/admin/compliance-obligations` | `COMPLIANCE_MANAGEMENT` | `/api/compliance-obligations/**` | `PERM_COMPLIANCE_MANAGEMENT` |

### Risk and Remediation

| Page | Frontend Guard | Primary APIs | Backend Guard |
|---|---|---|---|
| `/admin/risk-register` | `RISK_MANAGEMENT` | `/api/risks/**` | `PERM_RISK_MANAGEMENT` |
| `/admin/remediation-plans` | `REMEDIATION_MANAGEMENT` | `/api/remediation-plans/**`, `/api/risks` | `PERM_REMEDIATION_MANAGEMENT`; approvals require role `ADMIN` or `AUDIT_MANAGER` |

### Reporting and Admin Ops

| Page | Frontend Guard | Primary APIs | Backend Guard |
|---|---|---|---|
| `/admin/reports` | `REPORT_VIEW` | `/api/reports/**` | `PERM_REPORT_VIEW` |
| `/admin/applications` | `APPLICATION_MANAGEMENT` | `/api/applications/**` | `PERM_APPLICATION_MANAGEMENT` |
| `/admin/users` | `USER_MANAGEMENT` | `/api/users/**` | `PERM_USER_MANAGEMENT` |
