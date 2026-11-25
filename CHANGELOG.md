# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.1.0]: https://github.com/paulaciano/mypocket/releases/tag/v0.1.0
