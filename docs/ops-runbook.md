# Operations Runbook

## CI quality gates
- Backend pipeline runs `mvn verify` and fails on test or coverage gate failure.
- Frontend pipeline runs `npm run test:coverage` and fails on test or coverage gate failure.
- Coverage reports are uploaded as build artifacts for regression triage.

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

## Recovery playbook
- Restart backend and frontend services.
- Re-run backend integration tests and frontend unit tests.
- Confirm report schedules and notifications endpoints return expected payloads.
