.DEFAULT_GOAL := help
COMPOSE := docker compose -f infrastructure/docker/docker-compose.yml --env-file .env

.PHONY: help
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.PHONY: setup
setup: ## One-time local setup: env file, formatting tools, git hooks
	@test -f .env || cp .env.example .env
	@npm install --no-fund --no-audit
	@./infrastructure/scripts/install-git-hooks.sh

.PHONY: format
format: ## Auto-format the whole repo (yaml/json/md + Java)
	@npm run format
	@test -f backend/pom.xml && mvn -q -f backend/pom.xml spotless:apply || true

.PHONY: format-check
format-check: ## Check formatting without changing files (what CI runs)
	@npm run format:check
	@test -f backend/pom.xml && mvn -q -f backend/pom.xml spotless:check || true

.PHONY: up
up: ## Start local infrastructure (Mongo, Redis, Kafka, LocalStack)
	$(COMPOSE) up -d

.PHONY: down
down: ## Stop local infrastructure
	$(COMPOSE) down

.PHONY: destroy
destroy: ## Stop local infrastructure and delete all local data volumes
	$(COMPOSE) down -v

.PHONY: logs
logs: ## Tail infrastructure logs
	$(COMPOSE) logs -f

.PHONY: ps
ps: ## Show status of local infrastructure containers
	$(COMPOSE) ps

.PHONY: apps-build
apps-build: ## Package every backend service (prerequisite for apps-up)
	mvn -f backend/pom.xml package -DskipTests

.PHONY: apps-up
apps-up: ## Start infra + all 8 backend services (needs apps-build + Terraform first - see docker-compose.yml)
	$(COMPOSE) --profile apps up -d --build

.PHONY: apps-down
apps-down: ## Stop the full stack (infra + apps)
	$(COMPOSE) --profile apps down

.PHONY: apps-logs
apps-logs: ## Tail logs for the 8 backend services
	$(COMPOSE) --profile apps logs -f identity-service booking-service flight-service hotel-service payment-service notification-service search-service gateway

.PHONY: apps-ps
apps-ps: ## Show status of the full stack (infra + apps)
	$(COMPOSE) --profile apps ps
