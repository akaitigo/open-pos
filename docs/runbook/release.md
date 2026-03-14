# Release Runbook

This runbook describes the minimum maintainer checklist for cutting an `open-pos` release.

## Release Policy

- `open-pos` follows SemVer.
- Until `1.0.0`, releases are expected to be `0.x.y` and may still include breaking changes when documented clearly.
- Every release must have a Git tag, GitHub Release entry, and a corresponding `CHANGELOG.md` update.

## Pre-flight Checklist

1. Start from a clean `main` branch.
2. Review the scope and unresolved risks for the release.
3. Update [../../CHANGELOG.md](../../CHANGELOG.md).
4. Run the supported local checks:

```bash
make doctor
pnpm install
make verify
```

5. If the release changes demo flows, frontend apps, seed data, auth, or API contracts, also run:

```bash
pnpm e2e:install
make verify-full
```

6. Confirm the latest GitHub Actions runs on `main` are green:
   - `CI`
   - `Security`

## Tag and Release

```bash
git checkout main
git pull --ff-only
git tag -a v0.x.y -m "v0.x.y"
git push origin v0.x.y
gh release create v0.x.y --generate-notes
```

`gh release create --generate-notes` uses [../../.github/release.yml](../../.github/release.yml) for category grouping.

## Post-release

1. Verify the GitHub Release notes look correct.
2. Confirm the tag points at the expected commit.
3. Move any unfinished issues to the next milestone.
4. Start a fresh `Unreleased` section in [../../CHANGELOG.md](../../CHANGELOG.md) if needed.

## Hotfixes

- Branch from the release commit or current `main`, depending on impact.
- Run the same checklist as a normal release.
- Prefer a follow-up patch release instead of force-updating an existing tag.
