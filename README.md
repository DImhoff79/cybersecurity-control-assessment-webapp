# Cybersecurity Control Assessment

Full-stack application for cybersecurity control assessments, with a Spring Boot backend and Vue 3 frontend.  
Supports role-based administration, audit execution, governance snapshots, and reporting workflows.

## Core Capabilities

- **Role + permission access model** with four roles:
  - `ADMIN`
  - `AUDIT_MANAGER`
  - `AUDITOR`
  - `APPLICATION_OWNER`
- **Role-default permissions** stored per user; the UI does not expose granular per-user permission editing. Spring Security method guards use **`PERM_*`** authorities derived from the user’s role defaults (and stored permission set) so `@PreAuthorize` matches service-layer checks.
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
  - Bootstrap initial published template from current mappings
- **Question–control mapping studio** (admin): unmapped question library, link to controls, maintenance helpers — route **`/admin/question-control-mapping-studio`** (requires `AUDIT_MANAGEMENT`).
- **Branching workflow (demo)** (admin): server-resolved conditional questionnaire graph, seeded demo data (Flyway **V36**), interactive “try run” — route **`/admin/branching-workflow-demo`** (requires `AUDIT_MANAGEMENT`).
- **Audit operations**:
  - Audit projects and kickoff workflows
  - Audit project scope editing with linked-audit reconciliation (add/remove scoped apps)
  - Automatic assignment of project-created audits to each scoped application's owner
  - Unified admin **Audit Queue** (triage + submitted reviews)
  - Assign/send/remind/attest
  - Self-service owner response flow
- **Workspace UX**:
  - Admin workspace uses left-sidebar + utility bar layout
  - Self-service workspace uses the same shell pattern with role-appropriate navigation
  - Admin workspace access is role-gated to `ADMIN`, `AUDIT_MANAGER`, and `AUDITOR`
- **Reporting**:
  - Dashboard metrics, trends, per-year/per-project tables
  - CSV/PDF exports
  - Auditor workbench and review queue
- **Sortable table headings** across major admin/self-service tables.
- **GRC / program management (Phase 2+)**:
  - Compliance obligations and requirement ↔ control mappings
  - Policies, versions, and regulatory scope
  - Risk register, risk ↔ control/finding links, remediation plans and actions
  - Findings workflow and control exception requests (including linkage to findings)
  - In-app notifications and scheduled report delivery hooks
  - Idempotent **demo dataset** seeder for local UI walkthroughs (toggle via config; off in `prod`)

## Prerequisites

- Java 17+
- Node.js 18+ and npm
- Maven not required (wrapper included)

## Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
# Skip test compilation (e.g. if working on main-only changes): ./mvnw spring-boot:run -Dmaven.test.skip=true
# Windows PowerShell: .\mvnw.cmd spring-boot:run "-Dmaven.test.skip=true"
```

- API: [http://localhost:8080](http://localhost:8080)
- H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- Default local DB URL (file + multi-process): `jdbc:h2:file:./data/assessment;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`  
  (`DB_CLOSE_ON_EXIT=FALSE` is **not** combined with `AUTO_SERVER` on H2 2.2+.)

Default users (ensured at startup if missing):

- `admin@example.com` / `admin123` (ADMIN)
- `audit.manager@example.com` / `manager123` (AUDIT_MANAGER)
- `auditor@example.com` / `auditor123` (AUDITOR)
- `owner@example.com` / `owner123` (APPLICATION_OWNER)

### Authentication notes

- **HTTP Basic** and **OAuth2 login** (when configured) both load **`ROLE_*`** and **`PERM_*`** into the security context so API calls match `@PreAuthorize` rules. If you change roles or permissions in the database, **log out and back in** (or clear the session) so a new authentication picks up authorities.
- If you see **403 Forbidden** on APIs while `/api/auth/me` looks correct, ensure you are not using a stale session from before this model was aligned.

## Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install
npm run dev -- --host
```

