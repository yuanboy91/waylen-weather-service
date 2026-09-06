#!/bin/bash
# Deploy: pull → build → deploy; supports --rollback
# Usage: ./deploy.sh [--rollback]
set -e

# ---- config ----
APP_NAME="waylen-weather-service"
APP_PORT="8099"
SRC_DIR="/home/deploy/waylen-weather-service"
DEPLOY_DIR="/opt/application"
JAVA_CMD="java"
HEALTH_URL="http://localhost:${APP_PORT}/actuator/health"
HEALTH_RETRIES=10
HEALTH_INTERVAL=3
OPENWEATHERMAP_API_KEY=""
# --------------

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

[ -n "$OPENWEATHERMAP_API_KEY" ] && JAVA_ARGS="--openweathermap.api.key=$OPENWEATHERMAP_API_KEY" || JAVA_ARGS=""

# Graceful shutdown (5s), force kill on timeout
stop_app() {
  local pid=$(pgrep -f "${APP_NAME}.jar" || true)
  [ -z "$pid" ] && return
  log "Stopping PID: $pid"
  kill "$pid" 2>/dev/null || true
  sleep 5
  kill -9 "$pid" 2>/dev/null || true
}

# Start app and poll health check
start_and_check() {
  cd "$DEPLOY_DIR"
  nohup $JAVA_CMD $JVM_OPTS -jar -Xms512m -Xmx512m "$APP_NAME.jar" $JAVA_ARGS > "$APP_NAME.log" 2>&1 &
  log "Waiting for health check (max $((HEALTH_RETRIES * HEALTH_INTERVAL))s)..."
  for i in $(seq 1 $HEALTH_RETRIES); do
    sleep $HEALTH_INTERVAL
    curl -fsS "$HEALTH_URL" >/dev/null 2>&1 && { log "✔ Health check passed"; return 0; }
    log "  Health check $i/$HEALTH_RETRIES ..."
  done
  log "✘ Health check timeout"
  tail -n 30 "$DEPLOY_DIR/$APP_NAME.log" || true
  return 1
}

# ---- rollback ----
if [ "$1" = "--rollback" ]; then
  log "======== Rollback ========"
  [ -f "$DEPLOY_DIR/$APP_NAME.jar.bak" ] || { log "No backup available"; exit 1; }
  stop_app
  cp "$DEPLOY_DIR/$APP_NAME.jar.bak" "$DEPLOY_DIR/$APP_NAME.jar"
  start_and_check && { log "✔ Rollback done"; exit 0; }
  log "✘ Rollback failed"; exit 1
fi

# ---- deploy ----
log "======== Deploying $APP_NAME ========"

log "[1/5] Pulling code"
cd "$SRC_DIR"
git fetch origin
BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || echo main)
git reset --hard "origin/$BRANCH"

log "[2/5] Building"
mvn clean package -DskipTests
JAR="$SRC_DIR/target/$APP_NAME.jar"
[ -f "$JAR" ] || { log "Artifact not found: $JAR"; exit 1; }

log "[3/5] Stopping old process"
stop_app

log "[4/5] Deploying package"
[ -f "$DEPLOY_DIR/$APP_NAME.jar" ] && cp "$DEPLOY_DIR/$APP_NAME.jar" "$DEPLOY_DIR/$APP_NAME.jar.bak"
cp "$JAR" "$DEPLOY_DIR/"

log "[5/5] Starting application"
start_and_check && { log "✔ Deployed"; exit 0; }
log "✘ Deploy failed"; exit 1
