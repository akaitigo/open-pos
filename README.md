# open-pos

> Universal Point of Sale System — 汎用POSシステム

[![CI](https://github.com/akaitigo/open-pos/actions/workflows/ci.yml/badge.svg)](https://github.com/akaitigo/open-pos/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A multi-tenant, offline-capable POS (Point of Sale) system built with microservices architecture.

> [!WARNING]
> **This project is in early development (v0.1.x).** Authentication and authorization are NOT yet implemented on REST API endpoints. **Do not deploy to production or expose to the public internet.** See [SECURITY.md](SECURITY.md) for details.

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

### Quick Start

```bash
# Start infrastructure (PostgreSQL, Redis, RabbitMQ, Hydra)
make up

# Generate gRPC code from proto definitions
make proto

# Build backend
make build

# Install frontend dependencies & build
pnpm install
make build-apps
```

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
make test-apps     # Frontend tests

# Lint everything
make lint          # Proto + Backend + Frontend
```

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

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines and how to submit changes.

## Security

See [SECURITY.md](SECURITY.md) for our security policy and how to report vulnerabilities.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
