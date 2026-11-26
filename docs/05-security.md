# 5. Security

## Authentication and authorization

- **S1** Customers and administrators authenticate with email and password and receive a signed JWT
  (HS256) valid for 15 minutes. There are no refresh tokens: when the token expires, the client logs
  in again ([ADR-0007](adr/0007-self-issued-jwt.md)).
- **S2** Passwords are hashed with BCrypt. Hashes and tokens are never logged or returned.
- **S3** Roles are `CUSTOMER` and `ADMIN`. `/admin/**` requires `ADMIN`. Customer endpoints require
  `CUSTOMER`.
- **S4** Ownership is checked in the application layer on every wallet, transfer and withdrawal
  access. A resource that belongs to someone else answers `404` (W10), so ids cannot be probed.

## Webhooks

- **S5** Provider webhooks carry `X-Provider-Timestamp` (Unix seconds) and `X-Provider-Signature`
  (`sha256=` + hex HMAC-SHA256 of `timestamp + "." + raw body`, with a shared secret). The signature
  is compared in constant time, before the body is parsed. Requests older or newer than 5 minutes
  and bodies larger than 16 KB are rejected ([ADR-0008](adr/0008-signed-provider-webhooks.md)).
- **S6** Replays inside the 5-minute window have no effect thanks to the provider reference
  deduplication (CI2) and the settlement rules (CO8).

## Input and data

- **S7** Every request body is validated (Bean Validation) before reaching the application layer.
  Unknown JSON properties are rejected.
- **S8** All database access uses parameterized queries (JPA / prepared statements).
- **S9** Error responses never expose stack traces, SQL or internal identifiers beyond the resource
  ids the caller already has.

## Personal data

- **S17** Personal data is minimized and confined to the user module (P1, P2,
  [ADR-0010](adr/0010-platform-generated-identifiers.md)). Other customers only ever see account
  numbers; events and logs carry identifiers, never emails.
- **S18** The JWT subject is the user id, not the email.

## Secrets and configuration

- **S10** Secrets (JWT secret, webhook secret, database password, admin bootstrap password) come from
  environment variables. The repository only contains example values in the development profile and
  in `compose.yaml`, clearly marked as non-production.
- **S11** The application refuses to start outside the development and test profiles if a secret is
  missing or still has an example value. In every profile, the JWT and webhook secrets must have at
  least 32 bytes.

## Network exposure

- **S12** `/actuator/prometheus` and the other actuator endpoints except `health` are not exposed
  outside the Docker network in `compose.yaml`.
- **S13** The OpenAPI description and Swagger UI are served only in the development profile.

## Supply chain

- **S14** New dependencies must use a version published at least one week earlier.
- **S15** GitHub Actions are pinned by commit SHA. Dependabot updates Maven dependencies, Docker
  images and Actions, checking at most every 3 days and only proposing versions published at least
  3 days earlier.
- **S16** CI runs with `permissions: contents: read` and checks out with `persist-credentials: false`.

## Known limitations (accepted)

- No rate limiting on login.
- Registration answers `409` when the email is already registered, which reveals that the email has
  an account. Avoiding it would require confirming registrations by email, which is out of scope.
- No token revocation before expiry (mitigated by the 15-minute lifetime).
- Restricting a wallet does not invalidate its owner's token; it blocks money movement (W3), not login.
- Account numbers can be probed by attempting transfers; they reveal no personal data (P2).
