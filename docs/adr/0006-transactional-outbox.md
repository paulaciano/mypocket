# ADR-0006: Transactional outbox with a polling relay

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

After a transfer commits, a `TransferCompleted` event must reach Kafka. Writing to the database and
publishing to Kafka are two systems: publishing inside the transaction can emit events for
transactions that later roll back, and publishing after commit can lose the event if the process
crashes in between (dual write).

## Decision

- The event is inserted into an `outbox_events` table **in the same transaction** as the money
  movement (E1).
- A relay inside wallet-core polls unpublished events in creation order with
  `SELECT … FOR UPDATE SKIP LOCKED`, publishes them to Kafka with the wallet id as key, waits for the
  broker acknowledgement and marks them as published (E2, E3).
- Delivery is at least once. Every event has a unique `eventId` and consumers deduplicate by it.
- The relay is written explicitly (no framework), so the pattern is visible and testable.

## Alternatives considered

- **Change data capture (Debezium):** no polling and lower latency, but adds Kafka Connect and its
  operation to the platform.
- **Framework-provided event publication registry:** less code, but hides the mechanism behind
  framework conventions.
- **Publish directly after commit:** simplest, but loses events on crashes.

## Consequences

- Event latency is bounded by the polling interval (sub-second by default).
- Duplicates are possible after a crash between publish and mark; consumers must be idempotent.
- Published rows are kept; cleanup can be added if the table size becomes relevant.
