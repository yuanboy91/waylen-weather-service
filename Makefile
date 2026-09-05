# =============================================================================
# Waylen Weather Service - top-level developer build wrapper around Maven.
#
# Run `make help` for the list of targets.
#
# Conventions:
#   - Every target uses Maven (which the repo expects to be on PATH or under
#     $MAVEN_HOME). Override with `make MAVEN=/path/to/mvn ...`.
#   - Targets that need the OpenWeatherMap API key read it from the
#     environment; fail-fast if it is missing so the developer notices
#     immediately instead of getting a half-broken app at boot.
# =============================================================================

APP       := waylen-weather-service
JAR       := target/$(APP).jar
PORT      ?= 8099
MAVEN     ?= mvn
SPRING_PROFILES ?=

# Fail fast if the API key is missing on targets that need it.
NEED_KEY  := $(OPENWEATHERMAP_API_KEY)

.PHONY: help all build compile test test-offline run run-local package \
        clean actuator health info stop tail-logs

help:
	@echo "================================================================"
	@echo "  $(APP) - make targets"
	@echo "================================================================"
	@echo "  make all           compile + test + package"
	@echo "  make build         mvn compile (deps only)"
	@echo "  make compile       alias for build"
	@echo "  make test          mvn test (may hit real upstream for the"
	@echo "                     open-weather integration tests)"
	@echo "  make test-offline  run only tests that don't need an API key"
	@echo "                     (cache + controller tests; requires no env)"
	@echo "  make package       mvn clean package -DskipTests"
	@echo "  make clean         mvn clean"
	@echo "  make run           run via spring-boot:run (needs API key)"
	@echo "  make run-local     alias for run"
	@echo "  make actuator      show /actuator/health JSON"
	@echo "  make info          show /actuator/info   JSON"
	@echo "  make stop          kill the running app by jar name (best-effort)"
	@echo "  make tail-logs     tail -f logs/spring.log if it exists"
	@echo "================================================================"

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------

all: test package

build compile:
	@if [ -z "$(NEED_KEY)" ] ; then \
	  echo "[note] OPENWEATHERMAP_API_KEY is not set; integration tests that hit" \
	       "the upstream API will fail. Set it to run them." ; \
	fi
	$(MAVEN) -q -DskipTests package

test:
	@if [ -z "$(NEED_KEY)" ] ; then \
	  echo "[note] OPENWEATHERMAP_API_KEY unset - integration tests (e.g." \
	       "OpenWeatherClientTest) will fail. 'make test-offline' skips them." ; \
	fi
	$(MAVEN) -q test

# Skip integration tests that hit the upstream API by class name.
# (Settings also exist in surefire config; the -Dtest filter is the
# belt-and-braces ensure-offline form.)
test-offline:
	$(MAVEN) -q test \
	  -Dtest='WeatherServiceCacheTest,WeatherControllerTest'

package:
	$(MAVEN) -q clean package -DskipTests

clean:
	$(MAVEN) -q clean

# ---------------------------------------------------------------------------
# Run
# ---------------------------------------------------------------------------

run run-local: build
	@if [ -z "$(OPENWEATHERMAP_API_KEY)" ]; then \
	  echo "[error] OPENWEATHERMAP_API_KEY must be set for 'make run'" ; \
	  echo "        export OPENWEATHERMAP_API_KEY=YOUR_KEY   then retry" ; \
	  exit 1 ; \
	fi
	@echo "[run] starting $(APP) on port $(PORT) ..."
	SPRING_PROFILES=$(SPRING_PROFILES) \
	  $(MAVEN) -q spring-boot:run \
	  -Dspring-boot.run.jvmArguments="-Xms256m -Xmx512m"

# ---------------------------------------------------------------------------
# Operational helpers (require the app to already be running on PORT)
# ---------------------------------------------------------------------------

actuator:
	@curl -fsS "http://localhost:$(PORT)/actuator/health" && echo

info:
	@curl -fsS "http://localhost:$(PORT)/actuator/info"   && echo

stop:
	@-pkill -f "$(APP).jar" || true
	@echo "[stop] requested termination of $(APP) (no-op if not running)"

tail-logs:
	@if [ -f logs/spring.log ]; then tail -n 100 -f logs/spring.log; \
	 else echo "[tail-logs] no logs/spring.log yet"; \
	fi
