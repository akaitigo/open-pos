.PHONY: help doctor verify verify-full up up-all up-dev down logs logs-pos \
       dev-gateway dev-product dev-store dev-pos dev-inventory dev-analytics dev-backend dev-backend-stop \
       local-build local-up local-up-fast local-down local-seed local-smoke local-demo reset \
       docker-build docker-build-core docker-up-core docker-down-core docker-smoke docker-demo \
       demo-up demo-down \
       build build-apps build-services \
       test test-apps test-backend test-frontend test-e2e test-all grpc-test load-test \
       lint proto proto-lint proto-breaking seed db-backup db-restore clean \
       prune-branches

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

doctor: ## Check whether the local development prerequisites are installed and usable
	bash scripts/doctor.sh

verify: ## Run the supported local quality gate (typecheck, lint, unit/functional tests)
	pnpm -r typecheck
	$(MAKE) lint
	$(MAKE) test-all

verify-full: verify docker-demo ## Run the full local quality gate including demo smoke and Playwright E2E
	$(MAKE) test-e2e

# === Infrastructure ===
up: ## Start infrastructure only (PostgreSQL, Redis, RabbitMQ, Hydra)
	docker compose -f infra/compose.yml up -d --wait postgres redis rabbitmq hydra

up-all: ## Start infrastructure + all backend services
	docker compose -f infra/compose.yml up -d

down: ## Stop all containers
	docker compose -f infra/compose.yml down

up-dev: ## Start infrastructure with dev tools (pgAdmin, Redis Commander)
	docker compose -f infra/compose.yml --profile dev up -d

logs: ## Show Docker Compose logs for infrastructure and any containerized backend services
	docker compose -f infra/compose.yml logs -f

logs-pos: ## Show pos-service logs (host-run or containerized mode)
	bash scripts/logs-pos.sh

# === Local Development (quarkusDev) ===
dev-gateway: ## Start api-gateway in dev mode
	cd services/api-gateway && ../../gradlew quarkusDev

dev-product: ## Start product-service in dev mode
	cd services/product-service && ../../gradlew quarkusDev

dev-store: ## Start store-service in dev mode
	cd services/store-service && ../../gradlew quarkusDev

dev-pos: ## Start pos-service in dev mode
	cd services/pos-service && ../../gradlew quarkusDev

dev-inventory: ## Start inventory-service in dev mode
	cd services/inventory-service && ../../gradlew quarkusDev

dev-analytics: ## Start analytics-service in dev mode
	cd services/analytics-service && ../../gradlew quarkusDev

dev-backend: ## Start all backend services in dev mode (managed, Ctrl+C to stop all)
	bash scripts/dev-backend.sh

