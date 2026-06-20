# Makefile — task shortcuts for the microservices stack.
# Run from this directory (where docker-compose.yml lives), e.g. "make up", or from the repo root: "make -C microservices up".

DC := docker compose
SVC ?=
FMT_ROOTS := ../../ledger_service ../../exchange_service ../../frontend_service

.DEFAULT_GOAL := help
.PHONY: up reset logs down fmt fmt-check help

up:  ## build (cached) and start in the background
	$(DC) build
	$(DC) up -d

reset:  ## wipe volumes (re-runs init.sql), rebuild, start - DELETES ALL DATA
	$(DC) down -v --remove-orphans
	$(DC) build
	$(DC) up -d

logs:  ## tail logs; one service: make logs SVC=ledger-service-app
	$(DC) logs -f $(SVC)

down:  ## stop and remove containers (keeps volumes / data)
	$(DC) down

fmt:  ## format all Clojure(Script) sources to the project style
	cd tools/fmt && clojure -M:run fix $(FMT_ROOTS)

fmt-check:  ## verify formatting; non-zero exit if any file would change (used by CI)
	cd tools/fmt && clojure -M:run check $(FMT_ROOTS)

help:
	@echo Targets: up, reset, logs, down, fmt, fmt-check
	@echo   up         build cached image and start detached
	@echo   reset      wipe all data, rebuild, start
	@echo   logs       tail logs (add SVC=name for one service)
	@echo   down       stop, keeping data
	@echo   fmt        format all Clojure(Script) sources to the project style
	@echo   fmt-check  verify formatting (non-zero exit if changes needed)
