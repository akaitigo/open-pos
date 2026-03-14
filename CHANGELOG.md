# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project follows Semantic Versioning.

## [Unreleased]

### Added

- Added `GOVERNANCE.md` and `docs/runbook/triage.md` to make maintainer decision-making and issue triage explicit.
- Added PR component auto-labeling and a label taxonomy runbook for maintainer triage consistency.
- Added `make doctor`, `make verify`, and `make verify-full` as supported local quality gates.
- Added developer Makefile helpers for PostgreSQL backup/restore, demo reset, `pos-service` log tailing, and grpcurl-based gRPC health checks.
- Added maintainer-facing release guidance in [docs/runbook/release.md](docs/runbook/release.md).
- Added a documentation index in [docs/README.md](docs/README.md).
- Added [SUPPORT.md](SUPPORT.md) and [MAINTAINERS.md](MAINTAINERS.md) for public OSS operations.
- Added GitHub issue contact links and release note categorization.
- Added `.mise.toml`, `CITATION.cff`, and Release Drafter configuration for reproducible setup and release metadata.
- Added an idempotent demo seed dataset with two stores, per-store terminals/staff, inventory, and sample transactions.

### Changed

- Added GitHub CodeQL scanning and tightened the documented security reporting path to GitHub private advisories only.
- Tightened contributor setup docs around actual required tools (`curl`, `jq`, `bc`) and the supported verification flow.
- Rewrote the local development runbook to match the supported `make local-demo` / `make docker-demo` flows, generated runtime config files, and current troubleshooting steps.
- Added clearer prerequisite checks to local helper scripts.
- Expanded the documented local development command set to include `make docker-build`, `make reset`, `make db-backup`, `make db-restore`, `make logs-pos`, and `make grpc-test`.
- Expanded the supported local demo path to include `inventory-service` so seeded inventory and transaction history are available immediately.
- Fixed the Docker-based startup flow to wait on `hydra` health instead of the one-shot `hydra-migrate` container.
- Shifted open-ended setup and usage questions toward GitHub Discussions, keeping Issues focused on bugs and feature work.
- Enabled GitHub Discussions, branch protection on `main`, auto-merge support, and GitHub-native secret scanning / push protection.

## [0.1.0] - Initial development series

### Added

- Initial public repository structure, local demo flow, CI, security scanning, and core architecture docs.
