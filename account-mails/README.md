# account-mails fallback

When SMTP is not configured, HAWK writes notification emails as text files in this directory.

Structure:
- `account-mails/<accountNumber>/mail-<timestamp>.txt`

To verify during testing:
1. Create/trigger a HIGH-severity alert transaction.
2. Check latest files under the account number folder.

