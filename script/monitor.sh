#!/bin/bash
# ============================================================
# monitor.sh — Application health monitor + email alert
# Schedule: run every 5 minutes via crontab
# ============================================================

# ---------- Setting ----------
APP_NAME="waylen-weather-service"
APP_PORT="8099"
HEALTH_URL="http://localhost:${APP_PORT}/actuator/health"
STATE_FILE="/tmp/monitor.state"
LOG_FILE="/home/deploy/monitor.log"

# SMTP
SMTP_SERVER="smtp.qq.com"
SMTP_PORT="465"
SMTP_USER="yuanboy91@qq.com"
SMTP_AUTH_CODE="bhzkogqroqrccbea"
MAIL_TO="yuanboy91@qq.com"
# -------------------------------

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"; }

# send an email
send_mail() {
  local subject="$1" body="$2" subj_b64
  subj_b64=$(printf '%s' "$subject" | base64 -w0)   # UTF-8 encode Chinese subject
  {
    printf 'From: %s\n' "$SMTP_USER"
    printf 'To: %s\n' "$MAIL_TO"
    printf 'Subject: =?UTF-8?B?%s?=\n' "$subj_b64"
    printf 'Content-Type: text/plain; charset=utf-8\n'
    printf '\n%s\n' "$body"
  } > /tmp/mail_body.txt
  curl -s --url "smtps://${SMTP_SERVER}:${SMTP_PORT}" \
       --ssl-reqd \
       --mail-from "$SMTP_USER" \
       --mail-rcpt "$MAIL_TO" \
       -u "${SMTP_USER}:${SMTP_AUTH_CODE}" \
       -T /tmp/mail_body.txt
  rm -f /tmp/mail_body.txt
}

# Health check: retry 3 times on failure
check() {
  for i in 1 2 3; do
    if curl -fsS --connect-timeout 5 --max-time 10 "$HEALTH_URL" >/dev/null 2>&1; then
      return 0
    fi
    [ "$i" -lt 3 ] && sleep 5
  done
  return 1
}

# No state file means last status was OK
PREV=$(cat "$STATE_FILE" 2>/dev/null || echo ok)

# Application exception: only handle failures; normal state is fully silent
if ! check; then
  if [ "$PREV" = "down" ]; then
    log "Continuous abnormal application"
  else
    log "Application exception, send alert email"
    send_mail "【Alert】${APP_NAME} Health check failed" \
"Application health check failed: ${HEALTH_URL}
Time: $(date '+%Y-%m-%d %H:%M:%S')
Please log in to the server to check."
  fi
  echo down > "$STATE_FILE"
  exit 1
fi

# Normal: only reset state silently
echo ok > "$STATE_FILE"