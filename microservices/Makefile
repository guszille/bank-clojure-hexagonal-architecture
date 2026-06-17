# Makefile — task shortcuts for the microservices stack.
# Run from this directory (where docker-compose.yml lives), e.g. "make up", or from the repo root: "make -C microservices up".

DC := docker compose
SVC ?=

.DEFAULT_GOAL := help
.PHONY: up reset logs down help

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

help:
	@echo Targets: up, reset, logs, down
	@echo   up     build cached image and start detached
	@echo   reset  wipe all data, rebuild, start
	@echo   logs   tail logs (add SVC=name for one service)
	@echo   down   stop, keeping data
