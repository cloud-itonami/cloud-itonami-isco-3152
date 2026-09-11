# cloud-itonami-isco-3152

Open Occupation Blueprint for **ISCO-08 3152**: Ships' Deck Officers and Pilots.

This repository designs a maritime deck coordination actor for ship operations: voyage planning, position reporting, navigational hazard escalation, and port-arrival logistics coordination under a governor-gated actor, so a ship's deck officer keeps structured operational records and coordinates pre-voyage actions instead of ad-hoc logs.

**Maturity: `:implemented`.** `src/maritime/` implements the
`MaritimeDeckActor` as a `langgraph.graph/state-graph`
(`maritime.actor`) wired to a `Maritime Advisor` (`maritime.advisor`)
and an independent `MaritimeDeckGovernor` (`maritime.governor`),
following the itonami actor pattern (ADR-2607011000): `:intake -> :advise
-> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. The closed allowlist of
permitted operations is `maritime.operation/catalogue`. 19 tests / 119
assertions green (`kbb -M:test`).

## What This Actor Does NOT Do

**This actor coordinates voyage planning, position reporting, and port logistics only.
It does NOT:**

- **Make actual navigation decisions or command course changes.** Route planning, heading, course correction,
  and navigation orders remain the master's sole responsibility and the deck officer's human judgment at sea.
  The actor proposes voyage plans and logs readings; humans execute navigation commands.
- **Make collision-avoidance decisions.** Collision detection and avoidance are the bridge team's
  (master, deck officer, lookout) sole responsibility. The actor can flag navigational hazards for
  human review; it never decides or executes collision responses.
- **Exercise command authority.** The actor advises; the master/deck officer decides.
  No actor proposal supersedes human judgment at sea.

## Scope & HARD invariants

Proposal ops — the closed allowlist, all `:effect :propose`. The list lives in
`maritime.operation/catalogue`; this table is a reading of it, and
`permitted-ops-matches-the-catalogue` keeps the two from drifting apart:

| op | required payload | escalates? |
|---|---|---|
| `:log-position-report` — routine position/status reporting | `:latitude` `:longitude` | no |
| `:draft-voyage-plan` — voyage-plan draft for the officer's own review and filing | `:departure-port` `:destination-port` | no |
| `:flag-navigational-hazard` — surface a reported navigational hazard (weather, traffic, obstacles) | `:hazard-description` | **always** |
| `:coordinate-port-arrival` — port-arrival logistics coordination (ETA, berth, cargo) | `:port` | no |

**HARD invariants** (always `:hold`, never overridable):

1. **vessel-registered** — the vessel must be registered before any operation.
2. **operation-permitted** — the op must be in the catalogue above. Anything
   else is refused, whether or not anyone anticipated it. Command authority
   (`:course-command`, `:heading-command`, `:collision-avoidance`,
   `:master-command`, `:command-authority`) is refused with a specific
   explanation naming the human authority it belongs to, but it is refused by
   the same allowlist as an op nobody has ever seen.
3. **operation-complete** — the payload must carry the fields the catalogue
   requires, so a committed record is a record of something.
4. **effect-is-propose** — `:effect` must be `:propose` only (the governor
   never directly executes operations).

**ESCALATION invariants** (always human sign-off):

5. **`:flag-navigational-hazard`** always escalates, regardless of confidence.
   The policy is the catalogue's `:escalates?`, not a second list in the governor.
6. **Low confidence** (< 0.6) escalates to human review.

> Invariant 2 was a deny-list until 2026-09-06: it named five forbidden ops and
> admitted every op it did not name, while this README already claimed a closed
> allowlist. Measured on that build, `:alter-course`, `:override-autopilot`,
> `:issue-helm-order`, `:engine-order` and even `:anything-at-all` all reached
> `:commit` and wrote a record on a registered vessel. The list of orders a
> ship's officer must never delegate is not a list anyone finishes writing, so
> the enumerated list is now the permitted one.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3152`). Required capabilities:

- :audit-ledger
- :forms
- :identity

See `docs/business-model.md` and `docs/operator-guide.md`.

## License

AGPL-3.0-or-later.
