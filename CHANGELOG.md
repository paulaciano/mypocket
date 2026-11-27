# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-11-27

### Added

- P2P transfers `POST /api/v1/transfers`: from one of the customer's wallets to another wallet's
  account number, with an optional description, a per-operation limit per currency (ARS 1,000,000,
  USD 5,000) and status, currency and balance checks.
- `GET /api/v1/transfers/{transferId}`, visible to the sender and the recipient only.
- Idempotency for money-moving customer requests: a required `Idempotency-Key` header; a repeated
  request gets the original response with `Idempotent-Replayed: true`, and reusing a key for a
  different request answers `409 IDEMPOTENCY_KEY_REUSED`. Keys are per user and do not expire.
- Record of money operations rejected by a business rule (cash-ins and transfers), with their
  reason, kept for audit.
- A lock timeout: an operation that cannot lock its wallets within 5 seconds answers
  `503 SERVICE_BUSY` with `Retry-After: 1` and changes nothing.
- A glossary of domain terms in the documentation.

### Changed

- The error for an unknown account number is now `UNKNOWN_ACCOUNT_NUMBER` (was
  `ACCOUNT_NOT_FOUND`), also in the cash-in webhook and in the `reason` of the
  `mypocket.cashin.rejected` metric.
- Validation errors of headers name the header (for example `Idempotency-Key`).

### Security

- Transfer descriptions reject control, formatting and line separator characters, so they cannot
  disguise text or break log lines.
- A wallet that cannot send learns nothing about the destination of a transfer.

## [0.2.0] - 2026-11-26

### Added

- Double-entry ledger: every movement is one transaction with a debit and a credit of the same
  amount; customer wallets keep a cached balance that never goes negative; system wallets
  (`EXTERNAL`, `WITHDRAWALS_IN_TRANSIT`) per currency.
- Cash-in webhook `POST /api/v1/webhooks/cash-ins`, signed by the payment provider and naming the
  wallet by its account number. Each provider reference is applied once; a repetition returns the
  original result.
- Rejected cash-ins are logged and counted in the `mypocket.cashin.rejected` metric by reason.
- Wallet history `GET /api/v1/wallets/{walletId}/entries`, newest first and paginated.
- `scripts/provider.sh`, a payment provider simulator that signs and sends cash-ins.

### Changed

- Text fields, amounts included, only accept JSON strings: a JSON number or boolean is rejected as
  `MALFORMED_REQUEST`.
- Invalid query parameters answer `400 VALIDATION_ERROR` with the list of fields.

### Security

- The ledger is append-only, also in the database: updates and deletes of transactions and entries
  are rejected.
- Webhook signatures (HMAC-SHA256 over the raw body, 5-minute window, 16 KB body limit) are checked
  before the body is parsed, for every spelling of a webhook path.
- The webhook secret must have at least 32 bytes and no example value outside development and test.

## [0.1.0] - 2026-11-24

### Added

- Design documentation and architecture decision records.
- Maven multi-module build with the `wallet-core` service, PostgreSQL schema managed by Flyway,
  Docker image and Docker Compose platform.
- CI on GitHub Actions and Dependabot updates.
- Customer registration with email and password; ARS and USD wallets are opened automatically,
  each with a platform-generated account number.
- Login returning a short-lived JWT access token.
- Administrator account created from configuration; role-based access for customers and admins.
- `GET /api/v1/me`, `GET /api/v1/wallets` and `GET /api/v1/wallets/{walletId}`.
- Errors as RFC 9457 Problem Details with a stable `code`.
- OpenAPI description and Swagger UI in the development profile.

### Security

- Startup refuses missing secrets or example values outside the development and test profiles.
- Personal data stays out of logs and identifiers.

[0.3.0]: https://github.com/paulaciano/mypocket/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/paulaciano/mypocket/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/paulaciano/mypocket/releases/tag/v0.1.0
