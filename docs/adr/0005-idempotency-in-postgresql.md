# ADR-0005: Idempotency stored in PostgreSQL inside the business transaction

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

Clients retry when the network fails. A retried transfer or withdrawal must not move money twice, and
the client must get the same answer it would have received the first time. Two copies of the same
request may also arrive at the same time.

## Decision

- Customer requests that move money carry an `Idempotency-Key`. Webhooks are deduplicated by the
  provider's reference.
- An `idempotency_records` table with a unique `(user_id, key)` stores a fingerprint of the request
  (operation + hash of the body) and the response (status and body).
- The record is **inserted at the start of the same database transaction** that moves the money, and
  its response is stored before commit. If a concurrent request with the same key is in flight, the
  unique index makes the second insert wait; when the first commits, the second fails on the
  constraint, reads the stored response and returns it (I5).
- Business rejections roll the transaction back, so they leave no record and can be retried (I6).
- Records do not expire (I7).

## Alternatives considered

- **External key-value store with TTL (e.g. Redis):** fast, but the key and the money movement are
  written in different systems; a crash between both writes either duplicates money or loses the
  response. It also adds a component to operate.
- **Unique constraint on the business tables only (no stored response):** prevents duplicates but
  cannot return the original response to the retry.

## Consequences

- Exactly-once effect is guaranteed by the database transaction, with no extra infrastructure.
- The table grows with every operation; a retention policy can be added if it ever matters.
- The idempotency check costs one indexed insert per operation.
