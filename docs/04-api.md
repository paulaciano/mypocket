# 4. API Design

REST over JSON. Base path `/api/v1`. The OpenAPI description is generated from the code. In the
development profile only, it is served at `/v3/api-docs`, with Swagger UI at `/swagger-ui.html`.

## Conventions

- **Authentication:** `Authorization: Bearer <JWT>` on every endpoint except registration, login,
  webhooks and health.
- **Amounts:** JSON strings with a dot and up to 2 decimals (`"1500.50"`), never JSON numbers, so no
  client loses precision ([ADR-0009](adr/0009-money-representation.md)). A JSON number where a string
  is expected is rejected as `MALFORMED_REQUEST`.
- **Identifiers:** UUIDs.
- **Timestamps:** ISO-8601 in UTC (`2026-11-20T15:04:05Z`).
- **Idempotency:** `Idempotency-Key` header on money-moving customer operations (rules I1–I7).
- **Pagination:** `?page=0&size=20` (`page` from 0, `size` from 1 to 100). Responses include
  `content`, `page`, `size`, `totalElements` and `totalPages`.
- **Errors:** [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) Problem Details
  (`application/problem+json`) with a stable `code` field:

```json
{
  "title": "Insufficient funds",
  "status": 422,
  "code": "INSUFFICIENT_FUNDS",
  "detail": "Wallet balance does not cover the requested amount.",
  "instance": "/api/v1/transfers"
}
```

| Status | When |
|---|---|
| 400 | Invalid field, path or query parameter (`VALIDATION_ERROR`, with an `errors` list of `field` and `message`) or malformed body / unknown fields (`MALFORMED_REQUEST`) |
| 401 | Missing or invalid token (`UNAUTHORIZED`), wrong login credentials (`INVALID_CREDENTIALS`), missing, invalid or expired webhook signature (`INVALID_SIGNATURE`) |
| 403 | Authenticated but not allowed for the role: a customer calling an admin endpoint or an administrator calling a customer endpoint (`FORBIDDEN`) |
| 404 | Resource does not exist or belongs to someone else (W10), for example `WALLET_NOT_FOUND` |
| 409 | Conflicting state: email already registered (`EMAIL_ALREADY_REGISTERED`), cash-in reference already applied with other data (`CASH_IN_REFERENCE_CONFLICT`), idempotency key reused with another body, settling an already settled withdrawal with another outcome |
| 413 | Webhook body larger than 16 KB (`PAYLOAD_TOO_LARGE`) |
| 422 | Business rule rejected the operation (`INSUFFICIENT_FUNDS`, `WALLET_CANNOT_SEND`, `WALLET_CANNOT_RECEIVE`, `ACCOUNT_NOT_FOUND`, `LIMIT_EXCEEDED`, `CURRENCY_MISMATCH`, …) |

## Endpoints

### Authentication

| Method | Path | Description | Rules |
|---|---|---|---|
| POST | `/auth/register` | Register a customer (email and password) and open their wallets. `201` with user id, email and wallets. | U1–U3, W1, W2 |
| POST | `/auth/login` | Exchange email and password for an access token. | U4 |

```http
POST /api/v1/auth/login
{ "email": "ana@example.com", "password": "correct horse battery" }

200 OK
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900 }
```

```http
POST /api/v1/auth/register
{ "email": "ana@example.com", "password": "correct horse" }

201 Created
{ "userId": "…", "email": "ana@example.com",
  "wallets": [
    { "id": "…", "accountNumber": "7310582940", "currency": "ARS", "status": "ACTIVE", "balance": "0.00" },
    { "id": "…", "accountNumber": "4829103755", "currency": "USD", "status": "ACTIVE", "balance": "0.00" } ] }
```

### Customer

| Method | Path | Description | Rules |
|---|---|---|---|
| GET | `/me` | Current user: id, email and role. | P1 |
| GET | `/wallets` | My wallets with account number, balance and status, ARS first. | W10 |
| GET | `/wallets/{walletId}` | One of my wallets; another customer's wallet answers `404 WALLET_NOT_FOUND`. | W10, S4 |
| GET | `/wallets/{walletId}/entries` | Wallet history, newest first, paginated. | L6 |
| POST | `/wallets/{walletId}/close` | Close the wallet. `200` with the wallet. | W8, W9 |
| POST | `/transfers` | Send money. **Idempotency-Key.** `201`. | T1–T8, I1–I6 |
| GET | `/transfers/{transferId}` | A transfer I sent or received. | |
| POST | `/wallets/{walletId}/withdrawals` | Request a withdrawal. **Idempotency-Key.** `202` with status `PENDING`. | CO1–CO5, I1–I6 |
| GET | `/withdrawals/{withdrawalId}` | One of my withdrawals. | |

