# Backend — Cybersecurity Assessment API

Spring Boot 3 application (Java 17). See the **[repository root README](../README.md)** for full architecture, auth model, and production notes.

## Run locally

```bash
cd backend
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`
- Default JDBC (dev): `jdbc:h2:file:./data/assessment;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`

## Tests and coverage

```bash
./mvnw test
./mvnw verify          # includes JaCoCo coverage checks
```

**Focused HTTP smoke / integration** (in-memory H2, Flyway, `MockMvc`):

```bash
./mvnw test -Dtest=ApplicationApiSmokeIntegrationTest
```

Class: `com.cyberassessment.controller.ApplicationApiSmokeIntegrationTest` — auth providers, **`/api/auth/me`**, actuator health (mail health indicator disabled in-test), and demo branching workflow endpoints.

**Profiles** (narrow Surefire includes): `-Pbackend-smoke`, `-Pbackend-unit`, `-Pbackend-integration`, `-Pbackend-regression`. Example: `./mvnw test -Pbackend-smoke`.

On **Windows PowerShell**, quote JVM/Maven flags that contain dots, e.g. `.\mvnw.cmd spring-boot:run "-Dmaven.test.skip=true"`.

## Key packages

| Package | Role |
|--------|------|
| `controller` | REST endpoints |
| `service` | Domain logic |
| `repository` / `entity` | JPA |
| `security` | `CustomUserDetailsService`, `AppOAuth2UserService` |
| `resources/db/migration` | Flyway SQL (through **V50**; includes branching demo, regulatory scope, security review, intake, canonical flow) |
