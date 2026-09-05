#!/bin/bash
# ===============================
# Usage：
#   ./deploy.sh            
#   ./deploy.sh --rollback 
# ===============================
set -e

# ---------- Setting ----------
APP_NAME="waylen-weather-service"
APP_PORT="8099"
SRC_DIR="/home/deploy/waylen-weather-service"
DEPLOY_DIR="/opt/application"
JAVA_CMD="java"
# -----------------------------


# ---------- Rollback ----------
rollback() {
  log "======== Roll back to the last backup ========"
  [ -f "$DEPLOY_DIR/$APP_NAME.jar.bak" ] || { log "No backup files available"; exit 1; }
  
  OLD=$(pgrep -f "${APP_NAME}.jar" || true)
  [ -n "$OLD" ] && { log "Stop the process $OLD"; kill $OLD || true; sleep 5; }
  
  cp "$DEPLOY_DIR/$APP_NAME.jar.bak" "$DEPLOY_DIR/$APP_NAME.jar"
  cd "$DEPLOY_DIR"
  BUILD_ID=dontKillMe nohup $JAVA_CMD -jar "$APP_NAME.jar" > "$APP_NAME.log" 2>&1 &
  
  sleep 15
  curl -fsS "http://localhost:${APP_PORT}/actuator/health" >/dev/null 2>&1 \
    && log "✔ Rollback completed" \
    || { log "✘ Rollback failed, Please check the application"; tail -n 30 "$APP_NAME.log"; exit 1; }
}

[ "$1" = "--rollback" ] && rollback
# ---------- Rollback ----------


# ---------- Main process ----------
log "======== Start deployment $APP_NAME ========"

log "[1/5] Retrieve the latest code（$SRC_DIR）"
cd "$SRC_DIR"
git fetch origin
BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || echo main)
git reset --hard "origin/$BRANCH"


log "[2/5] Maven packaging"
mvn clean package -DskipTests
JAR="$SRC_DIR/target/$APP_NAME.jar"
[ -f "$JAR" ] || { log "The packaged product does not exist: $JAR"; exit 1; }
log "Maven packaging completed: $JAR"


log "[3/5] Stop the old process"
OLD_PID=$(pgrep -f "${APP_NAME}.jar" || true)
if [ -n "$OLD_PID" ]; then
  log "Stop PID: $OLD_PID"
  kill $OLD_PID || true
  sleep 5
  kill -9 $OLD_PID 2>/dev/null || true
fi


log "[4/5] Backup old packages and copy new packages"
[ -f "$DEPLOY_DIR/$APP_NAME.jar" ] && cp "$DEPLOY_DIR/$APP_NAME.jar" "$DEPLOY_DIR/$APP_NAME.jar.bak"
cp "$JAR" "$DEPLOY_DIR/"
log "The new package has been copied to $DEPLOY_DIR/$APP_NAME.jar"


log "[5/5] Launch the application ..."
cd "$DEPLOY_DIR"
BUILD_ID=dontKillMe nohup $JAVA_CMD -jar "$APP_NAME.jar" > "$APP_NAME.log" 2>&1 &
#log "已启动 PID=$!，等待健康检查（最多 30 秒）..."

for i in $(seq 1 10); do
  sleep 3
  if curl -fsS "http://localhost:${APP_PORT}/actuator/health" >/dev/null 2>&1; then
    log "✔ Deployment successful"
    exit 0
  fi
done

log "✘ Health check timeout, Please check the application"
tail -n 30 "$DEPLOY_DIR/$APP_NAME.log" || true
exit 1
# -----------------------------

