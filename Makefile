.PHONY: help up down build test lint proto clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = \":.*?## \"}; {printf \"\033[36m%-20s\033[0m %s\n\", $$1, $$2}'

# === Infrastructure ===
up: ## Start all infrastructure (PostgreSQL, Redis, RabbitMQ, Hydra)
	docker compose -f infra/compose.yml up -d

down: ## Stop all infrastructure
	docker compose -f infra/compose.yml down

up-dev: ## Start infrastructure with dev tools (pgAdmin, Redis Commander)
	docker compose -f infra/compose.yml -f infra/compose.override.yml up -d

logs: ## Show infrastructure logs
	docker compose -f infra/compose.yml logs -f

# === Build ===
build: ## Build all services
	./gradlew build -x test

build-apps: ## Build all frontend apps
	pnpm -r build

# === Test ===
test: ## Run all backend tests
	./gradlew test

test-apps: ## Run all frontend tests
	pnpm -r test

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

# === Clean ===
clean: ## Clean all build artifacts
	./gradlew clean
	pnpm -r clean
	rm -rf proto/gen
