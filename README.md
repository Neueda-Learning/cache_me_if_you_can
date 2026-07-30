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

## Run tests

```powershell
.\mvnw.cmd test
```


