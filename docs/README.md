# Documentation Map

This directory contains the project design, runbooks, product requirements, and historical research notes.

## Start Here

- [../README.md](../README.md): repository overview, quick start, and top-level policies
- [../CONTRIBUTING.md](../CONTRIBUTING.md): contribution workflow and local verification
- [../SUPPORT.md](../SUPPORT.md): how to ask for help or report problems
- [../SECURITY.md](../SECURITY.md): private vulnerability reporting policy
- [../GOVERNANCE.md](../GOVERNANCE.md): maintainer-led decision process and escalation paths
- [../CHANGELOG.md](../CHANGELOG.md): release notes and notable changes

## Setup and Daily Development

- [guides/setup.md](guides/setup.md): local environment setup and supported startup flows
- [runbook/local-dev.md](runbook/local-dev.md): supported local modes, runtime config outputs, and troubleshooting
- [guides/contributing.md](guides/contributing.md): lightweight contribution workflow
- [guides/branching-strategy.md](guides/branching-strategy.md): branch and PR conventions
- [runbook/release.md](runbook/release.md): maintainer release checklist
- [runbook/triage.md](runbook/triage.md): issue, PR, and label triage policy
- [runbook/labels.md](runbook/labels.md): label taxonomy and PR auto-labeling rules

## Architecture

- [architecture/system-overview.md](architecture/system-overview.md): services, boundaries, and runtime topology
- [architecture/api-design.md](architecture/api-design.md): REST/gRPC contract guidance
- [architecture/data-model.md](architecture/data-model.md): key entities and storage model
- [architecture/event-driven.md](architecture/event-driven.md): messaging and event flow
- [architecture/offline-strategy.md](architecture/offline-strategy.md): offline sync design

## Requirements and Planning

- [requirements/overview.md](requirements/overview.md): product scope overview
- [requirements/functional/](requirements/functional/): functional requirements by domain
- [requirements/non-functional/](requirements/non-functional/): performance, scalability, security requirements
- [plans/roadmap.md](plans/roadmap.md): current roadmap and milestone plan

## ADRs

- [adr/001-monorepo.md](adr/001-monorepo.md)
- [adr/002-bff-pattern.md](adr/002-bff-pattern.md)
- [adr/003-offline-strategy.md](adr/003-offline-strategy.md)
- [adr/004-multi-tenant.md](adr/004-multi-tenant.md)
- [adr/005-money-as-bigint.md](adr/005-money-as-bigint.md)

## Historical Research and Audits

- [research/](research/): external research and implementation notes
- [plans/test-workflow-review.md](plans/test-workflow-review.md): point-in-time audit of the old testing workflow; treat this as historical context, not current status
