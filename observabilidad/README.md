# Observabilidad (bonificación del Corte 3)

El servidor expone sus métricas en `GET /metrics`, en formato de texto de Prometheus, sin librerías extra (`ServerMetrics.java`). Prometheus las lee cada 5 segundos y Grafana las muestra en un tablero ligado al SLO del ADR-001.

## Levantarlo

```bash
cp .env.example .env     # define GRAFANA_ADMIN_PASSWORD
docker compose --profile observabilidad up --build
```

- Grafana: http://localhost:3000 → *Dashboards → RPG → RPG - SLO del servidor de partida*
- Prometheus: http://localhost:9090 (*Status → Targets* debe mostrar `rpg-servidor` en estado UP)

Para ver datos, se genera carga con el script del Corte 2:

```bash
k6 run --env PLAYERS=8 --env DURATION=2m --env JOIN_CODE=<codigo> perf/scripts/lan_ws_k6.js
```

## Métricas y SLO

| Panel | PromQL | SLO (ADR-001) |
|---|---|---|
| Latencia p95 | `histogram_quantile(0.95, sum by (le) (rate(rpg_move_duration_seconds_bucket[1m])))` | < 150 ms (línea roja) |
| Tasa de error | `sum(rate(rpg_moves_total{result=~"error\|not_joined\|spoofed"}[1m])) / sum(rate(rpg_moves_total[1m]))` | < 1 % (línea roja) |
| Throughput | `sum(rate(rpg_moves_total[1m]))` | — |
| Abuso | `rate(rpg_messages_dropped_total[1m])`, `rate(rpg_joins_total{result!="ok"}[1m])` | Señales de las amenazas A-02 y A-05 |

`rpg_move_duration_seconds` mide, en el servidor, el tiempo de procesar un `move` y difundir el estado. La latencia que ve el jugador, de ida y vuelta, la mide k6 en el pipeline (`move_latency`). El tablero sirve para ubicar si una violación del SLO viene del servidor o de la red.

`prometheus/alertas.yml` define las reglas de grabación `rpg:move_latency_p95_seconds` y `rpg:move_error_ratio`, y alertas cuando el SLO se incumple durante 2 minutos o el servidor deja de responder.

## Archivos

```
prometheus/prometheus.yml          scrape del servicio "servidor" de Compose
prometheus/alertas.yml             reglas del SLO
grafana/provisioning/              datasource y proveedor del tablero (se cargan solos)
grafana/dashboards/rpg-slo.json    el tablero
```
