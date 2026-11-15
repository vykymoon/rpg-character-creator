#!/usr/bin/env bash
# Espera a que el servicio responda /health con status UP y, si se indica,
# con la version esperada. Lo usan el despliegue, la reversion y el pipeline
# de carga larga, porque el plan gratuito de Render suspende el servicio por
# inactividad y tarda en despertar.
# Uso: scripts/wait-for-health.sh <url-base> [version-esperada] [timeout-segundos]
set -euo pipefail

URL="${1:?uso: wait-for-health.sh <url-base> [version] [timeout]}"
EXPECTED="${2:-}"
TIMEOUT="${3:-600}"
DEADLINE=$(( $(date +%s) + TIMEOUT ))

while true; do
  BODY="$(curl -sS --max-time 20 "${URL%/}/health" || true)"
  STATUS="$(echo "$BODY" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("status",""), d.get("version",""))' 2>/dev/null || true)"
  read -r ST VER <<<"${STATUS:-x x}" || true
  if [[ "${ST:-}" == "UP" && ( -z "$EXPECTED" || "${VER:-}" == "$EXPECTED" ) ]]; then
    echo "Servicio listo: status=$ST version=$VER"
    exit 0
  fi
  if (( $(date +%s) >= DEADLINE )); then
    echo "El servicio no respondio la version esperada (${EXPECTED:-cualquiera}) en ${TIMEOUT}s. Ultima respuesta: $BODY" >&2
    exit 1
  fi
  echo "Esperando... status=${ST:-?} version=${VER:-?}"
  sleep 10
done
