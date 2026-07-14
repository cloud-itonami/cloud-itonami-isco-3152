# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this repository, please email
the cloud-itonami maintainers at the address listed in the GitHub repository
settings. Do not file a public GitHub issue for security vulnerabilities.

Include:
- A clear description of the vulnerability
- Steps to reproduce (if applicable)
- Potential impact
- Any proposed fix or mitigation

## Scope

Security concerns in this repository include:

1. **Governor bypass:** Any proposal that reaches `:commit` without proper
   vessel/equipment registration checks, or any proposal containing engine
   control commands that is not held or escalated.

2. **Audit trail tampering:** Any modification to the append-only ledger
   (which should be write-only).

3. **Escalation bypass:** Any mechanical fault flag that does not escalate
   to human sign-off.

## Principles

- **Defense in depth:** Multiple layers of validation (proposal validation,
  governor check, escalation logic).
- **No silent failures:** Violations are explicit (`:violations`, `:hard?`,
  `:escalate?`).
- **Append-only audit trail:** Every decision is logged; past logs cannot be
  altered.

## Licensing & Compliance

This project is AGPL-3.0-or-later. Derivative works must remain open source
and include security improvements.
