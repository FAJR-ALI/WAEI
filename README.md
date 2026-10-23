# WAEI Training Platform

A marketplace connecting **beneficiary organizations** with **training providers**. Providers publish programs; beneficiaries request a session, pay through **Moyasar**, and follow the request through delivery.

Organizations register themselves and upload a supporting document. A platform administrator reviews the document and verifies or rejects the registration; only verified organizations can sign in.

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

Send the returned token as `Authorization: Bearer <token>` on every protected request.

After a Moyasar payment the browser returns to the backend-rendered page at
`http://localhost:8080/payment/callback/{paymentId}`, which pulls the current status from Moyasar.

## Environment variables

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | HMAC key for JWTs (at least 32 characters) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Bootstrap platform administrator |
| `UPLOAD_DIR` | Directory for organization registration documents (default `uploads/organization-documents`) |
| `PROGRAM_IMAGE_DIR` | Directory for training program images (default `uploads/program-images`) |
| `CORS_ORIGINS` | Comma-separated allowed origins; empty by default (set it for browser clients) |
| `MOYASAR_SECRET_KEY` | Moyasar `sk_test_...` or `sk_live_...` key |
| `MOYASAR_WEBHOOK_SECRET` | Secret token configured on the Moyasar webhook |
| `MOYASAR_SUCCESS_URL` | Base return URL (default `http://localhost:8080/payment/callback`); the payment id is appended |
| `MOYASAR_BACK_URL` | Back button URL on Moyasar checkout (default `http://localhost:8080/payment/callback`) |
| `ZOOM_ACCOUNT_ID`, `ZOOM_CLIENT_ID`, `ZOOM_CLIENT_SECRET` | Zoom Server-to-Server OAuth app credentials |
| `ZOOM_HOST_EMAIL` | Licensed Zoom user that hosts the platform's training meetings |
| `ZOOM_TIMEZONE` | Zoom meeting timezone (default `Asia/Riyadh`) |
| `ZOOM_ENABLED` | Enables automatic Zoom meeting creation (default `true`; it is skipped when credentials are missing) |

## Roles and scoping

- `PLATFORM_ADMIN` — reviews organization registrations, verifies or rejects them, manages accounts, and sees all payments/requests.
- `ORG_ADMIN` of a `PROVIDER` organization — manages its programs and accepts/declines requests. Training start and completion advance automatically from the schedule.
- `ORG_ADMIN` of a `BENEFICIARY` organization — creates requests and pays for them.

All write operations are scoped using the organization stored in the JWT, not values supplied by the client. Organizations register themselves; the platform administrator does not create them. A registration creates the organization as `PENDING` and its `ORG_ADMIN` account as `PENDING`. Verification activates the account, so the organization can sign in.

## Organization registration and verification

```
register ──► PENDING ──► VERIFIED   (ORG_ADMIN account becomes ACTIVE)
                 │
                 └────► REJECTED ──► resubmit ──► PENDING
```

Registration is public and takes a `multipart/form-data` body: organization details, the organization administrator credentials, and one supporting document. Documents must be PDF, JPEG or PNG and are stored privately; only the platform administrator can download them.

```bash
# Self-registration (no token required)
curl -X POST http://localhost:8080/api/v1/organization/register \
  -F "name=Helping Hands Organization" \
  -F "type=BENEFICIARY" \
  -F "sector=Education" \
  -F "email=contact@helpinghands.sa" \
  -F "phoneNumber=0512345678" \
  -F "documentReference=CR-123456" \
  -F "adminFullName=Organization Administrator" \
  -F "adminEmail=admin@helpinghands.sa" \
  -F "adminPassword=Password@123" \
  -F "document=@commercial-registration.pdf"

# Platform admin: review queue, document, verify, reject
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/organization/registrations?status=PENDING"
curl -H "Authorization: Bearer <token>" -o document.pdf \
  http://localhost:8080/api/v1/organization/1/document
curl -X PUT -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/organization/1/verify
curl -X PUT -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"reason":"The document is not readable"}' \
  http://localhost:8080/api/v1/organization/1/reject

# Rejected organization: fix the data and resubmit (credentials verify the applicant)
curl -X POST http://localhost:8080/api/v1/organization/1/resubmit \
  -F "name=Helping Hands Organization" \
  -F "type=BENEFICIARY" \
  -F "sector=Healthcare" \
  -F "email=contact@helpinghands.sa" \
  -F "phoneNumber=0512345678" \
  -F "adminEmail=admin@helpinghands.sa" \
  -F "password=Password@123" \
  -F "document=@updated-registration.pdf"
```

Sign in only works once the organization is `VERIFIED` and its account is `ACTIVE`. A rejected organization is told the rejection reason when it tries to sign in.

## Training request lifecycle

```
PENDING ──► ACCEPTED ──► PAYMENT_PENDING ──► CONFIRMED ──► IN_PROGRESS ──► COMPLETED
   │            │                │              │
   ├──► REJECTED└────────────────┴──────────────┴──► CANCELLED
```

