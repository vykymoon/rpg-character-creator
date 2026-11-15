#!/usr/bin/env bash
# Despliega en Render una imagen ya publicada en GHCR.
# Uso: RENDER_DEPLOY_HOOK_URL=... scripts/render-deploy.sh ghcr.io/<owner>/rpg-servidor:sha-<sha>
#
# El deploy hook de Render es un secreto (GitHub Environment "production").
# Para un servicio basado en imagen, el parametro imgURL indica que imagen
# exacta desplegar; asi el despliegue y la reversion usan el mismo mecanismo.
set -euo pipefail

IMAGE="${1:?uso: render-deploy.sh <imagen>}"
: "${RENDER_DEPLOY_HOOK_URL:?falta el secreto RENDER_DEPLOY_HOOK_URL}"

case "$IMAGE" in
  *:latest) echo "No se despliega una imagen etiquetada como latest: $IMAGE" >&2; exit 1 ;;
esac

ENCODED="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$IMAGE")"
SEP='&'; [[ "$RENDER_DEPLOY_HOOK_URL" == *\?* ]] || SEP='?'

echo "Desplegando $IMAGE"
HTTP_CODE="$(curl -sS -o /tmp/render-response.json -w '%{http_code}' -X POST "${RENDER_DEPLOY_HOOK_URL}${SEP}imgURL=${ENCODED}")"
echo "Respuesta de Render: HTTP $HTTP_CODE"
cat /tmp/render-response.json || true
echo
if [[ "$HTTP_CODE" -lt 200 || "$HTTP_CODE" -ge 300 ]]; then
  echo "Render rechazo el despliegue" >&2
  exit 1
fi
