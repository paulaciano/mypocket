# ADR-0002: Two services in a monorepo, communicating only through Kafka

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

Fraud detection reacts to transfers in real time and may restrict wallets. It has a different rhythm
of change and different scaling needs than the core ledger, and it must be possible to replace it with
an implementation in another language without changing wallet-core.

## Decision

- Two services, **wallet-core** and **fraud-detector**, in one repository under `services/`.
- They share **no code, no database and no synchronous API**. Their only contract is the set of Kafka
  topics and JSON event schemas documented in the [API design](../04-api.md#events-kafka).
- fraud-detector does not change wallets itself: it publishes `FraudAlertTriggered` with the requested
  status and wallet-core, the only owner of wallet state, applies the restriction.
- The root Maven POM aggregates both modules for a single build command; no module depends on the
  other.

## Alternatives considered

- **Fraud rules inside wallet-core:** simpler to run, but couples fraud logic to the ledger's release
  cycle and makes a replacement in another language impossible without touching the core.
- **fraud-detector calling a wallet-core admin endpoint to restrict the wallet:** adds a synchronous dependency,
  service credentials and retry handling; an event keeps the direction of dependency one-way.
- **Shared library for event classes:** couples the build of both services and blocks a non-Java
  replacement. The JSON contract is documented instead.

## Consequences

- Each service duplicates its own small event classes; contract tests in each service check the JSON
  shape against documented examples.
- Restricting after an alert is eventually consistent: a few transfers may complete between the alert
  and the restriction.
- Replacing fraud-detector means removing its module and deploying the new implementation against the
  same topics.
