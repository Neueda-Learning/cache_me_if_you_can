# Transaction Monitoring (Spring Boot + MySQL + JDBC)

This project is configured to use:
- Spring Boot REST API
- MySQL (no H2)
- Spring JDBC (no Hibernate/JPA entities)
- SQL-first schema initialization from `schema.sql`

## Prerequisites

- Java 21
- MySQL 8+
- Maven Wrapper (`mvnw.cmd` is included)

## Database

The app connects to:
- Database: `transaction_monitoring`
- URL: `jdbc:mysql://localhost:3306/transaction_monitoring`

Schema is auto-initialized at startup from:
- `src/main/resources/schema.sql`

If you prefer creating everything manually in MySQL Workbench, use:
- `database/init_mysql_workbench.sql`

Main transaction table name is:
- `TRANSACTION_TABLE`

## Configuration

Edit credentials in `src/main/resources/application.properties` if needed:

- `spring.datasource.username`
- `spring.datasource.password`

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Verify schema is loaded

Call the schema status endpoint:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/system/schema-status"
```

Expected response contains:
- `allRequiredTablesPresent: true`
- table flags for `ACCOUNT`, `PAYEE`, `RULE`, `TRANSACTION_TABLE`, `ALERT`

## Alert API (Lifecycle)

Base URL:
- `http://localhost:8080/api/v1/alerts`

Endpoints:
- `POST /api/v1/alerts`
- `GET /api/v1/alerts/{alertId}`
- `GET /api/v1/alerts?status=OPEN&severity=HIGH`
- `PATCH /api/v1/alerts/{alertId}/status`
- `PATCH /api/v1/alerts/{alertId}/acknowledge`
- `PATCH /api/v1/alerts/{alertId}/investigating`
- `PATCH /api/v1/alerts/{alertId}/close`
- `PATCH /api/v1/alerts/{alertId}/dismiss`

Create alert body example:

```json
{
  "ruleId": 1,
  "transactionId": 1,
  "severity": "HIGH"
}
```

Generic status update body example:

```json
{
  "status": "ACKNOWLEDGED"
}
```

## Quick endpoint tests (PowerShell)

```powershell
# 1) Create an alert
$createBody = @{
  ruleId = 1
  transactionId = 1
  severity = "HIGH"
} | ConvertTo-Json

$created = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/alerts" -ContentType "application/json" -Body $createBody
$alertId = $created.data.alertId

# 2) Move through lifecycle
Invoke-RestMethod -Method Patch -Uri "http://localhost:8080/api/v1/alerts/$alertId/acknowledge"
Invoke-RestMethod -Method Patch -Uri "http://localhost:8080/api/v1/alerts/$alertId/investigating"
Invoke-RestMethod -Method Patch -Uri "http://localhost:8080/api/v1/alerts/$alertId/close"

# 3) Fetch single alert
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/alerts/$alertId"
```

## Run tests

```powershell
.\mvnw.cmd -Dtest=AlertServiceImplTest test
```

Run all tests:

```powershell
.\mvnw.cmd test
```
