# HAWK Postman Quick Reference

## 1) Create rules in UI first (`/rules.html`)
Use these values so each alert type is easy to trigger:

- Rule 1: `THRESHOLD`, Severity `HIGH`, Threshold `10000`
- Rule 2: `VELOCITY`, Severity `CRITICAL`, Threshold `3`, Time Window `10`
- Rule 3: `NEW_PAYEE`, Severity `MEDIUM`
- Rule 4: `DAILY_LIMIT`, Severity `LOW`, Threshold `20000`

Keep all rules `ACTIVE`.

## 2) Create data in UI (`/accounts.html`, `/payees.html`)
- Create Account #2 with a valid email (important for notification):
  - Example account number: `123456789002`
  - Email: your real email for demo
  - Balance: enough for all demo transactions (for example `100000`)
- Create at least 2 payees:
  - `payeeAId` and `payeeBId`

## 3) Import and run Postman collection
- Import `POSTMAN_COLLECTION.json`
- Set variables:
  - `account2Id`, `payeeAId`, `payeeBId`
- Run in order:
  1. `Auth - Login`
  2. `Create Transaction - THRESHOLD`
  3. `Create Transaction - NEW_PAYEE`
  4. `Create Transaction - VELOCITY (Run 3x quickly)`
  5. `Create Transaction - DAILY_LIMIT (exceed day cap)`
  6. `Alerts - All`
  7. `Alerts - Grouped (Last 60 min)`

## 4) Verify popup and alert pages in UI
- Keep `dashboard.html` open to see popup notifications.
- Check `alerts.html` for status/severity filters.
- Open `alert-detail.html` for lifecycle actions.

## 5) Verify grouping deduplication
- Call `GET /api/v1/alerts/grouped?minutes=60`
- Expect grouped output by `ruleId + severity` with counts.

## 6) Verify email notifications for Account #2
The current backend sends notification when triggered alert severity is `HIGH`.

- Trigger HIGH alert using the THRESHOLD rule request.
- Verify one of these paths:
  - SMTP configured: email lands in account #2 email inbox.
  - SMTP not configured: file appears under `account-mails/<account-number>/`.

