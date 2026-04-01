# Documentation Map

This directory contains the project design, runbooks, product requirements, and historical research notes.

## Start Here

- [../README.md](../README.md): repository overview and quick start

## Setup and Daily Development

- [guides/setup.md](guides/setup.md): local environment setup and supported startup flows
- [guides/local-development.md](guides/local-development.md): quarkusDev / local-demo / docker-demo モードの比較と使い分け
- [guides/demo-assets.md](guides/demo-assets.md): regenerate README screenshots and GIFs from the supported local demo flow
- [runbook/local-dev.md](runbook/local-dev.md): supported local modes, runtime config outputs, and troubleshooting
- [guides/contributing.md](guides/contributing.md): ブランチ戦略とコミット規約
- [guides/branching-strategy.md](guides/branching-strategy.md): branch and PR conventions
- [guides/performance-testing.md](guides/performance-testing.md): パフォーマンステスト計画（k6、SLO検証、CI統合）
- [runbook/release.md](runbook/release.md): release checklist
- [runbook/labels.md](runbook/labels.md): label taxonomy and PR auto-labeling rules

## Architecture

- [architecture/system-overview.md](architecture/system-overview.md): services, boundaries, and runtime topology
- [architecture/api-design.md](architecture/api-design.md): REST/gRPC contract guidance
- [architecture/data-model.md](architecture/data-model.md): key entities and storage model
- [architecture/event-driven.md](architecture/event-driven.md): messaging and event flow
- [architecture/offline-strategy.md](architecture/offline-strategy.md): offline sync design
- [architecture/sequence-diagrams.md](architecture/sequence-diagrams.md): key flow sequence diagrams (POS checkout, offline sync, PIN auth)

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

## Deployment and Operations

- [deployment-guide.md](deployment-guide.md): production K8s deployment, Flyway migrations, rollback procedures
- [api-reference.md](api-reference.md): REST API endpoints, gRPC services, response formats

## Compliance

- [compliance/invoice-compliance.md](compliance/invoice-compliance.md): Japanese Qualified Invoice System (インボイス制度) requirements
- [compliance/e-books-act.md](compliance/e-books-act.md): 電子帳簿保存法 対応ガイド
- [compliance/pci-dss.md](compliance/pci-dss.md): PCI DSS 対応ガイド

## Historical Research and Audits

- [research/](research/): external research and implementation notes
- [plans/test-workflow-review.md](plans/test-workflow-review.md): point-in-time audit of the old testing workflow; treat this as historical context, not current status
