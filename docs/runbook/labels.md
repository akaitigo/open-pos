# Label Taxonomy

This runbook documents the label groups used in `open-pos` and how maintainers should apply them.

## Goals

- keep issue and pull request triage predictable
- make ownership and affected surfaces visible at a glance
- preserve useful release notes and roadmap hygiene

## Label Groups

### Work Shape

Use one `type:*` label to describe the primary shape of the change:

- `type:feature`
- `type:bug`
- `type:docs`
- `type:chore`
- `type:test`
- `type:epic`

These labels are still maintainer-curated. Do not rely on file paths alone to decide them.

### Affected Surface

Use `svc:*`, `app:*`, `pkg:*`, `infra`, or `proto` to show what part of the repository is affected.

| Label | Intended Surface | Auto-applied on PRs |
|-------|------------------|---------------------|
| `svc:api-gateway` | `services/api-gateway/**` | yes |
| `svc:pos` | `services/pos-service/**` | yes |
| `svc:product` | `services/product-service/**` | yes |
| `svc:analytics` | `services/analytics-service/**` | yes |
| `svc:inventory` | `services/inventory-service/**` | yes |
| `svc:store` | `services/store-service/**` | yes |
| `app:pos-terminal` | `apps/pos-terminal/**` | yes |
| `app:admin` | `apps/admin-dashboard/**` | yes |
| `pkg:shared-types` | `packages/shared-types/**` | yes |
| `proto` | `proto/**` | yes |
| `infra` | CI, Compose, scripts, toolchain, root build config | yes |

Automation is defined in [../../.github/labeler.yml](../../.github/labeler.yml) and runs via [../../.github/workflows/labeler.yml](../../.github/workflows/labeler.yml).

If a change is cross-cutting, keep all relevant surface labels instead of forcing a single one.

### Product and Technical Concerns

Use `area:*` labels when the change maps to a domain concern rather than a repository boundary.

Current `area:*` labels include:

- `area:auth`
- `area:payment`
- `area:receipt`
- `area:tax`
- `area:cart`
- `area:a11y`
- `area:sync`
- `area:i18n`
- `area:perf`
- `area:security`
- `area:monitoring`
- `area:dx`
- `area:offline`
- `area:settlement`
- `area:customer`
- `area:report`
- `area:loyalty`

These are maintainer-applied and should reflect the user-facing or architectural concern, not only the file path.

### Priority

Use `P0:*` to `P3:*` when priority should be visible in the tracker:

- `P0:critical`
- `P1:high`
- `P2:medium`
- `P3:low`

Priority labels are not added automatically.

## Maintainer Guidance

For pull requests:

1. Let the PR Labeler apply the obvious surface labels first.
2. Verify the auto-applied labels are still correct for the final diff.
3. Add or adjust `type:*`, `area:*`, and priority labels manually when they matter.
4. Remove misleading labels on cross-cutting or reshaped PRs before merge.

For issues:

- labels are always maintainer-applied
- prefer the smallest useful set of labels
- avoid using labels as a substitute for a clear issue title or description
