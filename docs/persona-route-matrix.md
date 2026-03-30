# Persona → route → permission

Keep this aligned with `frontend/src/router/index.js` `meta.permission` and `frontend/src/config/adminNavigation.js`. Backend `@PreAuthorize` must allow the same read paths.

| Persona / role pattern | Primary entry | Key routes | Router `meta.permission` (typical) |
|------------------------|---------------|------------|--------------------------------------|
| **Application owner** | `/start` | `/my-audits`, `/audits/:id/respond`, `/selfservice/*` | `SELF_SERVICE` or none (workspace shell) |
| **Auditor** | `/admin/program-home` or RoleHub | `/admin/audit-projects`, `/admin/operations`, `/admin/reports`, `/admin/findings`, `/admin/issue-program`, `/admin/control-exceptions`, `/admin/risk-register` | `REPORT_VIEW` for program read surfaces; audit workspace `/admin/audits/:id` uses `AUDIT_EXECUTION` or `AUDIT_MANAGEMENT` |
| **Audit manager** | `/admin/program-home` | Same as auditor plus `/admin/audits`, `/admin/questionnaire`, write on findings/exceptions | `AUDIT_MANAGEMENT` + `REPORT_VIEW` as applicable |
| **Risk owner** | RoleHub / nav | `/admin/risk-register` (write) | `RISK_MANAGEMENT` |
| **Administrator** | RoleHub / `/admin/users` | Users, applications, questionnaire | `USER_MANAGEMENT`, `APPLICATION_MANAGEMENT`, etc. |

## Read vs write (shared pages)

| Page | Read (`REPORT_VIEW` or scoped) | Write |
|------|-------------------------------|--------|
| Findings (`/admin/findings`) | List scoped to accessible audits | `AUDIT_MANAGEMENT` |
| Issue program hub | Timeline/cards | Create/approve flows per feature flags |
| Control exceptions | List scoped | Approve/reject: `AUDIT_MANAGEMENT` |
| Risk register | List/detail | Create/update status: `RISK_MANAGEMENT` |

When adding a new admin route: set `meta.permission`, add `adminNavigation.js` with `permissions` / `anyPermissions`, and match backend controller `hasAuthority` for GET vs POST.
