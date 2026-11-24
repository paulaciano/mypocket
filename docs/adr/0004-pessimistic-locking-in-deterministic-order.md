# ADR-0004: Pessimistic locking in deterministic order

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

Concurrent operations on the same wallet must not overdraw it or lose updates. A transfer touches two
wallets: if A→B and B→A run at the same time and each locks its source first, they deadlock. Popular
wallets (a shop receiving many payments) see high contention.

## Decision

- Every operation that changes a customer balance locks the affected customer wallet rows with
  `SELECT … FOR UPDATE` (JPA `PESSIMISTIC_WRITE`) before reading the balance.
- When more than one wallet is involved, rows are **locked in ascending wallet id order**, in a single
  query, regardless of which is source and which is destination.
- Transactions run at `READ COMMITTED`; the row locks provide the needed serialization.
- A lock timeout is configured so a stuck transaction fails fast instead of piling up requests.

## Alternatives considered

- **Optimistic locking (version column + retry):** works well under low contention, but under high
  contention most attempts fail and retry, wasting work and adding latency; retries also complicate
  idempotency.
- **`SERIALIZABLE` isolation:** correct, but serialization failures require retries for the same
  reasons, and they are harder to reason about.
- **Atomic conditional update (`UPDATE … SET balance = balance - x WHERE balance >= x`):** avoids
  explicit locks for one wallet, but the transfer still needs both rows consistent with the entries,
  and lock ordering is still required.

## Consequences

- Operations on the same wallet are serialized; throughput per wallet is bounded by transaction
  duration, so transactions must stay short (no remote calls inside them).
- Deadlocks between money operations are prevented by construction; the concurrency tests (T7)
  verify it.
