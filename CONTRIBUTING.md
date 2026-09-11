# Contributing

We welcome contributions from the community. This repository follows the
patterns established by cloud-itonami ISCO actor projects.

## Code Standards

- All source code must be `.cljc` (portable, no JVM-only constructs).
- Follow the existing structure: `src/maritime/` namespace, tests in
  `test/maritime/`.
- Use `langgraph.graph` for state machine definitions.
- All proposals must be `:effect :propose` (advisory, not actuation).

## Testing

Run the test suite before submitting a pull request:

```bash
kbb -M:test
```

## Scope Boundaries

**Never add proposals or capabilities that:**

- Make actual navigation decisions or command course/heading changes
- Resolve collision-avoidance scenarios
- Exercise command authority

These remain the master's and deck officer's exclusive human responsibility at sea.
