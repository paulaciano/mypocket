# 7. Roadmap

Small phases, each one usable on its own and closed with a version. Every phase starts with a review
of its open assumptions and ends with an updated CHANGELOG, README and tag.

| Version | Phase | Delivers | Rules |
|---|---|---|---|
| **0.1.0** | Foundation, users and wallets | Maven aggregator and wallet-core skeleton; PostgreSQL + Flyway; CI, Dependabot; Problem Details errors; registration, login (JWT), admin bootstrap; automatic ARS/USD wallets with account numbers; list and get my wallets. | M3, U1–U5, P1, P2, W1, W2, W10, S1–S4, S7–S11, S13, S17, S18 |
| **0.2.0** | Ledger and cash-in | Transactions, entries, system wallets, cached balances; signed cash-in webhook with reference deduplication; wallet history; payment provider simulator script; ledger reconciliation test. | M1, M2, M4, L1–L6, CI1–CI7, S5, S6 |
| **0.3.0** | P2P transfers and concurrency | Idempotency mechanism; transfers with deterministic locking; concurrent stress tests (opposite transfers, 100 threads). | I1–I7, T1–T8, L7 |
| **0.4.0** | Cash-out and limits | Withdrawals with in-transit hold, per-operation and daily limits, settlement webhook (completed / failed). | CO1–CO9 |
| **0.5.0** | Wallet lifecycle | Admin status changes (`ACTIVE` / `DEBIT_BLOCKED` / `FROZEN`), customer close; status checks enforced in every money operation. | W3–W9 |
| **0.6.0** | Events | Transactional outbox, polling relay, Kafka; `TransferCompleted`; consumer of `FraudAlertTriggered` that restricts wallets. | E1–E4, W6 |
| **0.7.0** | Fraud detector | Second service: sliding-window rule, alert emission, deduplication. | F1–F4 |
| **0.8.0** | Observability | Business and technical metrics, Prometheus, Grafana dashboard. | S12 |
| **1.0.0** | Release | Full `docker compose up` platform, independent project review and fixes, final README with architecture and trade-offs. | all |
