# cloud-itonami-isco-3152

Open Occupation Blueprint for **ISCO-08 3152**: Ships' Deck Officers and Pilots.

This repository designs a maritime deck coordination actor for ship operations: voyage planning, position reporting, navigational hazard escalation, and port-arrival logistics coordination under a governor-gated actor, so a ship's deck officer keeps structured operational records and coordinates pre-voyage actions instead of ad-hoc logs.

**Maturity: `:implemented`.** `src/maritime/` implements the
`MaritimeDeckActor` as a `langgraph.graph/state-graph`
(`maritime.actor`) wired to a `Maritime Advisor` (`maritime.advisor`)
and an independent `MaritimeDeckGovernor` (`maritime.governor`),
following the itonami actor pattern (ADR-2607011000): `:intake -> :advise
-> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 7 tests / 17 assertions
green (`clojure -M:test`).

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

Proposal ops (closed allowlist, all `:effect :propose`):

- `:log-position-report` — routine position/status reporting (latitude, longitude, course, speed)
- `:draft-voyage-plan` — voyage-plan draft for the officer's own review and filing (route, timing, hazards noted)
- `:flag-navigational-hazard` — surface a reported navigational hazard (weather, traffic, obstacles), ALWAYS escalates
- `:coordinate-port-arrival` — port-arrival logistics coordination (ETA, berth, cargo notes)

**HARD invariants** (always `:hold`, never overridable):

1. **vessel-registered** — the vessel must be registered before any operation.
2. **no-navigation-command** — proposals must NEVER contain actual course/heading commands,
   collision-avoidance decisions, or command authority actions (`:course-command`, `:heading-command`,
   `:collision-avoidance`, `:master-command`, `:command-authority`).
   Only administrative coordination (planning, reporting, hazard surfacing) are permitted.
3. **effect-is-propose** — `:effect` must be `:propose` only (the governor
   never directly executes operations).

**ESCALATION invariants** (always human sign-off):

4. **`:flag-navigational-hazard`** always escalates, regardless of confidence.
5. **Low confidence** (< 0.6) escalates to human review.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3152`). Required capabilities:

- :audit-ledger
- :forms
- :identity

See `docs/business-model.md` and `docs/operator-guide.md`.

## License

AGPL-3.0-or-later.
