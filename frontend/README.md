# Frontend — Cybersecurity Assessment UI

Vue 3 + Vite + Pinia + Bootstrap 5. See the **[repository root README](../README.md)** for features, roles, and deployment.

## Run locally

Requires the API on **port 8080** (or set `VITE_API_ORIGIN`).

```bash
cd frontend
npm install
npm run dev -- --host
```

App: `http://localhost:5173` — `/api` is proxied to `http://127.0.0.1:8080` in dev (`vite.config.js`).

## Scripts

| Command | Purpose |
|---------|---------|
| `npm run dev` | Dev server |
| `npm run build` | Production bundle → `dist/` |
| `npm run preview` | Preview production build |
| `npm run test:unit` | Vitest (all tests under `src/`, including `src/integration/`) |
| `npm run test:integration` | Vitest — only `src/integration/` |
| `npm run test:smoke` | Vitest — fast subset (API, auth store, router smoke, `NewApplicationIntake.spec.js`, etc.) |
| `npm run test:regression` | Vitest — full `src/` run (same as `test:unit`) |
| `npm run test:coverage` | Vitest + coverage report (`coverage/`) |
| `npm run test:e2e` | Playwright — all specs in `e2e/` (install browsers once: `npx playwright install chromium`) |
| `npm run test:e2e:smoke` | Playwright — `e2e/smoke.spec.ts` |
| `npm run test:e2e:regression` | Playwright — `e2e/regression.spec.ts` (builder, branching demo, audit respond, …) |
| `npm run test:e2e:ui` | Playwright UI mode |
| `npm run test:full` | `test:unit`, then `test:integration`, then `test:e2e` |

Playwright uses **`playwright.config.ts`**. Set **`PLAYWRIGHT_SKIP_WEBSERVER=1`** if Vite is already running on port **5173**. **`e2e/`** is excluded from Vitest via **`vitest.config.js`**. Shared login: **`e2e/helpers/login.ts`**.

## Notable paths

| Path | Description |
|------|-------------|
| `src/router/index.js` | Routes, auth, permission guards |
| `src/stores/auth.js` | Session + permissions |
| `src/services/api.js` | Axios, Basic auth header, 401 redirect |
| `src/config/adminNavigation.js` | Admin sidebar |
| `src/views/admin/BranchingWorkflowDemo.vue` | Branching demo UI |
| `src/views/admin/QuestionControlMappingStudio.vue` | Mapping studio |
