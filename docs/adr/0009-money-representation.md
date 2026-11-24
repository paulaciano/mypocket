# ADR-0009: Money representation

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

Binary floating point cannot represent most decimal amounts exactly. Amounts cross three boundaries:
Java code, the database and JSON clients, some of which parse numbers as doubles.

## Decision

- In Java, amounts are `BigDecimal` with scale 2, wrapped in a `Money` value type with its currency.
  Amounts with more than 2 decimals are rejected, never rounded (M1).
- In PostgreSQL, amounts are `NUMERIC(19,2)`.
- In JSON, amounts are **strings** (`"1500.50"`).

## Alternatives considered

- **Integer minor units (`long` cents):** exact and fast, but every boundary must convert, and an API
  in cents is easy to misread by a factor of 100.
- **JSON numbers:** natural, but JavaScript and many JSON libraries would parse them as doubles.

## Consequences

- One `Money` type centralizes validation and arithmetic.
- Clients must send and parse amounts as strings.
