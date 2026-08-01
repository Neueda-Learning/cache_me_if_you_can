# HAWK Frontend (Static)

This folder contains the static frontend for the Transaction Monitoring project.

## Pages

- `index.html` - landing page
- `login.html` - operator login screen
- `signup.html` - lightweight registration screen (UI only)
- `dashboard.html` - KPI dashboard and recent alerts
- `transactions.html` - transaction list with filters
- `alerts.html` - alert list with lifecycle actions
- `alert-detail.html` - single alert view and actions
- `rules.html` - rule management and create form

## Branding

- Product name: **HAWK**
- Palette follows HSBC-inspired red/charcoal/white styling
- Logo is rendered from inline SVG in `js/common.js`

## How to Run

Run Spring Boot and open pages directly:

- `http://localhost:8080/`
- `http://localhost:8080/dashboard.html`
- `http://localhost:8080/swagger-ui/index.html`

## Notes

- API endpoints used by UI:
  - `/api/transactions/*`
  - `/api/v1/alerts/*`
  - `/api/v1/rules/*`
- Alerts and rules APIs return wrapped payloads (`{ message, data, timestamp }`).

