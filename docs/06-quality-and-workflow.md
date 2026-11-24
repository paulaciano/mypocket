# 6. Quality and Workflow

## Testing strategy

| Level | Scope | Tools |
|---|---|---|
| **Unit** | Domain rules: money, wallet state transitions, limits, fraud window. No Spring, no database. | JUnit 5, AssertJ |
| **Integration** | Use cases against a real PostgreSQL and Kafka: transactions, locking, idempotency, outbox, migrations, security. | Spring Boot Test, Testcontainers |
| **API** | HTTP contract: status codes, Problem Details, authentication, validation. | MockMvc |
| **Concurrency** | Many threads moving money between the same wallets at once; checks exact final balances, ledger balance (L1, L3) and absence of deadlocks (T7). | Testcontainers + `ExecutorService` |
| **End-to-end** | Manual check of the full platform before closing a phase. | `docker compose up` + `scripts/` |

Rules for tests:

- Every business rule has at least one test, and the test's display name starts with the rule id:
  `@DisplayName("T4: rejects transfer when balance does not cover the amount")`.
- Every bug fix comes with a test that reproduces it.
- No mocks of PostgreSQL or Kafka in integration tests.
- No skipped or disabled tests on `main`.

## Code quality

- Formatting is checked in the build (Spotless + Palantir Java Format); a badly formatted file fails
  `./mvnw verify`.
- Coverage is reported with JaCoCo; it is a signal for review, not a gate.
- Comments explain *why*, only where the reason is not obvious.
- Immutable request/response types as Java records.

## Build

```bash
./mvnw verify          # format check, compile, all tests (requires Docker)
docker compose up      # whole platform
```

## Continuous integration

GitHub Actions on every pull request and on `main`:

1. `./mvnw verify` for both services (Testcontainers runs on the runner's Docker).
2. Docker images build.

Workflow security: `permissions: contents: read`, `persist-credentials: false`, actions pinned by
commit SHA (S15, S16).

## Git workflow

- `main` is protected: changes arrive only through pull requests with green CI.
- Branches: `feat/…`, `fix/…`, `docs/…`, `chore/…`.
- Commits follow [Conventional Commits](https://www.conventionalcommits.org/).
- One pull request per functionality, small enough to review in one sitting.
- If code and documentation disagree, the same pull request fixes one of them.

## Versioning and releases

- [Semantic Versioning](https://semver.org/). Each roadmap phase ends with a minor version
  (`0.1.0`, `0.2.0`, …) until `1.0.0`.
- The last pull request of a phase updates `CHANGELOG.md` ([Keep a Changelog](https://keepachangelog.com/)),
  the README and the project version.
- The tag `vX.Y.Z` is created after that pull request is merged.

## Architecture Decision Records

Significant decisions are recorded in [`docs/adr`](adr/) with context, decision, alternatives and
consequences. An accepted ADR is never edited; a new ADR supersedes it and both are cross-linked.
