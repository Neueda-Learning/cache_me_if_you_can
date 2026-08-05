# HAWK External Simulation Testing Guide

This guide covers:
1. Rules to create in UI
2. Postman transactions to trigger all alert types
3. Grouped deduplication validation
4. Email notification validation for Account #2

## A. Preconditions

1. Start backend and database.
2. Login UI as admin.
3. Ensure Account #2 exists with enough balance and a valid email.
4. Ensure at least 2 payees exist.

## B. Rule Setup in UI (`/rules.html`)

Create exactly these rules:

### 1) THRESHOLD
- Rule Name: `High Value Check`
- Rule Type: `THRESHOLD`
- Severity: `HIGH`
- Threshold: `10000`
- Time Window: blank

### 2) VELOCITY
- Rule Name: `Rapid Burst Check`
- Rule Type: `VELOCITY`
- Severity: `CRITICAL`
- Threshold: `3`
- Time Window: `10`

### 3) NEW_PAYEE
- Rule Name: `First Time Payee`
- Rule Type: `NEW_PAYEE`
- Severity: `MEDIUM`
- Threshold: blank
- Time Window: blank

### 4) DAILY_LIMIT
- Rule Name: `Daily Outflow Limit`
- Rule Type: `DAILY_LIMIT`
- Severity: `LOW`
- Threshold: `20000`
- Time Window: blank

Keep all rules active.

## C. Postman Setup

Import `POSTMAN_COLLECTION.json` and set variables:
- `baseUrl` -> `http://localhost:8080`
- `username` -> `admin`
- `password` -> `Admin@1234`
- `account2Id` -> account id to test for email (usually `2`)
- `payeeAId` -> existing payee id (already used pair)
- `payeeBId` -> a different payee id (for NEW_PAYEE)

## D. Transaction Sequence to trigger all alerts

Run requests in this order:

1. `Auth - Login`
2. `Create Transaction - THRESHOLD`
   - amount 12000 > 10000 should trigger HIGH alert
3. `Create Transaction - NEW_PAYEE`
   - use `payeeBId` not used before for account2
4. `Create Transaction - VELOCITY (Run 3x quickly)`
   - run same request 3 times within 10 min
5. `Create Transaction - DAILY_LIMIT (exceed day cap)`
   - this pushes day total above 20000

Then run:
- `Alerts - All`
- `Alerts - Grouped (Last 60 min)`

## E. UI checks after simulation

1. Open `dashboard.html`
   - popup appears for new alerts
   - open alert count updates
2. Open `alerts.html`
   - filter by HIGH/CRITICAL/MEDIUM/LOW
3. Open `alert-detail.html`
   - verify rule, transaction id, account/payee numbers

## F. Grouped deduplication check

Call:
- `GET /api/v1/alerts/grouped?minutes=60`
- optional: `GET /api/v1/alerts/grouped?minutes=60&severity=HIGH`

Expected:
- grouped entries with `ruleId`, `severity`, `count`, `firstCreated`, `message`
- counts align with transactions you pushed in the last 60 minutes

## G. Email notification for Account #2

Current behavior in backend:
- notification path runs for `HIGH` severity alerts.
- THRESHOLD rule above is HIGH, so it is the easiest trigger.

### Steps
1. Ensure Account #2 has email set in UI Accounts form.
2. Trigger `Create Transaction - THRESHOLD`.
3. Validate delivery:
   - SMTP configured -> mailbox receives email.
   - SMTP not configured -> file fallback in `account-mails/<accountNumber>/`.

### Optional verification command (Windows PowerShell)

```powershell
Set-Location "C:\transaction_monitor"
Get-ChildItem -Path ".\account-mails" -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 10 FullName, LastWriteTime
```

If files are present, open latest and verify subject/body includes flagged transaction details.

