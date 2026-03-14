.PHONY: help doctor verify verify-full up up-all up-dev down logs \
       dev-gateway dev-product dev-store dev-pos dev-inventory dev-analytics dev-backend \
       local-build local-up local-up-fast local-down local-seed local-smoke local-demo \
       docker-build-core docker-up-core docker-down-core docker-smoke docker-demo \
       build build-apps build-services \
       test test-apps test-backend test-frontend test-e2e test-all \
       lint proto proto-lint proto-breaking seed clean

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
	docker compose -f infra/compose.yml -f infra/compose.override.yml up -d

logs: ## Show infrastructure logs
	docker compose -f infra/compose.yml logs -f

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

dev-backend: ## Start all backend services in dev mode (background)
	cd services/product-service && ../../gradlew quarkusDev &
	cd services/store-service && ../../gradlew quarkusDev &
	cd services/pos-service && ../../gradlew quarkusDev &
	cd services/inventory-service && ../../gradlew quarkusDev &
	cd services/analytics-service && ../../gradlew quarkusDev &
	cd services/api-gateway && ../../gradlew quarkusDev &

local-build: ## Build the core backend services locally for reliable dev startup
	./gradlew \
		:services:product-service:quarkusBuild \
		:services:store-service:quarkusBuild \
		:services:pos-service:quarkusBuild \
		:services:api-gateway:quarkusBuild \
		-Dquarkus.package.jar.type=uber-jar

local-up: ## Build and start the core backend services locally
	bash scripts/local-stack-up.sh

local-up-fast: ## Start the core backend services locally without rebuilding
	bash scripts/local-stack-up.sh --skip-build

local-down: ## Stop backend services started by scripts/local-stack-up.sh
	bash scripts/local-stack-down.sh

local-seed: ## Seed demo data and generate frontend .env.development.local files
	bash scripts/seed.sh

local-smoke: ## Verify the seeded local demo data via the API gateway
	bash scripts/local-demo-smoke.sh

local-demo: up local-up local-seed local-smoke ## Start infra, local backend, seed demo data, and verify the API path
	@echo "Demo data is ready. Reload the browser to pick up the latest demo-config.json."

docker-build-core: ## Build the core backend container images sequentially
	docker compose -f infra/compose.yml build product-service
	docker compose -f infra/compose.yml build store-service
	docker compose -f infra/compose.yml build pos-service
	docker compose -f infra/compose.yml build api-gateway

docker-up-core: up local-down ## Start the core backend services in containers
	docker compose -f infra/compose.yml up -d --wait product-service store-service pos-service api-gateway

docker-down-core: ## Stop only the containerized core backend services
	docker compose -f infra/compose.yml stop api-gateway product-service store-service pos-service

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
	cd proto && buf breaking --against '.git#branch=main'

# === Seed Data ===
seed: ## Seed demo data (requires running backend)
	bash scripts/seed.sh

# === Clean ===
clean: ## Clean all build artifacts
	./gradlew clean
	pnpm -r clean
	rm -rf proto/gen
