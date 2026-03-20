# Operations Runbook

## CI quality gates
- Backend pipeline runs `mvn verify` and fails on test or coverage gate failure.
- Frontend pipeline runs `npm run test:coverage` and fails on test or coverage gate failure.
- Coverage reports are uploaded as build artifacts for regression triage.

### Local parity (before push)
- Backend: `cd backend && ./mvnw test` (or `mvnw.cmd test` on Windows); use `./mvnw verify` for the same JaCoCo rules as CI.
- Frontend: `cd frontend && npm run test:unit` for a quick run; `npm run test:coverage` matches the CI gate.
- Dependabot opens weekly PRs for `frontend` (npm), `backend` (Maven), and GitHub Actions (see `.github/dependabot.yml`).

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