dev-backend-stop: ## Stop dev-backend services started by scripts/dev-backend.sh
	@for f in .local/pids/dev/*.pid; do \
		[ -e "$$f" ] || continue; \
		pid=$$(cat "$$f"); \
		kill -TERM "$$pid" 2>/dev/null || true; \
		rm -f "$$f"; \
	done
	@echo "dev-backend services stopped."

local-build: ## Build the supported local backend services locally for reliable dev startup
	./gradlew \
		:services:product-service:quarkusBuild \
		:services:store-service:quarkusBuild \
		:services:pos-service:quarkusBuild \
		:services:inventory-service:quarkusBuild \
		:services:api-gateway:quarkusBuild \
		-Dquarkus.package.jar.type=uber-jar

local-up: ## Build and start the supported local backend services
	bash scripts/local-stack-up.sh

local-up-fast: ## Start the supported local backend services without rebuilding
	bash scripts/local-stack-up.sh --skip-build

local-down: ## Stop backend services started by scripts/local-stack-up.sh
	bash scripts/local-stack-down.sh

local-seed: ## Seed demo data and generate frontend .env.development.local files
	bash scripts/seed.sh

local-smoke: ## Verify the seeded local demo data via the API gateway
	bash scripts/local-demo-smoke.sh

local-demo: up local-up local-seed local-smoke ## Start infra, local backend, seed demo data, and verify the API path
	@echo "Demo data is ready. Reload the browser to pick up the latest demo-config.json."

demo-up: local-demo ## Alias: start the full local demo (infra + backend + seed + smoke)
	@echo "Run 'pnpm dev:pos' and 'pnpm dev:admin' to open the frontends."

demo-down: local-down down ## Alias: stop the local demo (backend + infra)
	@echo "Demo stopped."

reset: ## Reset PostgreSQL data, restart the supported backend mode, and reseed the demo data
	bash scripts/reset.sh

docker-build: ## Build all backend service container images in parallel
	docker compose -f infra/compose.yml build product-service store-service pos-service inventory-service analytics-service api-gateway

docker-build-core: ## Build the supported local backend container images in parallel
	docker compose -f infra/compose.yml build product-service store-service pos-service inventory-service api-gateway

docker-up-core: up local-down ## Start the supported local backend services in containers
	docker compose -f infra/compose.yml up -d --wait product-service store-service pos-service inventory-service api-gateway

docker-down-core: ## Stop only the supported local backend services in containers
	docker compose -f infra/compose.yml stop api-gateway product-service store-service pos-service inventory-service

docker-smoke: ## Seed demo data and verify the containerized core stack via the API gateway
	bash scripts/seed.sh
	bash scripts/local-demo-smoke.sh

docker-demo: docker-build-core docker-up-core docker-smoke ## Build, start, seed, and verify the containerized core stack
	@echo "Container demo data is ready. Reload the browser to pick up the latest demo-config.json."

# === Build ===
build: ## Build all services
	./gradlew build -x test

build-apps: ## Build all frontend apps
	pnpm -r build

build-services: ## Build all backend services (alias for build)
	./gradlew build -x test

# === Test ===
test: ## Run all backend tests
	./gradlew test

test-apps: ## Run frontend unit/functional tests
	pnpm test

test-backend: ## Run all backend tests (alias for test)
	./gradlew test

test-frontend: ## Run frontend unit/functional tests (alias for test-apps)
	pnpm test

test-e2e: ## Run Playwright E2E tests (requires browsers via `pnpm e2e:install`)
	pnpm test:e2e

test-all: test-backend test-frontend ## Run backend + frontend unit/functional tests

grpc-test: ## Run grpcurl-based health checks against the running backend gRPC services
	bash scripts/grpc-test.sh

load-test: ## Run k6 load tests against the running local stack (requires k6)
	bash scripts/run-k6.sh

# === Lint ===
lint: ## Lint proto and frontend
	cd proto && buf lint
	pnpm -r lint

# === Proto ===
proto: ## Generate code from proto definitions
	cd proto && buf generate

proto-lint: ## Lint proto files
	cd proto && buf lint

proto-breaking: ## Check proto breaking changes
	cd proto && buf breaking --against '../.git#branch=main,subdir=proto'

# === Seed Data ===
seed: ## Seed demo data (requires running backend)
	bash scripts/seed.sh

db-backup: ## Write a PostgreSQL SQL backup to FILE=.local/backups/openpos-YYYYmmdd-HHMMSS.sql by default
	bash scripts/db-backup.sh "$(FILE)"

db-restore: ## Restore a PostgreSQL SQL backup from FILE=path and restart the detected backend mode
	bash scripts/db-restore.sh "$(FILE)"

# === Git Maintenance ===
prune-branches: ## Delete local branches whose remote tracking branch is gone
	git fetch --prune origin
	git branch -vv | grep ': gone]' | awk '{print $$1}' | xargs -r git branch -d

# === Clean ===
clean: ## Clean all build artifacts
	./gradlew clean
	pnpm -r clean
	rm -rf proto/gen
	rm -rf .local
	rm -f apps/admin-dashboard/.env.development.local apps/pos-terminal/.env.development.local
	rm -f apps/admin-dashboard/public/demo-config.json apps/pos-terminal/public/demo-config.json
