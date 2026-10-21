# WAEI Training Platform

A marketplace connecting **beneficiary organizations** with **training providers**. Providers publish programs; beneficiaries request a session, pay through **Moyasar**, and follow the request through delivery.

## Stack

- **Backend:** Spring Boot 4.1.1, Java 17+, Spring Security (JWT), Spring Data JPA
- **Database:** MySQL in normal use, H2 in the `dev` profile and in tests
- **Frontend:** Angular 22 (standalone components, no UI framework)
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

### 2. Frontend

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200`. The dev server proxies `/api` to `http://localhost:8080` (`frontend/proxy.conf.json`).

## Environment variables

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | HMAC key for JWTs (at least 32 characters) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Bootstrap platform administrator |
| `CORS_ORIGINS` | Comma-separated allowed origins (default `http://localhost:4200`) |
| `MOYASAR_SECRET_KEY` | Moyasar `sk_test_...` or `sk_live_...` key |
| `MOYASAR_WEBHOOK_SECRET` | Secret token configured on the Moyasar webhook |
| `MOYASAR_SUCCESS_URL` | Redirect after payment (default `http://localhost:4200/payment/callback`) |
| `MOYASAR_BACK_URL` | Back button URL on Moyasar checkout |

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
3. Set the same success URL in the environment (`MOYASAR_SUCCESS_URL`) or keep the default.

The webhook verifies `secret_token`, checks the amount and currency, and is idempotent per payment id. For local testing without a public webhook, the callback page calls `POST /api/v1/payment/sync-by-invoice/{invoiceId}` to pull the status from Moyasar directly.

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