- Each provider program has a daily price, fixed `hoursPerDay`, and `maxDays` (`1..365`).
- The beneficiary selects the start date/time and a number of consecutive days (`1..maxDays`). Every day starts at the same time; the end is calculated from the last day plus `hoursPerDay`.
- The held slot covers the full multi-day range and is conflict-checked again on acceptance (program row is locked).
- Total price: `daily price × daysCount`. `participantCount` is recorded but does not change the amount. The request stores `daysCount`, `dailyPrice`, `hoursPerDay`, and the total in `programPrice`.
- Program categories are fixed: `صحة`, `سلامة`, `إسعافات أولية`, `صحة نفسية`, `تغذية`, `لياقة بدنية`.
- Program images are optional (JPEG, PNG, or WebP, max 5 MB). Images are public via `GET /api/v1/training-program/{id}/image`.
- Training status advances automatically: `CONFIRMED` becomes `IN_PROGRESS` at `scheduledStart`, then `COMPLETED` at `scheduledEnd`. A startup run catches up requests whose schedule passed while the app was offline. `completedAt` records the scheduled end.
- The scheduler runs at startup and every 60 seconds (`app.training.status-refresh-ms`).
- Providers cannot manually start or complete training; the manual start/complete endpoints were removed.
- Cancellation is allowed before the training starts. Cancelling a `PENDING`, `ACCEPTED`, or `PAYMENT_PENDING` request cancels any pending payment attempt without a refund.
- Cancelling a `CONFIRMED` request before `scheduledStart` triggers a full Moyasar refund. Any paid payment found during cancellation is refunded before the request is marked `CANCELLED`.
- `IN_PROGRESS` and `COMPLETED` are terminal for cancellation/refunds.
- Beneficiary and provider dashboards expose a `CANCELLED` filter and show the cancellation/rejection reason, cancellation date, payment status, and refund details when available.

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

## Zoom online meetings

Online programs get a Zoom meeting automatically after a paid training request reaches `CONFIRMED` or `IN_PROGRESS`.

1. Create a **Server-to-Server OAuth** app in the Zoom App Marketplace.
2. Add the scopes `meeting:write:admin`, `meeting:read:admin`, and `meeting:delete:admin`.
3. Use a **licensed** host user. Free Zoom accounts limit group meetings to 40 minutes.
4. Set `ZOOM_ACCOUNT_ID`, `ZOOM_CLIENT_ID`, `ZOOM_CLIENT_SECRET`, and `ZOOM_HOST_EMAIL`.
5. Restart the app.

Behavior:

- The reconciler runs every 30 seconds and also backfills active paid online requests at startup.
- The beneficiary sees a **Join Zoom** link in the dashboard after the meeting is created.
- The provider/admin can request a fresh host start link from the dashboard. Host URLs are not stored because Zoom expires them after about two hours.
- One meeting is used for the whole request. Programs up to 50 days use a daily recurring meeting; longer programs use a recurring meeting with no fixed time to stay within Zoom's recurrence limit.
- Cancelling or refunding the request deletes the Zoom meeting and clears the join link. If Zoom is unavailable, the local link is still cleared.
- Zoom failures never block a payment. Failed attempts are retried after a five-minute cooldown.
- A missing Zoom configuration disables meeting creation without affecting the rest of the platform.
- Zoom limits meeting creation to 100 requests per host per day.

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/login`, `GET /api/v1/auth/me` |
| Registration | `POST /api/v1/organization/register`, `POST /api/v1/organization/{id}/resubmit` (public) |
| Verification | `GET /api/v1/organization/registrations`, `GET /api/v1/organization/{id}/document`, `PUT /api/v1/organization/{id}/verify`, `PUT /api/v1/organization/{id}/reject` (platform admin) |
| Organizations | `GET /api/v1/organization/get...`, `PUT /api/v1/organization/update/{id}`, `DELETE /api/v1/organization/delete/{id}` (platform admin writes) |
| Accounts | `GET/POST/PUT/DELETE /api/v1/account/...`, `PUT /api/v1/account/status/{id}` |
| Programs | `GET /api/v1/training-program/...`, `GET /categories`, `POST /add`, `PUT /update/{id}`, `PUT /change-status/{id}`, `GET/POST/DELETE /{id}/image` |
| Requests | `GET /api/v1/training-request/...`, `POST /add`, `POST /{id}/accept`, `/reject`, `/cancel`, `GET /{id}/meeting/host-link` |
| Payments | `POST /api/v1/payment/add`, `POST /{id}/refund`, `POST /{id}/sync`, `POST /sync-by-invoice/{invoiceId}`, `POST /webhook/moyasar` |

## Tests

```bash
.\mvnw test
```

Tests run against in-memory H2 so they do not touch the local MySQL database.
