# ADR-0003: Double-entry ledger with cached balance on customer wallets

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

A wallet balance must be auditable: it has to be possible to explain every cent from the history.
At the same time, checking the balance before each debit has to be fast and safe under concurrency.

## Decision

- Every money movement is a **transaction** made of **ledger entries** (`DEBIT` / `CREDIT`) whose
  totals are equal (L1). Entries are append-only (L2).
- Money entering or leaving the platform, and money of pending withdrawals, is booked against
  **system wallets** (`EXTERNAL`, `WITHDRAWALS_IN_TRANSIT`) per currency.
- **Customer wallets keep a cached `balance` column**, updated in the same database transaction as
  the entries, while the wallet row is locked (ADR-0004). A database constraint keeps it non-negative
  (L4).
- **System wallets have no cached balance** (L5): they participate in a large share of operations,
  and locking and updating a single row for each of them would serialize all cash-ins and withdrawals.
- A reconciliation test checks that every cached balance equals the sum of its entries (L3).

## Alternatives considered

- **Balance only in a column (no ledger):** fast, but not auditable and corrections overwrite history.
- **Balance always computed from entries:** no redundancy, but every debit needs an aggregate over a
  growing table and there is no single row to lock for serialization.
- **Cached balance on system wallets too:** consistent model, but turns them into a global hot spot.

## Consequences

- The balance exists twice (column and entries); the invariant is protected by keeping all writes in
  one ledger component and by the reconciliation test.
- System wallet balances are computed on demand; they are only needed for audits and metrics.
