# Triage Runbook

This runbook describes the minimum maintainer workflow for incoming issues and pull requests.

## Goals

- keep user-facing reports actionable
- route questions to the right channel
- preserve a predictable label taxonomy
- make priority visible without over-processing

## Issue Intake

1. Confirm the report belongs in the public tracker.
2. Redirect setup and usage questions to Discussions when they are not actionable bugs.
3. Reject public security reports and point reporters to [../../SECURITY.md](../../SECURITY.md).
4. Ask for missing reproduction details before attempting a fix.

## Baseline Labels

Use at least one label from each relevant group:

- `type:*` for the work shape, for example `type:bug`, `type:feature`, `type:docs`, `type:chore`
- `svc:*`, `app:*`, `pkg:*`, `infra`, or `proto` for the affected surface
- `area:*` when the problem maps to a product or technical concern
- `P0:*` to `P3:*` when priority needs to be made explicit

See [labels.md](labels.md) for the current label taxonomy and PR label automation rules.

## Priority Guidance

- `P0:critical`: security issue, data corruption, or a release-blocking regression
- `P1:high`: broken supported flow, major contributor friction, or high-confidence user pain
- `P2:medium`: important but not release-blocking
- `P3:low`: backlog, polish, or speculative work

## Pull Request Triage

Before merge, confirm that:

- the title and labels describe the change
- auto-applied surface labels still match the final diff
- the linked issue or rationale is clear for non-trivial work
- the author ran the relevant local checks
- required GitHub checks are green
- changelog, docs, ADRs, or follow-up issues exist when the change affects long-lived behavior

## Closing Rules

- Close as duplicate when an existing issue already tracks the same problem.
- Close as not planned when the request does not fit the current roadmap or support boundary.
- Close as answered when the thread is support-only and the outcome is documented elsewhere.
- Prefer linking the canonical issue, ADR, discussion, or docs page when closing.
