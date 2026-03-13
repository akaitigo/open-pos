# Contributing to open-pos

Thank you for your interest in contributing to open-pos! This guide will help you get started.

## Development Setup

### Prerequisites

- Java 21 (GraalVM CE recommended)
- Kotlin 2.3+
- Node.js 22+ with pnpm
- Docker & Docker Compose
- buf CLI (for Protocol Buffers)
- Gradle 9+

### Getting Started

```bash
# Clone the repository
git clone https://github.com/akaitigo/open-pos.git
cd open-pos

# Install frontend dependencies
pnpm install

# Recommended local verification path
make local-demo
pnpm dev:admin
pnpm dev:pos
```

### Running Locally

```bash
# Host-run core backend (recommended for day-to-day work)
make local-demo

# Containerized core backend (useful for release verification)
make docker-demo

# Restart only the supported backend mode you are using
make local-up-fast
make docker-up-core
```

## How to Contribute

### Reporting Issues

- Check [existing issues](https://github.com/akaitigo/open-pos/issues) to avoid duplicates.
- Use the appropriate issue template.
- Include reproduction steps and environment details.

### Submitting Changes

1. **Fork** the repository and create a feature branch:
   ```bash
   git checkout -b feature/#<issue-number>-short-description
   ```

2. **Make your changes** following the coding conventions below.

3. **Run tests** before submitting:
   ```bash
   make test          # Backend tests
   make test-apps     # Frontend tests
   make local-smoke   # Seeded API smoke test
   make lint          # Lint supported local targets (proto, frontend)
   ```

4. **Create a Pull Request** with:
   - A clear title and description
   - `Closes #<issue-number>` in the description
   - Passing CI checks

### Branch Naming

```
feature/#123-add-product-search
fix/#456-fix-price-calculation
```

## Coding Conventions

### Backend (Kotlin / Quarkus)

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- No `!!` (non-null assertions) — use `?.let {}`, `?: throw`, or `requireNotNull()`
- Use `@ApplicationScoped` by default for CDI beans
- All monetary amounts in **sen** (1/100 of yen) as `Long`/`BIGINT`
- All tables must have `organization_id` for multi-tenant isolation

### Frontend (TypeScript / React)

- No `any` type — use `unknown` + type guards or generics
- Function components + hooks only (no class components)
- Use shared types from `@shared-types/openpos`
- Format amounts with `formatMoney()` from shared-types

### Protocol Buffers

- Run `buf lint` and `buf format -w` after editing `.proto` files
- Never reuse field numbers — mark removed fields as `reserved`
- Comment all messages, fields, and RPCs

### Testing

All features must include tests:

| Level | Backend | Frontend |
|-------|---------|----------|
| Unit | JUnit 5 + `@InjectMock` | Vitest + RTL |
| Functional | `@QuarkusTest` | Vitest + RTL |
| Integration | `@QuarkusTest` + Testcontainers | — |
| E2E | Playwright | Playwright |

- Follow AAA pattern (Arrange-Act-Assert)
- New code must have 80%+ line coverage
- E2E tests use `data-testid` and Page Object Model

## Architecture

### Layer Dependencies (top-down only)

```
Proto -> Config/Filter -> Entity -> Repository -> Service -> gRPC Handler
```

- Entities must NOT reference Services
- gRPC Handlers must NOT call Repositories directly
- Services communicate via RabbitMQ events only (no direct service-to-service calls)

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
