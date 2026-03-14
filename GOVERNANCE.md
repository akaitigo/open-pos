# Governance

`open-pos` is a maintainer-led OSS project.

## Principles

- Decisions happen in public GitHub artifacts whenever practical: issues, pull requests, discussions, ADRs, and release notes.
- Small changes should stay lightweight. The process should scale with risk, not with ceremony.
- Significant product, architecture, and security decisions should leave a durable written record.

## Roles

### Maintainers

Maintainers are listed in [MAINTAINERS.md](MAINTAINERS.md). They are responsible for:

- triage and roadmap curation
- reviewing and merging pull requests
- release management
- CI, dependency, and security hygiene
- documenting important decisions

### Contributors

Contributors can propose code, docs, design, and process improvements through issues, pull requests, and discussions.

### Reporters and users

Users can ask questions in Discussions, file bugs, and provide feedback on supported development flows.

## Decision Process

### Routine changes

Routine fixes and scoped improvements are handled through the normal pull request flow. A maintainer merge is the decision point.

### Significant changes

Use a linked GitHub issue or discussion when a change affects one or more of the following:

- service boundaries or public API contracts
- offline or multi-tenant behavior
- security posture or reporting policy
- release process or contributor workflow

If the decision is architectural or long-lived, capture it in `docs/adr/` or another durable project document before or with the merge.

### Security and release exceptions

Maintainers may act immediately on security or release-blocking issues. When that happens, the public rationale should be backfilled afterward in the relevant pull request, advisory, changelog entry, or ADR.

## Maintainer Changes

- New maintainers are added by the current maintainer set.
- Maintainer additions or removals should be reflected in [MAINTAINERS.md](MAINTAINERS.md).
- If the project becomes inactive, the current maintainer set may archive the repository rather than leave support expectations ambiguous.

## Escalation Path

- Setup and usage questions: [SUPPORT.md](SUPPORT.md) and GitHub Discussions
- Bugs and scoped feature requests: GitHub Issues
- Security vulnerabilities: [SECURITY.md](SECURITY.md)
