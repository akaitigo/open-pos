.PHONY: help up up-all up-dev down logs \
       dev-gateway dev-product dev-store dev-pos dev-inventory dev-analytics dev-backend \
       build build-apps build-services \
       test test-apps test-backend test-frontend test-all \
       lint proto proto-lint proto-breaking seed clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# === Infrastructure ===
up: ## Start infrastructure only (PostgreSQL, Redis, RabbitMQ, Hydra)
	docker compose -f infra/compose.yml up -d postgres redis rabbitmq hydra-migrate hydra

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

test-apps: ## Run all frontend tests
	pnpm -r test

test-backend: ## Run all backend tests (alias for test)
	./gradlew test

test-frontend: ## Run all frontend tests
	pnpm -r test -- --run

test-all: test-backend test-frontend ## Run all backend and frontend tests

# === Lint ===
lint: ## Lint proto, backend, and frontend
	cd proto && buf lint
	./gradlew ktlintCheck
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
