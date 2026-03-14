# Local Development Runbook

This runbook describes the supported day-to-day development flows for `open-pos`.

## Recommended Modes

### Host-run local backend

Use this for normal development. Infrastructure runs in Docker, while the supported local backend runs on the host as built Quarkus jars.

```bash
make local-demo
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

`make local-demo` performs all of the following:

- starts infrastructure via Docker Compose
- builds and starts `product-service`, `store-service`, `pos-service`, `inventory-service`, and `api-gateway` on the host
- seeds idempotent demo data
- writes frontend runtime config files
- runs the API smoke check

### Containerized local backend

Use this when you want the supported local backend to match the containerized CI/release-style path more closely.

```bash
make docker-demo
pnpm dev:admin
pnpm dev:pos
```

When switching from the host-run path, stop the local backend first:

```bash
make local-down
make docker-up-core
```

## Common Commands

### Infrastructure only

```bash
make up       # PostgreSQL, Redis, RabbitMQ, Hydra
make up-dev   # infra + pgAdmin + Redis Commander
make down
make logs     # Docker Compose logs
```

### Host-run backend

```bash
make local-up       # rebuild and start the local backend
make local-up-fast  # start without rebuilding
make local-down
make logs-pos       # tail .local/logs/pos-service.log while host-run mode is active
```

Logs and PIDs for the host-run backend are written to:

- `.local/logs/`
- `.local/pids/`

### Containerized backend

```bash
make docker-build        # all backend images
make docker-build-core
make docker-up-core
make docker-down-core
```

### Seed and smoke

```bash
make local-seed
make local-smoke
make docker-smoke
make grpc-test
make db-backup
make db-restore FILE=.local/backups/openpos-YYYYmmdd-HHMMSS.sql
make reset
```

`make reset` recreates the PostgreSQL data volume, restores the last detected supported backend mode, and reseeds the demo data. If no supported backend mode was running, it starts the host-run local backend before reseeding.

The seeded dataset contains:

- `テスト株式会社` (`T1234567890123`)
- stores `渋谷店` and `新宿店`
- owner / manager / cashier staff in each store (`1234` / `2345` / `3456`)
- 4 categories, 40 products, and inventory normalized to `100`
- 10 sample transactions (`COMPLETED 3 / VOIDED 1 / DRAFT 6`)

### Frontend dev servers

```bash
pnpm dev:admin
pnpm dev:pos
```

### Verification

```bash
make doctor
make verify
pnpm e2e:install
make verify-full
```

`make doctor` warns if `grpcurl` is missing, because `make grpc-test` depends on it.

## Runtime Config Files

`scripts/seed.sh` writes both environment files and runtime config files:

- `apps/admin-dashboard/.env.development.local`
- `apps/pos-terminal/.env.development.local`
- `apps/admin-dashboard/public/demo-config.json`
- `apps/pos-terminal/public/demo-config.json`

The frontend apps read `public/demo-config.json` at runtime, so a browser reload is enough after reseeding. You usually do not need to restart the Vite dev servers.

## Health Checks and Useful Endpoints

| Service | URL | Notes |
|---------|-----|-------|
| API Gateway | http://localhost:8080/api/health | Main smoke target |
| Product Service | http://localhost:8081/health | Host-run mode |
| Store Service | http://localhost:8082/health | Host-run mode |
| POS Service | http://localhost:8083/health | Host-run mode |
| PostgreSQL | localhost:15432/openpos | `openpos` / `openpos_dev` |
| Redis | localhost:16379 | no auth |
| RabbitMQ UI | http://localhost:15673 | `openpos` / `openpos_dev` |
| Hydra Public | http://localhost:14444 | OAuth/OIDC public endpoint |
| Hydra Admin | http://localhost:14445 | Hydra admin API |
| pgAdmin | http://localhost:15080 | available via `make up-dev` |
| Redis Commander | http://localhost:18081 | available via `make up-dev` |

Useful checks:

```bash
docker compose -f infra/compose.yml ps
curl -s http://localhost:8080/api/health
docker compose -f infra/compose.yml logs -f postgres
docker compose -f infra/compose.yml logs -f rabbitmq
```

## Troubleshooting

### Port conflicts

`open-pos` intentionally uses non-default local ports for shared infra:

- PostgreSQL: `15432`
- Redis: `16379`
- RabbitMQ AMQP/UI: `15672` / `15673`
- Hydra Public/Admin: `14444` / `14445`

If another local stack is already using those ports, stop it before running `make local-demo` or `make docker-demo`.

### Host-run backend fails to start

Check the per-service logs under `.local/logs/`. If you recently changed dependencies or build outputs, rebuild:

```bash
make local-down
make local-up
```

### Container stack is unhealthy

Inspect the relevant service logs, then do a full reset if necessary:

```bash
docker compose -f infra/compose.yml logs postgres
docker compose -f infra/compose.yml logs rabbitmq
docker compose -f infra/compose.yml down -v
make up
```

### Demo IDs or config are missing

If `demo-config.json` or `.env.development.local` files are missing, reseed:

```bash
make local-seed
make local-smoke
```

If the frontend still shows old data, reload the browser tab.

### Smoke check fails after switching modes

Make sure only one supported backend mode is active at a time:

```bash
make local-down
make docker-down-core
```

Then start the mode you actually want to use.

### Playwright E2E fails locally

Install the browser bundle once, then use the supported full verification path:

```bash
pnpm e2e:install
make verify-full
```
