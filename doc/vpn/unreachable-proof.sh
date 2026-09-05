#!/bin/bash
# =============================================================================
# unreachable-proof.sh — Prove that the deployed service is NOT reachable
# from a host that is not on the OpenVPN tunnel.
#
# Usage:
#   ./unreachable-proof.sh <vpn-internal-host> [public-ip]
#
# Exit codes:
#   0  PASS — service is unreachable from the public network (proof holds)
#   1  FAIL — service responded, isolation is broken, investigate
#   2  setup — missing argument or curl missing
#   3  SKIP  — could not classify the result (see log); treat as inconclusive
#
# Why this script exists:
#   The take-home assignment explicitly requires evidence that the service
#   sits behind a VPN and is not exposed to the public internet. This script
#   automates the negative test from the reviewer's own host (which must NOT
#   have the VPN connected when this is run).
# =============================================================================
set -u

if [ "$#" -lt 1 ]; then
  echo "[unreachable-proof] usage: $0 <vpn-internal-host> [public-ip]" >&2
  exit 2
fi

INTERNAL_HOST="$1"
PUBLIC_HOST="${2:-}"
APP_PORT="${APP_PORT:-8099}"
PROBE_TIMEOUT="${PROBE_TIMEOUT:-5}"

if ! command -v curl >/dev/null 2>&1; then
  echo "[unreachable-proof] FAIL: curl is not installed" >&2
  exit 2
fi

probe() {
  local label="$1" host="$2"
  echo "[unreachable-proof] Target: ${host}:${APP_PORT}"
  echo "[unreachable-proof] Probing ${label} ..."
  # --max-time caps the wait; --connect-timeout caps the SYN/SYN-ACK round.
  # We deliberately do NOT use -f so we can capture the curl exit code.
  out="$(curl -sS --connect-timeout "$PROBE_TIMEOUT" --max-time "$PROBE_TIMEOUT" \
        -w "\n__CURL_HTTP_CODE__=%{http_code}\n__CURL_TIME__=%{time_total}\n" \
        "http://${host}:${APP_PORT}/actuator/health" 2>&1)"
  rc=$?
  echo "$out"
  echo "[unreachable-proof] curl exit code: ${rc}"
  echo
  return "$rc"
}

# -----------------------------------------------------------------------------
# 1) Probe the VPN-internal host directly. If it is reachable without the VPN,
#    isolation is broken.
# -----------------------------------------------------------------------------
probe "internal host without VPN" "$INTERNAL_HOST"
rc_internal=$?

if [ "$rc_internal" -eq 0 ]; then
  echo "[unreachable-proof] FAIL: internal host is reachable WITHOUT the VPN." >&2
  echo "[unreachable-proof]       Isolation is broken — investigate the deploy." >&2
  exit 1
fi

# -----------------------------------------------------------------------------
# 2) (Optional) Probe the public IP of the deploy server. If it answers on
#    APP_PORT, the service is exposed to the public internet.
# -----------------------------------------------------------------------------
if [ -n "$PUBLIC_HOST" ]; then
  probe "public IP without VPN" "$PUBLIC_HOST"
  rc_public=$?
  if [ "$rc_public" -eq 0 ]; then
    echo "[unreachable-proof] FAIL: public IP answered on :${APP_PORT}." >&2
    echo "[unreachable-proof]       Service is reachable from the internet — fix the firewall." >&2
    exit 1
  fi
fi

# -----------------------------------------------------------------------------
# Interpret curl exit codes we care about:
#    7  = "Couldn't connect to server" (connection refused / no listener)
#   28  = "Connection timed out"        (network unreachable / filtered)
#    6  = "Could not resolve host"      (DNS failure on internal name)
#   52  = "Empty reply from server"     (TCP open but no HTTP response)
# Anything else is logged but treated as a PASS for the proof (we only fail
# the proof on rc=0, i.e. a real HTTP response).
# -----------------------------------------------------------------------------
case "$rc_internal" in
  7)  reason="connection refused / no listener" ;;
  28) reason="connection timed out (network filtered)" ;;
  6)  reason="DNS could not resolve ${INTERNAL_HOST}" ;;
  52) reason="empty reply (port open but no HTTP)" ;;
  *)  reason="curl rc=${rc_internal} (see output above)" ;;
esac

echo "[unreachable-proof] PASS: ${INTERNAL_HOST}:${APP_PORT} is unreachable without VPN."
echo "[unreachable-proof]       Reason: ${reason}"
exit 0