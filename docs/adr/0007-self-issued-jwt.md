# ADR-0007: Self-issued JWT for authentication

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

Every customer operation needs to know who is calling, and customers may only act on their own
wallets. There is a single API service and no external identity provider.

## Decision

- wallet-core issues its own access tokens at login: JWT signed with **HS256** and a secret from the
  environment, valid for **15 minutes**, with the user id as subject and the role as a claim.
- Requests are validated with Spring Security's OAuth2 resource server support.
- Passwords are hashed with BCrypt.
- No refresh tokens: when a token expires the client logs in again.

## Alternatives considered

- **External identity provider (e.g. Keycloak):** standard flows, but adds a heavy component and its
  configuration for a single API.
- **Asymmetric signature (RS256/ES256):** needed when other services verify tokens without sharing a
  secret; today only wallet-core issues and verifies them.
- **Server-side sessions:** stateful and less natural for an API-only product.

## Consequences

- Tokens cannot be revoked before expiry; the short lifetime limits the exposure.
- If another service ever needs to verify tokens, a new ADR moves to asymmetric keys.
