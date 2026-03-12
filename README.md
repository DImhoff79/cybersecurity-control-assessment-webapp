# Cybersecurity Control Assessment

Full-stack application for cybersecurity control assessments, with a Spring Boot backend and Vue 3 frontend.  
Supports role-based administration, audit execution, governance snapshots, and reporting workflows.

## Core Capabilities

- **Role + permission access model** with four roles:
  - `ADMIN`
  - `AUDIT_MANAGER`
  - `AUDITOR`
  - `APPLICATION_OWNER`
- **Role-default permissions only** (no granular per-user overrides in UI/admin APIs).
- **User management** for create/update/delete with guardrails:
  - Existing user email/display name are read-only after creation.
  - Role changes automatically apply role defaults.
  - `AUDIT_MANAGER` assignment: admin only.
  - `AUDITOR` assignment/management: admin or audit manager.
- **Questionnaire Builder** (single-page controls + questions maintenance).
- **Questionnaire Governance** for versioned working snapshots:
  - Create working snapshot from current mappings
  - Review items
  - Publish
  - Delete working snapshots (`DRAFT`) by admin/audit manager
- **Audit operations**:
  - Audit projects and kickoff workflows
  - Audit project scope editing with linked-audit reconciliation (add/remove scoped apps)
  - Automatic assignment of project-created audits to each scoped application's owner
  - Unified admin **Operations Queue** (triage + submitted reviews)
  - Assign/send/remind/attest
  - Self-service owner response flow
  - Task delegation via My Tasks
- **Workspace UX**:
  - Admin workspace uses left-sidebar + utility bar layout
  - Self-service workspace uses the same shell pattern with role-appropriate navigation
  - Admin workspace access is role-gated to `ADMIN`, `AUDIT_MANAGER`, and `AUDITOR`
- **Reporting**:
  - Dashboard metrics, trends, per-year/per-project tables
  - CSV/PDF exports
  - Auditor workbench and review queue
- **Sortable table headings** across major admin/self-service tables.

## Prerequisites

- Java 17+
- Node.js 18+ and npm
- Maven not required (wrapper included)

## Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```

- API: [http://localhost:8080](http://localhost:8080)
- H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- Local DB URL: `jdbc:h2:file:./data/assessment`

Default users (ensured at startup if missing):

- `admin@example.com` / `admin123` (ADMIN)
- `audit.manager@example.com` / `manager123` (AUDIT_MANAGER)
- `auditor@example.com` / `auditor123` (AUDITOR)
- `owner@example.com` / `owner123` (APPLICATION_OWNER)

## Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install
npm run dev -- --host
```

- App: [http://localhost:5173](http://localhost:5173)
- In development, API requests are proxied to backend on port `8080`.

## Quick Start

1. Start backend, then frontend.
2. Log in as `admin@example.com` / `admin123`.
3. Open **Admin Workspace -> Applications** to create/manage applications.
4. Open **Admin Workspace -> Questionnaire** to maintain controls/questions and governance versions.
5. Open **Admin Workspace -> Audit Projects** to create projects and scope applications.
6. Open **Admin Workspace -> Operations Queue** for triage and submitted review workflows.

## Architecture Overview

### Frontend (`frontend/src`)

- **Shell and navigation**
  - `App.vue`: top-level app shell, self-service workspace layout, notification/account utility actions
  - `layouts/AdminLayout.vue`: admin workspace shell (left sidebar + top utility bar)
  - `config/adminNavigation.js`: section-based admin sidebar configuration
  - `router/index.js`: route definitions, auth checks, permission/role guards, and admin-role access redirects
- **State and API**
  - `stores/auth.js`: current user session, credentials, permission helpers
  - `services/api.js`: axios client, auth header injection, 401 handling
  - `composables/useNotificationsMenu.js`: shared notifications menu loading/read interactions
- **Primary views**
  - `views/admin/UserManagement.vue`: role-driven user administration
  - `views/admin/QuestionnaireHub.vue`: questionnaire entry point for builder + governance
  - `views/admin/QuestionnaireBuilder.vue`: controls + questions maintenance
  - `views/admin/QuestionnaireTemplates.vue`: governance workflow and working snapshot lifecycle
  - `views/admin/KickoffAudit.vue`, `AuditProjects.vue`: audit operations and project scoping
  - `views/admin/OperationsQueue.vue`: consolidated triage and submitted reviews
  - `views/admin/Reports.vue`: reporting and export surfaces
  - `views/selfservice/MyAudits.vue`, `MyTasks.vue`, `AuditRespond.vue`: owner and assignee workflows
- **Shared UI behavior**
  - `composables/useTableSort.js`: reusable sortable table heading behavior used across tables
  - `styles/theme.css`: global theme, sortable header visual treatment

### Backend (`backend/src/main/java/com/cyberassessment`)

- **Web/API layer**
  - `controller/*Controller.java`: REST endpoints for auth, users, governance, audits, reports, evidence
- **Business layer**
  - `service/*Service.java`: core domain logic (role constraints, snapshot lifecycle, reporting, task flows)
- **Persistence layer**
  - `entity/*`: JPA entities and enums (`UserRole`, `UserPermission`, audit/questionnaire models)
  - `repository/*Repository.java`: data access via Spring Data JPA
- **Security**
  - `security/CustomUserDetailsService.java`: role + permission authorities into Spring Security context
  - `service/CurrentUserService.java`: centralized role/permission checks used by services
- **Database migration**
  - `resources/db/migration`: Flyway migrations for schema and data changes
  - Current local evolution includes governance, activity, and production-readiness migrations through `V24`

## Test Commands

### Frontend

```bash
cd frontend
npm run test:unit
npm run build
```

### Backend

```bash
cd backend
./mvnw test
```

## Mail (Optional)

To enable owner-notification email, configure `backend/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-user
    password: your-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

## Production Notes

- Configure a production RDBMS (for example PostgreSQL) via datasource properties.
- Build and serve frontend assets:
  - `cd frontend && npm run build`
  - serve `frontend/dist`
- Set `app.frontend-base-url` for absolute links used in email/workflow notifications.
- See `docs/scalability.md` for scalability guidance.
