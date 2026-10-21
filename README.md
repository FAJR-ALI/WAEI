# WAEI Training Platform

A marketplace connecting **beneficiary organizations** with **training providers**. Providers publish programs; beneficiaries request a session, pay through **Moyasar**, and follow the request through delivery.

## Stack

- **Backend:** Spring Boot 4.1.1, Java 17+, Spring Security (JWT), Spring Data JPA
- **Database:** MySQL in normal use, H2 in the `dev` profile and in tests
- **Interface:** REST API only; no bundled web UI
- **Payments:** Moyasar Invoices API, refunds via Payments API

## Running locally

### 1. Backend

MySQL:

```bash
# Windows PowerShell
$env:DB_PASSWORD="your-mysql-password"
.\mvnw spring-boot:run
```

The database `waei_db` must exist. The connection defaults to `root@localhost:3306`; override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

No MySQL? Use the in-memory dev profile:

```bash
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The platform administrator is created on first start:

- **Email:** `admin@waei.sa`
- **Password:** `Admin@12345`

Override with `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

### 2. Using the API

The backend is the whole application; there is no separate web app to start. Call the endpoints directly with curl, Postman, or another client:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@waei.sa","password":"Admin@12345"}'
```

Send the returned token as `Authorization: Bearer <token>` on every other request.

After a Moyasar payment the browser returns to the backend-rendered page at
`http://localhost:8080/payment/callback/{paymentId}`, which pulls the current status from Moyasar.

## Environment variables

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | HMAC key for JWTs (at least 32 characters) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Bootstrap platform administrator |
| `CORS_ORIGINS` | Comma-separated allowed origins; empty by default (set it for browser clients) |
| `MOYASAR_SECRET_KEY` | Moyasar `sk_test_...` or `sk_live_...` key |
| `MOYASAR_WEBHOOK_SECRET` | Secret token configured on the Moyasar webhook |
| `MOYASAR_SUCCESS_URL` | Base return URL (default `http://localhost:8080/payment/callback`); the payment id is appended |
| `MOYASAR_BACK_URL` | Back button URL on Moyasar checkout (default `http://localhost:8080/payment/callback`) |

## Roles and scoping

- `PLATFORM_ADMIN` — manages organizations, accounts, and sees all payments/requests.
- `ORG_ADMIN` of a `PROVIDER` organization — manages its programs and accepts/starts/completes requests.
- `ORG_ADMIN` of a `BENEFICIARY` organization — creates requests and pays for them.

All write operations are scoped using the organization stored in the JWT, not values supplied by the client. New accounts start as `PENDING` and must be activated by a platform administrator before they can sign in.

## Training request lifecycle

```
PENDING ──► ACCEPTED ──► PAYMENT_PENDING ──► PAID ──► IN_PROGRESS ──► COMPLETED
   │            │                │              │
   ├──► REJECTED└────────────────┴──► CANCELLED ◄┘
```

- The beneficiary selects the program and the training **date/time**. The slot is held at request creation and conflict-checked again on acceptance (program row is locked).
- Fixed price: the payment amount is the program price snapshot stored on the request. `participantCount` does not change the price.
- Cancellation is allowed before the training starts. Cancelling a `PAID` request triggers a Moyasar refund.
- `IN_PROGRESS` and `COMPLETED` are terminal for cancellation/refunds.

## Payment lifecycle

```
PENDING ──► PAID ──► REFUNDED
   ├──► FAILED
   └──► CANCELLED
```

Moyasar status mapping: `initiated → PENDING`, `paid/captured → PAID`, `failed → FAILED`, `refunded → REFUNDED`, `voided → CANCELLED`.

Only one `PENDING` or `PAID` payment may exist per training request. Payment status is normally updated by the webhook; clients cannot declare a payment paid.

## Moyasar setup (test mode)

1. Get a test secret key from the Moyasar dashboard and set `MOYASAR_SECRET_KEY=sk_test_...`.
2. In **Settings → Webhooks**, add:
   - **URL:** `https://<public-host>/api/v1/payment/webhook/moyasar` (must be HTTPS; use ngrok locally)
   - **Secret token:** same value as `MOYASAR_WEBHOOK_SECRET`
   - **Events:** `payment_paid`, `payment_failed`, `payment_refunded`, `payment_voided`
3. The default return URL is the backend page at `http://localhost:8080/payment/callback`; the payment id is appended automatically. Override the base with `MOYASAR_SUCCESS_URL` only if the API is served from a different host.

The webhook verifies `secret_token`, checks the amount and currency, and is idempotent per payment id. For local testing without a public webhook, the return page pulls the invoice status from Moyasar server-side, so `POST /api/v1/payment/sync-by-invoice/{invoiceId}` is not needed just to see the result.

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/login`, `GET /api/v1/auth/me` |
| Organizations | `GET/POST/PUT/DELETE /api/v1/organization/...` (platform admin writes) |
| Accounts | `GET/POST/PUT/DELETE /api/v1/account/...`, `PUT /api/v1/account/status/{id}` |
| Programs | `GET /api/v1/training-program/...`, `POST /add`, `PUT /update/{id}`, `PUT /change-status/{id}` |
| Requests | `GET /api/v1/training-request/...`, `POST /add`, `POST /{id}/accept`, `/reject`, `/start`, `/complete`, `/cancel` |
| Payments | `POST /api/v1/payment/add`, `POST /{id}/refund`, `POST /{id}/sync`, `POST /sync-by-invoice/{invoiceId}`, `POST /webhook/moyasar` |

## Tests

```bash
.\mvnw test
```

Tests run against in-memory H2 so they do not touch the local MySQL database.
