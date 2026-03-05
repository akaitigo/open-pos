# Contributing to open-pos

Thank you for your interest in contributing! This guide will help you get started.

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

## Getting Started

### Prerequisites

- Node.js >= 22
- pnpm >= 10
- JDK 21 (GraalVM CE recommended)
- Docker & Docker Compose
- Make
- buf CLI

### Setup

```bash
# Clone the repository
git clone https://github.com/akaitigo/open-pos.git
cd open-pos

# Start infrastructure (PostgreSQL, Redis, RabbitMQ, Hydra)
make up

# Generate protobuf code
make proto

# Build backend
make build

# Install frontend dependencies
pnpm install

# Start development servers
pnpm dev:pos    # POS terminal (port 5173)
pnpm dev:admin  # Admin dashboard (port 5174)
```

See [docs/guides/setup.md](docs/guides/setup.md) for detailed setup instructions.

## Branch Strategy

- `main`: Stable branch. No direct commits.
- `feature/#{issue}-short-description`: Feature development branches.

```
feature/#12-product-crud
feature/#25-offline-sync
```

## Development Workflow

1. **Check the Issue**: Review requirements and acceptance criteria on the GitHub Issue.
2. **Create a branch**: `git checkout -b feature/#12-product-crud`
3. **Develop**: Implement code + tests.
4. **Lint**: `make lint`
5. **Test**: `make test`
6. **Create PR**: `gh pr create` (include `Closes #12` in the body).
7. **CI passes**: Ensure all GitHub Actions checks pass.
8. **Merge**: Squash merge, which auto-closes the Issue.

## Commit Messages

```
<type>(<scope>): <subject>
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Refactoring |
| `test` | Tests |
| `docs` | Documentation |
| `chore` | Build/config changes |

### Scopes

- Backend: `pos`, `product`, `inventory`, `analytics`, `store`, `gateway`
- Frontend: `pos-terminal`, `admin`, `shared-types`
- Infra: `proto`, `infra`, `ci`

### Examples

```
feat(product): add product CRUD gRPC implementation
fix(pos): fix tax rounding error
chore(infra): update Docker Compose Redis version
```

## Coding Standards

- **Currency**: All monetary values in BIGINT/int64 (sen units: 10000 = 100 JPY). No floating point.
- **Multi-tenant**: All tables must have `organization_id`. Use Hibernate Filters.
- **TypeScript**: No `any` — use `unknown` with type guards. Validate API responses with Zod.
- **Tests**: Backend: `@QuarkusTest` + `@InjectMock`. Frontend: Vitest + React Testing Library.

## Reporting Bugs

Please open a [GitHub Issue](https://github.com/akaitigo/open-pos/issues/new) with:

- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, browser, Node version)

## Security Vulnerabilities

See [SECURITY.md](SECURITY.md) for reporting security issues. **Do not** open public issues for vulnerabilities.
