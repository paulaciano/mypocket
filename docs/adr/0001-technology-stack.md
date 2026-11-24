# ADR-0001: Technology stack

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

The platform moves money, so it needs strict transactional guarantees, row-level locking and a mature
ecosystem for security, persistence and messaging. It must start locally with one command and be
testable against real infrastructure.

## Decision

- **Java 25** (LTS) with **Spring Boot 4**, virtual threads enabled for request handling.
- **Maven** with the wrapper, and a root aggregator POM that only lists the services as modules.
- **PostgreSQL** as the single source of truth of wallet-core; schema managed with **Flyway**.
- **Apache Kafka** for domain events.
- **JUnit 5 + Testcontainers** to test against real PostgreSQL and Kafka.
- **Micrometer + Prometheus + Grafana** for metrics.
- **springdoc-openapi** for the API description.
- **Docker Compose** to run the whole platform.

## Alternatives considered

- **Gradle:** equivalent capabilities; Maven is more widespread in the target audience and its
  declarative build is easier to review.
- **Liquibase:** more features (rollbacks, XML/YAML changesets) that the project does not need; plain
  SQL migrations in Flyway are easier to read.
- **A cache/store such as Redis** for idempotency and limits: see [ADR-0005](0005-idempotency-in-postgresql.md).
- **Distributed tracing (OpenTelemetry):** with two services and one asynchronous hop, metrics and
  correlated logs are enough today.

## Consequences

- Integration tests and local runs require Docker.
- Virtual threads make thread count irrelevant; the database connection pool becomes the explicit
  concurrency limit.
