# Operations Runbook

## Release smoke checklist (priority order)

Run these **before** tagging or promoting a build; they mirror `.github/workflows/ci.yml`.

1. **Backend (Windows / PowerShell)**  
   `cd backend` → `.\mvnw.cmd verify`  
   Confirms unit/integration tests and JaCoCo coverage gates (`backend/pom.xml`).

2. **Frontend**  
   `cd frontend` → `npm ci` → `npm run build` → `npm run test:coverage`  
   Confirms production build and Vitest coverage thresholds (`frontend/vitest.config.js`).

3. **Manual full-stack** (local): start backend (`.\mvnw.cmd spring-boot:run`), start frontend (`npm run dev`), log in, open **Admin → Operations Queue** and one **GRC** screen (e.g. **Findings** or **Risk Register**) to confirm API + migrations on your target DB.

4. **Production DB**: ensure Flyway has applied through the latest migration (see `backend/src/main/resources/db/migration/V*.sql`) and **never** point `prod` at the dev H2 file URL.

Artifacts: CI uploads `backend-jacoco-report` and `frontend-coverage-report` for regression triage.

## CI quality gates
- Backend pipeline runs `mvn verify` and fails on test or coverage gate failure.
- Frontend pipeline runs `npm run test:coverage` and fails on test or coverage gate failure.
- Coverage reports are uploaded as build artifacts for regression triage.

### Local parity (before push)
- Backend: `cd backend && ./mvnw test` (or `mvnw.cmd test` on Windows); use `./mvnw verify` for the same JaCoCo rules as CI.
- Frontend: `cd frontend && npm run test:unit` for a quick run; `npm run test:coverage` matches the CI gate.
- Dependabot opens **weekly** PRs for `frontend` (npm) and `backend` (Maven), and **monthly** for GitHub Actions (see `.github/dependabot.yml`). Review guidance: **`docs/dependency-upgrades.md`**.

## Service health checks
- Backend health endpoint: `/actuator/health` (authentication required by default).
- Frontend service should serve from `http://localhost:5173` in local dev.

## Common incidents
- **Login failures in local dev**
  - Verify backend is reachable on port `8080`.
  - Check frontend proxy and `auth_mode`/`auth_credentials` in browser localStorage.
- **Evidence lifecycle not advancing**
  - Confirm scheduler is enabled (`app.automation.enabled=true`).
  - Validate lifecycle policy settings in `application.yml`.
- **Scheduled exports not sending**
  - Validate mail server configuration (`spring.mail.*`).
  - Review automation logs for `ReportScheduleService` failures.
- **Extra demo rows after local startup**
  - With `app.auth.seed-local-users=true` (default locally), the backend runs an idempotent **demo dataset** seeder (`DemoDatasetSeeder`) so applications, audits, findings, exceptions, risks, remediation, and sample compliance obligations each have several linked records for UI walkthroughs.
  - Toggle with `app.seed.demo-dataset` / env `APP_SEED_DEMO_DATASET` (default **true** in `application.yml`; **false** in `application-prod.yml`). Integration tests force it off via `src/test/resources/application.properties`.

## Recovery playbook
- Restart backend and frontend services.
- Re-run backend integration tests and frontend unit tests.
- Confirm report schedules and notifications endpoints return expected payloads.
