# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.2.0]: https://github.com/paulaciano/mypocket/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/paulaciano/mypocket/releases/tag/v0.1.0
