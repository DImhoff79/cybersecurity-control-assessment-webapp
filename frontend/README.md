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
| `npm run test:unit` | Vitest |
| `npm run test:coverage` | Vitest + coverage report (`coverage/`) |

## Notable paths

| Path | Description |
|------|-------------|
| `src/router/index.js` | Routes, auth, permission guards |
| `src/stores/auth.js` | Session + permissions |
| `src/services/api.js` | Axios, Basic auth header, 401 redirect |
| `src/config/adminNavigation.js` | Admin sidebar |
| `src/views/admin/BranchingWorkflowDemo.vue` | Branching demo UI |
| `src/views/admin/QuestionControlMappingStudio.vue` | Mapping studio |