- App: [http://localhost:5173](http://localhost:5173)
- In development, API requests are proxied to backend on port **8080** (`vite.config.js`).
- Optional: set **`VITE_API_ORIGIN`** (e.g. another host/port) when not using the dev proxy.

## Quick Start

1. Start backend, then frontend.
2. Log in as `admin@example.com` / `admin123`.
3. Open **Admin Workspace → Applications** to create/manage applications.
4. Open **Admin Workspace → Questionnaire** to maintain controls/questions and governance versions.
5. Open **Admin Workspace → Audit Projects** to create projects and scope applications.
6. Open **Admin Workspace → Audit Queue** for triage and submitted review workflows.
7. Optional: **Questionnaire → Branching workflow (demo)** or **Mapping studio** from the questionnaire hub (audit-manager/admin permissions).

## Architecture Overview

### Frontend (`frontend/src`)

- **Shell and navigation**
  - `App.vue`: top-level app shell, self-service workspace layout, notification/account utility actions
  - `layouts/AdminLayout.vue`: admin workspace shell (left sidebar + top utility bar)
  - `config/adminNavigation.js`: section-based admin sidebar configuration
  - `router/index.js`: route definitions, auth checks, permission/role guards, and admin-role access redirects
- **State and API**
  - `stores/auth.js`: current user session, credentials, permission helpers
  - `services/api.js`: axios client, auth header injection, 401 handling; optional `VITE_API_ORIGIN`
  - `composables/useNotificationsMenu.js`: shared notifications menu loading/read interactions
  - `utils/loadErrorFormat.js`: shared API error messages for admin screens (e.g. branching demo)
- **Primary views**
  - `views/admin/UserManagement.vue`: role-driven user administration
  - `views/admin/Applications.vue`: application inventory CRUD
  - `views/admin/QuestionnaireHub.vue`: questionnaire entry point for builder, governance, mapping studio, branching demo
  - `views/admin/OperationsQueue.vue`: triage + submitted reviews shell
  - `views/admin/QuestionnaireBuilder.vue`: controls + questions maintenance
  - `views/admin/QuestionnaireTemplates.vue`: governance workflow and working snapshot lifecycle
  - `views/admin/QuestionControlMappingStudio.vue`: library + control linking
  - `views/admin/BranchingWorkflowDemo.vue`: demo branching graph + try run
  - `views/admin/KickoffAudit.vue`, `AuditProjects.vue`: audit operations and project scoping
  - `views/admin/Reports.vue`: reporting and export surfaces
  - `views/selfservice/MyAudits.vue`, `AuditRespond.vue`: owner and assignee workflows
- **Shared UI behavior**
  - `composables/useTableSort.js`: reusable sortable table heading behavior used across tables
  - `utils/adminNavMatch.js`: longest-prefix match for sidebar highlighting
  - `styles/theme.css`: global theme, sortable header visual treatment

### Backend (`backend/src/main/java/com/cyberassessment`)

- **Web/API layer**
  - `controller/*Controller.java`: REST endpoints for auth, users, governance, audits, reports, evidence, demo branching (`DemoBranchingWorkflowController`)
- **Business layer**
  - `service/*Service.java`: core domain logic (role constraints, snapshot lifecycle, reporting, task flows)
  - `service/DemoBranchingWorkflowService.java`: demo graph load/save/resolve
- **Persistence layer**
  - `entity/*`: JPA entities and enums (`UserRole`, `UserPermission`, audit/questionnaire models, `demo_branching_*` demo tables)
  - `repository/*Repository.java`: data access via Spring Data JPA
- **Security**
  - `security/CustomUserDetailsService.java`: loads **`ROLE_*`** and **`PERM_*`** from the database user for HTTP Basic / form-style authentication
  - `security/AppOAuth2UserService.java`: after OAuth2 user-info load, merges the same **`PERM_*`** authorities when a local user exists for the email
  - `config/SecurityConfig.java`: CORS, CSRF off, OAuth2 user service wiring
  - `service/CurrentUserService.java`: resolves current user from security context for service-layer permission checks
- **Database migration**
  - `resources/db/migration`: Flyway migrations (schema + seeds), current through **`V36`** (includes demo branching workflow tables/seed)
  - Run automatically on startup when Flyway is enabled

## Test Commands

### Frontend

```bash
cd frontend
npm run test:unit          # Vitest: all specs (unit + src/integration/*.integration.spec.js)
npm run test:integration   # Vitest: only src/integration/
npm run test:smoke         # Same as test:unit (fast local smoke)
npm run test:coverage      # LCOV/HTML under frontend/coverage (no global threshold gate by default)
npm run build
```

**Browser smoke (Playwright)** — optional; not part of CI until browsers are installed in the pipeline.

```bash
cd frontend
npx playwright install chromium   # once per machine / after @playwright/test upgrades
npm run test:e2e                  # starts Vite on 5173 unless CI=1 or you set PLAYWRIGHT_SKIP_WEBSERVER=1
npm run test:full                 # Vitest + Playwright
```

- E2E specs live under **`frontend/e2e/`** (see **`frontend/playwright.config.ts`**). Vitest excludes **`e2e/**`** so Playwright and Vitest do not double-run the same files.
- If a dev server is already on port **5173**, run: `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run test:e2e` (Windows PowerShell: `$env:PLAYWRIGHT_SKIP_WEBSERVER=1; npm run test:e2e`).

### Backend

```bash
cd backend
./mvnw test
# Windows: .\mvnw.cmd test
# Tests + JaCoCo coverage rules: ./mvnw verify  (Windows: .\mvnw.cmd verify)
```

**HTTP smoke / integration (subset)** — Spring **`MockMvc`** against an in-memory H2 database with Flyway migrations:

```bash
cd backend
./mvnw test -Dtest=ApplicationApiSmokeIntegrationTest
```

Covers public auth metadata, **`/api/auth/me`** (unauthenticated vs basic-auth user), actuator health (mail health disabled in-test so aggregate status is not DOWN without SMTP), and demo branching API rules.

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

Typical deployment is **Java Spring Boot backend + static Vue `dist/`** in front of a **PostgreSQL** database (no Dockerfiles are required in-repo; use your platform’s process manager or container).

### Database and migrations

- Flyway runs migrations on startup when `spring.flyway.enabled=true` (default).
- Ensure the database user can create/update schema (first deploy) and that you **back up before upgrades** when moving between application versions.
- **Never** use the embedded H2 file store for production.
- If a migration file is edited **after** it was already applied locally, Flyway may report a **checksum mismatch**. Use `flyway repair` against that environment, or fix the checksum entry in `flyway_schema_history` in line with your team policy—**do not** rewrite applied migration files in shared environments.

### Backend JAR

```bash
cd backend
./mvnw -q -DskipTests package
# Windows: .\mvnw.cmd -q -DskipTests package
# Artifact: backend/target/cybersecurity-assessment-0.0.1-SNAPSHOT.jar
```

Run with the **`prod`** profile (see `backend/src/main/resources/application-prod.yml`):

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL JDBC URL and credentials
- `app.seed.demo-dataset=false` (already default in prod profile) so demo rows are not seeded
- `app.auth.mode=sso` and `app.auth.allow-basic=false` in prod — configure OAuth2/SSO to match your IdP
- Optional: `EVIDENCE_STORAGE_MODE`, mail (`spring.mail.*`), `app.frontend-base-url` for absolute links in emails/notifications

### Frontend static assets

```bash
cd frontend
npm ci
npm run build
```

Serve the contents of `frontend/dist/` from your CDN or reverse proxy and route browser navigation to `index.html` (SPA fallback).

### Operations

- See **`docs/ops-runbook.md`** for CI parity commands, incident triage, and demo-seed toggles.
- See **`docs/dependency-upgrades.md`** for how to review Dependabot PRs safely.
- See `docs/scalability.md` for scalability guidance.
