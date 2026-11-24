# MyPocket

Backend core of a digital wallet: multi-currency wallets, cash-in, instant P2P transfers and
withdrawals, on an immutable double-entry ledger, with real-time fraud detection through Kafka events.

> **Status:** under development, working towards `0.1.0`. See the [documentation](docs/README.md) and
> the [roadmap](docs/07-roadmap.md).

## Requirements

- Java 25
- Docker (integration tests use Testcontainers; the platform runs with Docker Compose)

Maven is not required: the project ships the Maven wrapper (`./mvnw`).

## Build and test

```bash
./mvnw verify
```

Checks formatting, compiles and runs all tests against a real PostgreSQL started by Testcontainers.
Fix formatting with `./mvnw spotless:apply`.

## Run

Whole platform:

```bash
docker compose up --build
curl localhost:8080/actuator/health
```

For development, run PostgreSQL in Docker and the service from the sources (development profile,
example credentials only):

```bash
docker compose up -d postgres
./mvnw -pl services/wallet-core spring-boot:run
```

## License

[MIT](LICENSE)