```http
GET /api/v1/wallets
Authorization: Bearer eyJ...

200 OK
[ { "id": "…", "accountNumber": "7310582940", "currency": "ARS", "status": "ACTIVE", "balance": "0.00" },
  { "id": "…", "accountNumber": "4829103755", "currency": "USD", "status": "ACTIVE", "balance": "0.00" } ]
```

```http
GET /api/v1/wallets/{walletId}/entries?page=0&size=20
Authorization: Bearer eyJ...

200 OK
{ "content": [
    { "id": "…", "transactionId": "…", "type": "TRANSFER", "direction": "DEBIT", "amount": "2500.00",
      "createdAt": "2026-11-20T15:04:05Z", "counterpartAccountNumber": "4829103755" },
    { "id": "…", "transactionId": "…", "type": "CASH_IN", "direction": "CREDIT", "amount": "10000.00",
      "createdAt": "2026-11-20T14:00:00Z", "counterpartAccountNumber": null } ],
  "page": 0, "size": 20, "totalElements": 2, "totalPages": 1 }
```

`counterpartAccountNumber` is the other wallet's account number on transfers and `null` otherwise.

```http
POST /api/v1/transfers
Idempotency-Key: 7f9c2ba4-e88f-11ee-8a1b-0242ac120002
{ "sourceWalletId": "…", "destinationAccountNumber": "4829103755", "amount": "2500.00", "description": "Dinner" }

201 Created
{ "id": "…", "sourceAccountNumber": "7310582940", "destinationAccountNumber": "4829103755",
  "currency": "ARS", "amount": "2500.00", "description": "Dinner", "createdAt": "2026-11-20T15:04:05Z" }
```

### Administrator

| Method | Path | Description | Rules |
|---|---|---|---|
| PUT | `/admin/wallets/{walletId}/status` | Set the status to `ACTIVE`, `DEBIT_BLOCKED` or `FROZEN`. Body: `{ "status": "FROZEN", "reason": "…" }`. `200` with the wallet. | W5, W7, W9 |

### Payment provider webhooks

Authenticated by HMAC signature, not by JWT ([ADR-0008](adr/0008-signed-provider-webhooks.md)).

| Method | Path | Description | Rules |
|---|---|---|---|
| POST | `/webhooks/cash-ins` | Funds arrived for a wallet, named by its account number. `201` first time, `200` with the original result on repetition. | CI1–CI7, S5, S6 |
| POST | `/webhooks/withdrawals/{withdrawalId}/settlement` | Outcome of a withdrawal. | CO6–CO9 |

```http
POST /api/v1/webhooks/cash-ins
X-Provider-Timestamp: 1763650000
X-Provider-Signature: sha256=5d41402abc4b2a76b9719d911017c592...
{ "reference": "PRV-000123", "accountNumber": "7310582940", "currency": "ARS", "amount": "10000.00" }

201 Created
{ "id": "…", "reference": "PRV-000123", "accountNumber": "7310582940", "currency": "ARS",
  "amount": "10000.00", "createdAt": "2026-11-20T15:04:05Z" }
```

```http
POST /api/v1/webhooks/withdrawals/{withdrawalId}/settlement
X-Provider-Timestamp: …
X-Provider-Signature: sha256=…
{ "outcome": "COMPLETED", "reference": "PRV-PAYOUT-778" }
```

### Operations

| Method | Path | Description |
|---|---|---|
| GET | `/actuator/health` | Liveness and readiness. |
| GET | `/actuator/prometheus` | Metrics (internal network only). |

## Events (Kafka)

JSON, one envelope for every event. The key is the wallet id, so events of one wallet keep their
order inside a partition (E3).

```json
{
  "eventId": "…",
  "type": "TransferCompleted",
  "version": 1,
  "occurredAt": "2026-11-20T15:04:05Z",
  "payload": { }
}
```

| Topic | Event | Producer → Consumer | Key | Payload |
|---|---|---|---|---|
| `wallet.transfers` | `TransferCompleted` | wallet-core → fraud-detector | sender wallet id | `transferId`, `sourceWalletId`, `destinationWalletId`, `currency`, `amount` |
| `fraud.alerts` | `FraudAlertTriggered` | fraud-detector → wallet-core | wallet id | `walletId`, `requestedStatus` (`DEBIT_BLOCKED` or `FROZEN`), `rule`, `detail` |

This contract is the only coupling between the services (ADR-0002). Events carry identifiers only, never personal data (P1).
