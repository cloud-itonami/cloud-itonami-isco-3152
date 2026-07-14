# Governance

This repository implements an ISCO-08 occupation blueprint for maritime
deck coordination (ISCO unit-group 3152).

## Decision-Making

Major decisions regarding the actor's scope, governor invariants, and
escalation rules are made through the cloud-itonami ADR (Architecture Decision
Record) process.

Changes to:
- Hard invariants in the governor
- Core proposal operations
- Governor escalation thresholds

...require community discussion and documented rationale (ADR or GitHub issue).

## Scope Boundaries

This actor is **strictly administrative coordination** at sea:

- **In scope:** voyage planning drafts, position reporting, port logistics, hazard surfacing
- **Out of scope:** actual navigation decisions, course commands, collision avoidance, command authority

The boundary is absolute. Any proposal to extend beyond administrative coordination must go through the
ADR process and receive explicit community and cloud-itonami maintainer agreement.

## Invariants

The three hard invariants (vessel registration, no navigation commands, effect-is-propose) are
immutable unless through a major ADR. Any pull request that would soften these invariants will be rejected.
