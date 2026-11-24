# Architecture Decision Records

Each ADR records one significant decision: context, decision, alternatives and consequences.
An accepted ADR is never edited. When a decision changes, a new ADR supersedes it and both link to
each other.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-technology-stack.md) | Technology stack | Accepted |
| [0002](0002-services-communicate-only-through-kafka.md) | Two services in a monorepo, communicating only through Kafka | Accepted |
| [0003](0003-double-entry-ledger.md) | Double-entry ledger with cached balance on customer wallets | Accepted |
| [0004](0004-pessimistic-locking-in-deterministic-order.md) | Pessimistic locking in deterministic order | Accepted |
| [0005](0005-idempotency-in-postgresql.md) | Idempotency stored in PostgreSQL inside the business transaction | Accepted |
| [0006](0006-transactional-outbox.md) | Transactional outbox with a polling relay | Accepted |
| [0007](0007-self-issued-jwt.md) | Self-issued JWT for authentication | Accepted |
| [0008](0008-signed-provider-webhooks.md) | HMAC-signed webhooks from the payment provider | Accepted |
| [0009](0009-money-representation.md) | Money representation | Accepted |
| [0010](0010-platform-generated-identifiers.md) | Platform-generated identifiers instead of personal data | Accepted |
