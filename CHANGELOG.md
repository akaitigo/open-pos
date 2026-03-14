# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project follows Semantic Versioning.

## [Unreleased]

### Added

- Added `make doctor`, `make verify`, and `make verify-full` as supported local quality gates.
- Added maintainer-facing release guidance in [docs/runbook/release.md](docs/runbook/release.md).
- Added a documentation index in [docs/README.md](docs/README.md).
- Added [SUPPORT.md](SUPPORT.md) and [MAINTAINERS.md](MAINTAINERS.md) for public OSS operations.
- Added GitHub issue contact links and release note categorization.

### Changed

- Tightened contributor setup docs around actual required tools (`curl`, `jq`, `bc`) and the supported verification flow.
- Added clearer prerequisite checks to local helper scripts.
- Fixed the Docker-based startup flow to wait on `hydra` health instead of the one-shot `hydra-migrate` container.

## [0.1.0] - Initial development series

### Added

- Initial public repository structure, local demo flow, CI, security scanning, and core architecture docs.
