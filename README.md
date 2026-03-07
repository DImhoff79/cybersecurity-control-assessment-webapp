# Cybersecurity Control Assessment

Full-stack application for annual cybersecurity control assessments: Spring Boot backend and Vue.js frontend with admin and self-service flows.

## Features

- **Admin:** Manage applications (with owners), control catalog (NIST 800-53 Low, PCI DSS v4, HIPAA, SOX), plain-English questions per control, create audits, assign and send audits to application owners.
- **App owners:** "My Audits" list and a question-based flow to complete assessments; answers map to controls in the background.
- **Audit history:** Multiple records per application per year; data is preserved.

## Prerequisites

- Java 17+
- Node.js 18+ and npm
- Maven (optional; use `./mvnw` if present)

## Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
# Or: mvn spring-boot:run
```

- API: http://localhost:8080
- H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/assessment`)

Default users (created on first run):

- **Admin:** admin@example.com / admin123
- **App owner:** owner@example.com / owner123

## Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev
```

- App: http://localhost:5173
- API requests are proxied to http://localhost:8080 when running in dev.

## Quick start

1. Start backend, then frontend.
2. Log in as **admin@example.com** / **admin123**.
3. **Admin → Applications:** Add an application and set its owner (e.g. owner@example.com).
4. **Admin → Controls:** Enable/disable or edit controls; add plain-English questions per control.
5. **Admin → Audits:** Create an audit (application + year), assign to the app owner, then "Assign & send to owner" (sends email if mail is configured).
6. Log out and log in as **owner@example.com** / **owner123**. Open **My Audits**, then **Fill out audit** to answer the plain-English questions.

## Mail (optional)

To send "Send to owner" emails, set in `backend/src/main/resources/application.yml`:

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

## Production

- Use PostgreSQL: set `spring.datasource.url`, driver, username, password and add PostgreSQL Flyway migration if needed.
- Build frontend: `cd frontend && npm run build`; serve the `dist/` folder and set `app.frontend-base-url` for email links.
