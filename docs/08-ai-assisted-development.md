# 8. AI-Assisted Development

MyPocket is built by a human engineer working with an AI coding assistant (Claude). This page
describes how that collaboration works and who is accountable for what.

## Roles

| The engineer | The AI assistant |
|---|---|
| Owns scope, product, architecture and assumptions. | Reads the code and documentation and detects what is unresolved. |
| Reviews and merges every pull request. | Proposes options with a recommendation. |
| Decides when a version is tagged. | Implements, tests and documents the agreed solution. |
| | Never merges, never pushes to `main` and asks before any hard-to-revert action. |

## Principles

- **Ask before resolving.** When something is ambiguous or has more than one reasonable answer, the
  assistant asks first, with a concrete proposal. Questions are grouped so they can be answered
  together. Conventional choices are made directly and reported.
- **Solve current requirements.** No complexity for hypothetical future needs. When a simpler option
  covers the requirement, it is the recommendation.
- **Stop on contradictions.** If implementation reveals something that contradicts what was agreed,
  work stops until the engineer decides.

## Cycle per phase

1. **Assumption review:** re-read the rules and bring every open point, each with a proposal.
2. **Plan:** small pull requests, one per functionality, in dependency order.
3. **Implementation with tests:** each business rule has its test; each bug fix has a test that
   reproduces the bug.
4. **Verification:** full build, no skipped tests, green CI and, when it matters, running the
   platform and trying it for real.
5. **Consistency:** code and documentation that disagree are reconciled in the same pull request.
6. **One pull request at a time:** the next one starts after the previous one is merged.
7. **Phase close:** CHANGELOG, README and version; the tag is created after the merge.

## Independent reviews

When an important phase closes, a fresh AI agent that has none of the conversation's context reviews
the whole project: code, security, concurrency, and whether the documentation matches the code. Each
finding is verified before it is reported. The engineer decides what is fixed now and what is
recorded for later.

## Traceability

- Design decisions are recorded as ADRs before or together with the code that implements them.
- Commits authored with AI assistance carry a `Co-Authored-By` trailer.
- Business rules have stable ids referenced by tests, so the reviewer can check that every rule is
  covered.
