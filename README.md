# MyPocket

Backend core of a digital wallet: multi-currency wallets, cash-in, instant P2P transfers and
withdrawals, on an immutable double-entry ledger, with real-time fraud detection through Kafka events.

> **Status:** `0.2.0`: users, wallets, double-entry ledger and signed cash-ins. Next: P2P transfers. See the
> [documentation](docs/README.md), the [roadmap](docs/07-roadmap.md) and the [changelog](CHANGELOG.md).

## Requirements

- Java 25
- Docker (integration tests use Testcontainers; the platform runs with Docker Compose)

Maven is not required: the project ships the Maven wrapper (`./mvnw`).

## Build and test

```bash
./mvnw verify
```

Checks formatting, compiles and runs all tests against a real PostgreSQL started by Testcontainers.
Fix formatting with `./mvnw -pl services/wallet-core spotless:apply`.

## Run

Whole platform:

```bash
docker compose up --build
curl localhost:8080/actuator/health
```

Both ways use the development profile with example credentials only, create an administrator
account `admin@example.com` / `admin-dev-example`, and serve the API reference at
<http://localhost:8080/swagger-ui.html>.

For development, run PostgreSQL in Docker and the service from the sources:

```bash
docker compose up -d postgres
./mvnw -pl services/wallet-core spring-boot:run
```

## Simulate the payment provider

Cash-ins arrive as webhooks signed by the payment provider. `scripts/provider.sh` plays that role
(it needs `curl` and `openssl`) and uses the development example secret by default:

```bash
scripts/provider.sh cash-in 7310582940 ARS 1500.50          # account number, currency, amount
scripts/provider.sh cash-in 7310582940 ARS 1500.50 PRV-001  # with an explicit provider reference
```

Sending the same reference again returns the original result without crediting the wallet twice.
`MYPOCKET_URL` and `MYPOCKET_PROVIDER_WEBHOOK_SECRET` override the target and the secret.

## License

[MIT](LICENSE)
