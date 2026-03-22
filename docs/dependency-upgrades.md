# Dependency upgrades (Dependabot)

Dependabot is configured in `.github/dependabot.yml`:

| Ecosystem        | Directory | Schedule | Open PR limit |
|------------------|-----------|----------|----------------|
| npm              | `frontend`| weekly   | 10             |
| Maven            | `backend` | weekly   | 10             |
| GitHub Actions   | `/`       | monthly  | (default)      |

## Review order (suggested)

1. **GitHub Actions** (`actions/checkout`, `setup-java`, `setup-node`, `upload-artifact`, etc.)  
   - Usually low risk; align major versions with Node/Java in `ci.yml` (Node 20, Java 17).

2. **npm (frontend)**  
   - **Patch/minor** (`axios`, `bootstrap`, etc.): merge after green CI.  
   - **Major** (`vite`, `vitest`, `vue`, `vue-router`, `pinia`): run `npm ci` and `npm run test:coverage && npm run build` locally; read upstream migration guides.

3. **Maven (backend)**  
   - **Patch/minor** (drivers, plugins, Spring Boot **3.x** line): merge after `.\mvnw.cmd verify`.  
   - **Spring Boot 4.x** (or other **major** jumps): treat as a **separate upgrade epic** — read release notes, re-validate security config, OAuth2, JPA, and Flyway; do not merge on green CI alone.

## PR checklist

- [ ] CI green for the PR branch (same gates as `master`).
- [ ] For majors: local `verify` / `test:coverage` + smoke login path.
- [ ] If `pom.xml` or `package-lock.json` changes conflict, prefer regenerating lockfile with `npm ci` after merge base update.

## Security

Use GitHub **Dependabot alerts** and `npm audit` / OWASP dependency-check as complementary signals; not every automated bump is appropriate for immediate merge.
