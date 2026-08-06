# HAWK — Transaction Monitor

HAWK is a lightweight, rule-driven transaction monitoring and alerting platform. It provides:
- a Spring Boot backend with a REST API
- a small operator UI (static HTML/JS) for managing rules and viewing alerts
- a rules engine with common rule types (THRESHOLD, VELOCITY, DAILY_LIMIT, NEW_PAYEE)

This repository contains everything needed to run the app locally: source code, static frontend, DB schema, and a Postman collection for API testing.

---

## Table of contents

- Quick start (Docker)
- Quick start (Local MySQL)
- Configuration
- Build & run
- Tests
- API examples
- Frontend
- Troubleshooting
- Development notes
- Contributing
- License & contact

---

## Quick notes

- Project Java version: 21
- Spring Boot parent: 4.1.0 (see `pom.xml`)
- Database: MySQL 8.x (recommended)

---

## Quick start — Docker (recommended for fast local setup)

1. Ensure Docker Desktop is installed and running.
2. From the repository root run (PowerShell):
```powershell
docker-compose up -d --build
```

This will start the database service and (if configured in `docker-compose.yml`) the app. If the compose file only contains MySQL, start MySQL with compose and run the app locally with the Maven wrapper.

---

## Quick start — Local MySQL + Maven

1. Create the database or allow the app to create it (the default URL uses createDatabaseIfNotExist).
2. Build and run using the included Maven wrapper (Windows PowerShell):
```powershell
# build (skip tests to speed up)
.\mvnw.cmd -DskipTests package
# run
.\mvnw.cmd spring-boot:run
```

Open the UI (defaults)
- Dashboard: http://localhost:8080/dashboard.html
- Rules: http://localhost:8080/rules.html
- Alerts: http://localhost:8080/alerts.html
- Swagger UI (if enabled at runtime): http://localhost:8080/swagger-ui/index.html

---

## Database setup

- Default connection is configured in `src/main/resources/application.properties`.
- Default example values in the repo:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/transaction_monitoring?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=<your-db-password>
```
- If your MySQL user cannot create the DB, create it manually and apply the schema:
```powershell
# create DB (example)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS transaction_monitoring CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# apply schema
mysql -u root -p transaction_monitoring < src\main\resources\schema.sql
```

---

## Configuration

- Primary config file: `src/main/resources/application.properties` (also see `application-docker.properties`).
- Important properties:
    - `spring.datasource.*` — DB connection
    - `hawk.jwt.secret` — JWT signing secret (replace in production)
    - `hawk.jwt.expiry-hours` — token expiry
    - `hawk.admin.*` — default admin seed credentials (the app will seed an admin user at startup if none exists)
    - `spring.mail.*` — email settings (optional)

Security note: Do not commit production secrets. Use environment variables, external config, or a secrets manager for production deployments.

---

## Build & run

- Build artifact (jar):
```powershell
.\mvnw.cmd -DskipTests package
# produced jar in target/ (example: transaction_monitor-0.0.1-SNAPSHOT.jar)
```
- Run the packaged jar:
```powershell
java -jar target\transaction_monitor-0.0.1-SNAPSHOT.jar
```

---

## Tests

Run unit tests:
```powershell
.\mvnw.cmd test
```
Test reports are under `target/surefire-reports/`.

---

## API overview & examples

- Main endpoints (prefix /api/v1):
    - GET /rules — list rules
    - POST /rules — create rule
    - PUT /rules/{id} — update rule
    - DELETE /rules/{id} — delete rule
    - GET /alerts — list alerts
    - GET /transactions — transaction endpoints

Example: create a THRESHOLD rule (PowerShell / curl):
```powershell
curl -X POST http://localhost:8080/api/v1/rules `
  -H "Content-Type: application/json" `
  -d '{"ruleName":"High Value Check","ruleType":"THRESHOLD","severity":"HIGH","threshold":1000.00}'
```

Postman
- A Postman collection is included: `POSTMAN_COLLECTION.json` — import it into Postman to try the main endpoints.

---

## Frontend

- Static files are in `src/main/resources/static/`.
- Key pages:
    - `dashboard.html` — KPIs and charts
    - `rules.html` — rule management UI
    - `alerts.html` — alert list
    - `transactions.html` — transaction list

---

## Troubleshooting (common issues)

- DB connection failures:
    - Ensure MySQL is running and accessible on the configured host/port.
    - Verify the credentials in `application.properties` or environment overrides.
    - From PowerShell:
```powershell
Get-Service *mysql*
Test-NetConnection -ComputerName localhost -Port 3306
```
- Schema / missing tables: run `src/main/resources/schema.sql` against the DB.
- Email / JavaMailSender missing: the MailService falls back to writing mail files to disk if SMTP is not configured. To enable SMTP, add `spring-boot-starter-mail` and set `spring.mail.host`, etc.
- Lombok warnings in IDE: install Lombok support plugin if needed — builds with Maven still succeed.

---

## Development notes

- Seed admin user: configured via `hawk.admin.username`, `hawk.admin.password`, `hawk.admin.fullname` — the app will seed an admin if none exists at startup.
- Rules engine and DTOs:
    - `src/main/java/com/neueda/transaction_monitor/service/RuleEngineService.java`
    - `src/main/java/com/neueda/transaction_monitor/dto/RuleDto.java`

Where to look for tests
- Unit tests are under `src/test/java/com/neueda/` and test reports are generated to `target/surefire-reports/`.

---

## Contributing

- Use feature branches and open PRs to `master` (or the branch your team uses).
- Run unit tests before opening a PR:
```powershell
.\mvnw.cmd test
```

License & contact
- License: add your project's license here (e.g., MIT, Apache-2.0).
- Contact: your.email@company.com

Need anything else?
- I can:
    - add or verify `docker-compose.yml` for an end-to-end dev experience
    - generate an expanded Postman collection (examples + environment)
    - add example env files or a sample `.env` for Docker Compose

If you want me to also validate or update other files (for example `docker-compose.yml`, `pom.xml`, or `application.properties`) say which files and I will check and fix links/values.