# WhatsApp payment confirmations

## Additional training request events

The same UltraMsg account and bundled logo are used for two more events, without n8n:

- New request: after `TrainingRequestService.addRequest` commits, the program's provider receives the beneficiary name, program title, `#` request number and scheduled start (Riyadh time), with instructions to review the request in Waei.
- Accepted request: after `TrainingRequestService.acceptRequest` commits, the beneficiary receives the provider name and request details, with instructions to complete payment in Waei. Acceptance does not claim payment or booking confirmation.

Attempt records live in the additive `training_request_whatsapp_notifications` table, unique per request/event. Existing payment notification records are unchanged. The same failure isolation, acceptance-versus-delivery distinction and no-automatic-retry limits described below apply. Duplicate callbacks do not resend an event; an invalid or missing number is recorded as failed. Existing requests are not replayed at startup.

`TrainingRequestWhatsAppIntegrationTests` verifies the real create/accept service flow with a mock sender: correct recipients, ownership restrictions, rollback, provider failures, disabled feature, missing numbers and duplicate/concurrent callbacks.

UltraMsg sends the existing `static/img/logo.png` with an Arabic payment confirmation caption to both organizations' `phoneNumber` fields:

- Beneficiary: request paid successfully, program, request number prefixed with `#`, amount and currency.
- Provider: beneficiary name and the same payment details. This confirms the customer's payment, not a payout to the provider.

Both captions use spaced sections and emoji labels. Payment transaction IDs are omitted; Saudi riyals use the new Saudi Riyal Sign (`U+20C1`) with two decimal places. Its appearance in WhatsApp depends on the recipient device's font support; captions cannot embed the site's SVG or custom fonts.

## Local setup

In IntelliJ's application run configuration, add these environment variables:

```text
WHATSAPP_ENABLED=true
ULTRAMSG_INSTANCE_ID=instanceYOUR_ID
ULTRAMSG_TOKEN=YOUR_TOKEN
```

Use the full instance identifier from UltraMsg. Never commit the token or share it in screenshots. The feature defaults to disabled. The instance must be authenticated and its trial/subscription active. Recipient numbers must have WhatsApp and the organizations must expect these payment messages. Saudi local `05...` numbers are normalized to `+9665...`; international numbers use `+` and country code.

Restart the application after setting the variables. With the existing `ddl-auto=update` configuration, Hibernate adds a separate `payment_whatsapp_notifications` table. Existing tables and data are not deleted. No new public endpoint or security exception is added.

## Trigger and reliability

The existing Moyasar flow invokes this feature only when a verified payment changes to `PAID`. Both messages are attempted after the database transaction commits; a rollback sends nothing. Existing successful payments are not replayed when enabling the feature.

Each payment/recipient role has one durable attempt record. A database lock plus a unique constraint prevents concurrent callbacks from sending duplicate attempts. One recipient's failure does not block the other or reverse the payment. The logo is sent as Base64 through UltraMsg's image endpoint, so no public logo URL is needed.

Statuses: `ATTEMPTED` (recorded before the network call), `ACCEPTED` (UltraMsg accepted/queued the request), `FAILED` (invalid/missing number or acceptance not confirmed). `ACCEPTED` does **not** mean delivered; check delivery in UltraMsg. Phone numbers and tokens are not written to application logs.

This is best-effort notification delivery, not a retry queue. No automatic retries are made: on timeout the provider may already have accepted the message, and blindly retrying risks duplicates. A process crash before the after-commit callback can miss a notification, and a crash after reservation can leave `ATTEMPTED`. Check UltraMsg before any manual resend. Sending occurs after commit with bounded HTTP timeouts and can add time to the callback response.

## Verification

`UltraMsgClientTest` checks the actual bundled logo, Arabic caption/form encoding, provider errors and disabled configuration with a mock HTTP server. `PaymentWhatsAppIntegrationTests` uses an in-memory database and a mocked UltraMsg client to verify both recipients, transaction rollback, amount validation, duplicate/concurrent callbacks and failure isolation. These tests send no real WhatsApp messages.

Verified: all 10 WhatsApp tests pass. The full project run reports 142 tests, with 2 failures in `PageRoutesIntegrationTests` (`homePagePromotesOrganizationRegistration` and `registrationFormsAreAvailableForBothOrganizationTypes`). Both failures were reproduced on a separate copy of HEAD before these changes; the related pages and tests were left unchanged.

For a live check, use organization numbers you control and perform one new successful Moyasar test payment. Expect one image with a caption per organization; if both organizations share one number, that number receives two role-specific messages. Refreshing the payment callback should not resend them.

UltraMsg API: https://docs.ultramsg.com/api/post/messages/image
