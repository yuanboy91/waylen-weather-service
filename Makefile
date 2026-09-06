# Waylen Weather Service — Maven build wrapper
# API key: export OPENWEATHERMAP_API_KEY=xxx (required for run/test)

APP    := waylen-weather-service
PORT   ?= 8099
MAVEN  ?= mvn
SPRING_PROFILES ?=

.PHONY: help all build test test-offline package clean run actuator info stop tail-logs

help:
	@echo "Usage:"
	@echo "  make build         Build (mvn package -DskipTests)"
	@echo "  make test          Run all tests"
	@echo "  make test-offline  Run tests without API key"
	@echo "  make package       Clean build"
	@echo "  make run           Start app on port $(PORT) (needs API key)"
	@echo "  make stop          Stop the running app"
	@echo "  make actuator      GET /actuator/health"
	@echo "  make info          GET /actuator/info"
	@echo "  make tail-logs     Tail application log"
	@echo "  make clean         mvn clean"

# ---------- Build ----------

all: test package

build:
	@if [ -z "$(OPENWEATHERMAP_API_KEY)" ]; then \
	  echo "[note] OPENWEATHERMAP_API_KEY not set — integration tests will fail"; \
	fi
	$(MAVEN) -q -DskipTests package

test:
	@if [ -z "$(OPENWEATHERMAP_API_KEY)" ]; then \
	  echo "[note] OPENWEATHERMAP_API_KEY not set — use 'make test-offline' to skip integration tests"; \
	fi
	$(MAVEN) -q test

# Only run tests that don't require an API key
test-offline:
	$(MAVEN) -q test -Dtest='WeatherServiceCacheTest,WeatherControllerTest'

package:
	$(MAVEN) -q clean package -DskipTests

clean:
	$(MAVEN) -q clean

# ---------- Run ----------

run: build
	@if [ -z "$(OPENWEATHERMAP_API_KEY)" ]; then \
	  echo "[error] OPENWEATHERMAP_API_KEY required — export it first"; \
	  exit 1; \
	fi
	@echo "[run] starting $(APP) on port $(PORT) ..."
	SPRING_PROFILES=$(SPRING_PROFILES) \
	  $(MAVEN) -q spring-boot:run \
	  -Dspring-boot.run.jvmArguments="-Xms256m -Xmx512m"

# ---------- Ops ----------

actuator:
	@curl -fsS "http://localhost:$(PORT)/actuator/health" && echo

info:
	@curl -fsS "http://localhost:$(PORT)/actuator/info" && echo

stop:
	@-pkill -f "$(APP).jar" || true
	@echo "[stop] $(APP) stopped (no-op if not running)"

tail-logs:
	@if [ -f logs/spring.log ]; then tail -n 100 -f logs/spring.log; \
	 else echo "[tail-logs] no logs/spring.log yet"; \
	fi
