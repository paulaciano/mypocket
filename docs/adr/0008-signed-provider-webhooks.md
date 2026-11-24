# ADR-0008: HMAC-signed webhooks from the payment provider

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

Cash-ins and withdrawal settlements come from an external payment provider (simulated). Anyone who
can call these endpoints can create money, so their origin must be verified. The provider also
retries deliveries.

## Decision

- Each webhook carries `X-Provider-Timestamp` and `X-Provider-Signature`, an HMAC-SHA256 of
  `timestamp + "." + raw body` with a secret shared with the provider.
- The signature is verified in constant time over the raw body before parsing; requests outside a
  5-minute window are rejected.
- Retries are handled by deduplication on the provider's reference (CI2) and by settlement rules
  (CO8), not by `Idempotency-Key`.
- A script in `scripts/` plays the provider role by signing and sending webhooks.

## Alternatives considered

- **Mutual TLS:** strong, but heavy to set up locally and not what most payment providers offer.
- **Static API key header:** simple, but a leaked key allows forging any request and there is no
  integrity check of the body.
- **A separate fake-provider service:** more realistic, but the signed script covers every flow.

## Consequences

- The shared secret must be configured on both sides; it is a secret (S10).
- The raw request body must be preserved for signature verification.
