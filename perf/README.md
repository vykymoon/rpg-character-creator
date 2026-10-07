# Pruebas de carga del servidor LAN

Miden el servidor autoritativo del ADR-001 bajo varios jugadores simultáneos.

## Qué se mide

`move_latency`: el tiempo entre que un jugador envía un `move` y recibe el `state` en el que el servidor ya refleja esa posición. Es lo que percibe el jugador — no el tiempo de ida del paquete.

`moves_omitted`: movimientos que nunca vieron su confirmación antes del siguiente. Cuentan como señal temprana de saturación, antes de que aparezcan errores.

## SLO

Del ADR-001: p95 por debajo de 150 ms y menos del 1 % de movimientos sin confirmar. Los dos están declarados como `thresholds` en el script, así que k6 termina con código distinto de cero si no se cumplen.

## Cómo ejecutar

Requiere [k6](https://grafana.com/docs/k6/latest/get-started/installation/) y el servidor levantado.

```
mvn compile exec:java@server
```

Desde la raíz del repositorio, en otra terminal:

```
k6 run --env LABEL=oficial-baseline --env PLAYERS=2 --env DURATION=1m perf/scripts/lan_ws_k6.js
k6 run --env LABEL=oficial-carga    --env PLAYERS=8 --env DURATION=3m perf/scripts/lan_ws_k6.js
```

Barrido de jugadores, manteniendo el intervalo en 200 ms:

```
for N in 2 4 8 16 24 32 48 64; do
  k6 run --env LABEL=jug-$N --env PLAYERS=$N perf/scripts/lan_ws_k6.js
done
```

Barrido de frecuencia, manteniendo los jugadores fijos:

```
for MS in 50 100 200 500 1000; do
  k6 run --env LABEL=int8-$MS --env PLAYERS=8 --env INTERVAL_MS=$MS perf/scripts/lan_ws_k6.js
done
```

Conviene reiniciar el servidor entre corridas: los jugadores de una corrida anterior siguen en el estado hasta que el servidor los descarta, y el `broadcast` los sigue incluyendo.

## Parámetros

| Variable | Por defecto | Qué hace |
|---|---|---|
| `BASE_URL` | `ws://localhost:8887` | Servidor a probar |
| `PLAYERS` | 2 | Jugadores simultáneos (VUs de k6) |
| `INTERVAL_MS` | 200 | Milisegundos entre movimientos de cada jugador |
| `DURATION` | `1m` | Duración del escenario |
| `LABEL` | `jug-<PLAYERS>` | Nombre de la corrida; da nombre al resumen |

## Resultados

Cada corrida deja su resumen en `perf/results/summary-<LABEL>.json`. La tabla completa de las mediciones del equipo y su interpretación están en [`perf/results/resultados.md`](results/resultados.md), y el análisis arquitectónico en [`docs/pruebas.md`](../docs/pruebas.md).

## Nota sobre TCP_NODELAY

Las corridas `nodelay-*` de la tabla de resultados se hicieron con el algoritmo de Nagle desactivado en el socket del servidor. Ese cambio no está en el código: fue una prueba para ubicar de dónde venía la latencia. El hallazgo y lo que implicaría aplicarlo están en `docs/pruebas.md`.
