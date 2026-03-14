# open-pos

> Universal Point of Sale System — 汎用POSシステム

[![CI](https://github.com/akaitigo/open-pos/actions/workflows/ci.yml/badge.svg)](https://github.com/akaitigo/open-pos/actions/workflows/ci.yml)
[![Security](https://github.com/akaitigo/open-pos/actions/workflows/security.yml/badge.svg)](https://github.com/akaitigo/open-pos/actions/workflows/security.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A multi-tenant, offline-capable POS (Point of Sale) system built with microservices architecture.

> [!WARNING]
> **This project is in early development (v0.1.x).** Authentication and authorization are NOT yet implemented on REST API endpoints. **Do not deploy to production or expose to the public internet.** See [SECURITY.md](SECURITY.md) for details.

## Project Status

- **Stage**: Public alpha for local development and architecture exploration
- **Supported demo flows**: `make local-demo` and `make docker-demo`
- **Quality gates on `main`**: CI, dependency audit, secret scanning, Playwright E2E
- **Not supported yet**: production deployment, internet exposure, or hard security guarantees

## Features

- **Multi-tenant**: Organization-level data isolation via Hibernate Filters
- **Offline-capable**: POS terminal works offline with IndexedDB (Dexie.js), syncs when back online
- **Microservices**: 6 backend services communicating via gRPC and RabbitMQ
- **Modern frontend**: React 19 + TypeScript + Tailwind CSS + shadcn/ui

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                    Clients                            │
│  ┌─────────────┐              ┌──────────────────┐   │
│  │ POS Terminal│              │ Admin Dashboard   │   │
│  │ (React PWA) │              │ (React SPA)       │   │
│  └──────┬──────┘              └────────┬─────────┘   │
└─────────┼──────────────────────────────┼─────────────┘
          │           REST               │
     ┌────▼─────────────────────────────▼────┐
     │           api-gateway (BFF)            │
     │        Quarkus REST + Auth             │
     └──┬────┬────┬────┬────┬────┬───────────┘
        │gRPC│    │    │    │    │
   ┌────▼┐ ┌─▼──┐│┌───▼┐┌──▼─┐┌▼────────┐
   │ pos ││prod-│││inv- ││stor││analytics │
   │ svc ││uct  │││ent- ││e   ││service   │
   └──┬──┘└──┬──┘│└──┬──┘└──┬─┘└────┬────┘
      │      │   │   │      │       │
      └──────┴───┴───┴──────┴───────┘
              │           │
     ┌────────▼──┐  ┌─────▼──────┐
     │ PostgreSQL│  │  RabbitMQ  │
     │  + Redis  │  │  (Events)  │
     └───────────┘  └────────────┘
```

| Service | Technology | Role |
|---------|-----------|------|
| api-gateway | Quarkus (REST) | BFF, auth, tenant injection |
| pos-service | Quarkus (gRPC) | Transactions, payments, receipts |
| product-service | Quarkus (gRPC) | Products, categories, tax rates |
| inventory-service | Quarkus (gRPC) | Inventory, stock movements |
| analytics-service | Quarkus (gRPC) | Sales analytics |
| store-service | Quarkus (gRPC) | Stores, staff management |
| pos-terminal | React PWA | POS terminal (tablet-optimized) |
| admin-dashboard | React SPA | Admin panel (desktop) |

## Tech Stack

**Backend**: Kotlin 2.3 / Quarkus 3.32 / GraalVM CE 21 / Gradle 9
**Frontend**: React 19 / TypeScript / Vite 6 / Tailwind CSS + shadcn/ui
**Database**: PostgreSQL 17 (schema isolation, Flyway migrations)
**Cache**: Redis 7 (Lettuce, cache-aside pattern)
**Messaging**: RabbitMQ 4 (SmallRye Reactive Messaging)
**Auth**: ORY Hydra v2.2 (OIDC/PKCE)
**API**: gRPC (proto3 + buf toolchain)

## Getting Started

### Prerequisites

- Java 21 (GraalVM CE recommended)
- Node.js 22+ with pnpm
- Docker & Docker Compose
- buf CLI
- curl
- jq
- bc

### Quick Start

```bash
make doctor
pnpm install

# Fastest supported local path: infra in Docker, core backend on the host
make local-demo
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173

# Containerized alternative: core backend in Docker too
make docker-demo
pnpm dev:admin
pnpm dev:pos
```

`make local-demo` / `make docker-demo` writes `apps/*/public/demo-config.json`, so reloading the browser is enough to pick up the latest seeded organization, store, and terminal IDs.

### Development

```bash
# Start with dev tools (pgAdmin, Redis Commander)
make up-dev

# Run backend service in dev mode
make dev-product   # product-service on quarkusDev
make dev-gateway   # api-gateway on quarkusDev

# Run frontend dev servers
pnpm dev:pos       # POS terminal → http://localhost:5173
pnpm dev:admin     # Admin dashboard → http://localhost:5174

# Run tests
make test          # Backend tests
make test-apps     # Frontend unit/functional tests
make verify        # typecheck + lint + backend/frontend unit-functional tests
pnpm e2e:install   # Install Playwright browser once
make verify-full   # verify + docker-demo + Playwright E2E

# Lint supported local targets
make lint          # Proto + Frontend
```

`pnpm test` runs unit/functional tests for `packages/` and `apps/`. E2E is opt-in via `pnpm test:e2e` so routine local verification does not depend on Playwright browsers.

### Supported Demo Paths

For day-to-day development, run infra in Docker and the core backend services on the host:

```bash
make local-demo  # starts infra + core backend + seed data + runtime demo-config files
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

If you want the same core stack containerized:

```bash
make docker-demo # builds the core images, starts them, seeds data, and verifies the API
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

If you only need to restart the host backend processes after a code change:

```bash
make local-up-fast
make local-down
make local-smoke
make docker-up-core
make docker-down-core
make docker-build-core
```

`make docker-up-core` stops the locally managed host backend first, so switching between the two supported modes is predictable.

See [docs/guides/setup.md](docs/guides/setup.md) for detailed setup instructions.

## Project Structure

```
open-pos/
├── proto/              # Protobuf definitions (buf workspace)
├── services/           # Quarkus microservices
│   ├── api-gateway/
│   ├── pos-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── analytics-service/
│   └── store-service/
├── apps/               # React frontends
│   ├── pos-terminal/
│   └── admin-dashboard/
├── packages/           # Shared TypeScript packages
│   └── shared-types/
├── e2e/                # Playwright E2E tests
├── infra/              # Docker Compose + init scripts
└── docs/               # Architecture, design, guides
```

## Documentation Map

- Start here: [docs/README.md](docs/README.md)
- Setup and local workflow: [docs/guides/setup.md](docs/guides/setup.md), [docs/runbook/local-dev.md](docs/runbook/local-dev.md)
- Architecture: [docs/architecture/system-overview.md](docs/architecture/system-overview.md), [docs/architecture/api-design.md](docs/architecture/api-design.md), [docs/architecture/data-model.md](docs/architecture/data-model.md)
- Requirements and roadmap: [docs/requirements/overview.md](docs/requirements/overview.md), [docs/plans/roadmap.md](docs/plans/roadmap.md)
- Decision records: [docs/adr/001-monorepo.md](docs/adr/001-monorepo.md)

## Support

- Setup help and usage guidance: [SUPPORT.md](SUPPORT.md)
- Security reporting: [SECURITY.md](SECURITY.md)
- Maintainer expectations: [MAINTAINERS.md](MAINTAINERS.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines and how to submit changes. Start with `make doctor`, then use `make verify` before opening a pull request.

## Security

See [SECURITY.md](SECURITY.md) for our security policy and how to report vulnerabilities.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
