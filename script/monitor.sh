#!/bin/bash
# monitor.sh — Application health monitor + email alert (crontab every 5 min)

# ---- config ----
APP_NAME="waylen-weather-service"
APP_PORT="8099"
HEALTH_URL="http://localhost:${APP_PORT}/actuator/health"
LOG_FILE="/home/deploy/monitor.log"
STATE_FILE="/tmp/monitor.state"

SMTP_SERVER="smtp.qq.com"
SMTP_PORT="465"
SMTP_USER="yuanboy91@qq.com"
SMTP_AUTH_CODE=""
MAIL_TO="yuanboy91@qq.com"
# --------------

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"; }

# Send email and log result
send_mail() {
  local subject="$1" body="$2"
  local subj_b64=$(printf '%s' "$subject" | base64 -w0)
  {
    printf 'From: %s\nTo: %s\n' "$SMTP_USER" "$MAIL_TO"
    printf 'Subject: =?UTF-8?B?%s?=\n' "$subj_b64"
    printf 'Content-Type: text/plain; charset=utf-8\n\n%s\n' "$body"
  } > /tmp/mail_body.txt
  if curl -s "smtps://${SMTP_SERVER}:${SMTP_PORT}" --ssl-reqd \
       --mail-from "$SMTP_USER" --mail-rcpt "$MAIL_TO" \
       -u "${SMTP_USER}:${SMTP_AUTH_CODE}" -T /tmp/mail_body.txt; then
    log "✔ Email sent: $subject"
  else
    log "✘ Email failed: $subject"
  fi
  rm -f /tmp/mail_body.txt
}

# Health check: retry 3 times, 5s interval
check() {
  for i in 1 2 3; do
    curl -fsS --connect-timeout 5 --max-time 10 "$HEALTH_URL" >/dev/null 2>&1 && return 0
    [ "$i" -lt 3 ] && sleep 5
  done
  return 1
}

PREV=$(cat "$STATE_FILE" 2>/dev/null || echo ok)

# Health check failed
if ! check; then
  if [ "$PREV" = "down" ]; then
    log "Still down, skip repeated alert"
  else
    log "App is down, sending alert email"
    send_mail "[Alert] ${APP_NAME} health check failed" \
"Health check failed: ${HEALTH_URL}
Time: $(date '+%Y-%m-%d %H:%M:%S')
Please log in to the server to check."
  fi
  echo down > "$STATE_FILE"
  exit 1
fi

# Health check passed, send recovery email if was down
if [ "$PREV" = "down" ]; then
  log "App recovered, sending recovery email"
  send_mail "[Recovered] ${APP_NAME} is back to normal" \
"Health check recovered: ${HEALTH_URL}
Time: $(date '+%Y-%m-%d %H:%M:%S')"
fi
echo ok > "$STATE_FILE"
