import ws from 'k6/ws';
import { check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// Prueba de carga del servidor LAN (ADR-001).
//
// Cada usuario virtual es un jugador: abre un WebSocket, hace join y
// despues envia un move cada INTERVAL_MS. La metrica que importa es
// move_latency: el tiempo entre enviar un move y recibir el state en el
// que el servidor ya refleja esa posicion. Es lo que percibe el jugador,
// no el tiempo de ida del paquete.
//
// Parametros (todos por --env):
//   BASE_URL     servidor            (ws://localhost:8887)
//   PLAYERS      jugadores simultaneos (2)
//   INTERVAL_MS  milisegundos entre movimientos (200)
//   DURATION     duracion del escenario (1m)
//   LABEL        nombre de la corrida, va en el nombre del resumen
//
// Ejemplos:
//   k6 run --env LABEL=oficial-baseline --env PLAYERS=2  --env DURATION=1m perf/scripts/lan_ws_k6.js
//   k6 run --env LABEL=oficial-carga    --env PLAYERS=8  --env DURATION=3m perf/scripts/lan_ws_k6.js
//   k6 run --env LABEL=jug-32           --env PLAYERS=32 --env DURATION=1m perf/scripts/lan_ws_k6.js
//   k6 run --env LABEL=int32-100 --env PLAYERS=32 --env INTERVAL_MS=100 perf/scripts/lan_ws_k6.js

const BASE_URL = __ENV.BASE_URL || 'ws://localhost:8887';
const PLAYERS = Number(__ENV.PLAYERS || 2);
const INTERVAL_MS = Number(__ENV.INTERVAL_MS || 200);
const DURATION = __ENV.DURATION || '1m';
const LABEL = __ENV.LABEL || `jug-${PLAYERS}`;

// Latencia de un movimiento: del move enviado al state que lo confirma.
const moveLatency = new Trend('move_latency', true);
// Movimientos que nunca vieron su confirmacion antes de cerrar la sesion.
const movesOmitted = new Counter('moves_omitted');
const moveFailed = new Rate('move_failed');

export const options = {
  vus: PLAYERS,
  duration: DURATION,
  thresholds: {
    // SLO del ADR-001: p95 por debajo de 150 ms y menos del 1 % de
    // movimientos sin confirmar.
    move_latency: ['p(95)<150'],
    move_failed: ['rate<0.01'],
  },
};

// Zona de aparicion despejada del mapa (GameMap.generate deja 1..5 libre).
// Moverse dentro de ella evita que el servidor rechace el movimiento por
// terreno y contamine la medicion con rechazos legitimos.
function nextPosition(step) {
  const ruta = [
    [2, 2], [3, 2], [4, 2], [5, 2],
    [5, 3], [5, 4], [5, 5],
    [4, 5], [3, 5], [2, 5],
    [2, 4], [2, 3],
  ];
  return ruta[step % ruta.length];
}

export default function () {
  const playerId = `k6-${__VU}`;

  const res = ws.connect(BASE_URL, {}, function (socket) {
    // Movimiento pendiente de confirmacion: {x, y, enviadoEn}.
    let pendiente = null;
    let paso = 0;

    socket.on('open', function () {
      socket.send(JSON.stringify({ type: 'join', playerId }));
    });

    socket.on('message', function (raw) {
      let msg;
      try {
        msg = JSON.parse(raw);
      } catch (e) {
        return;
      }
      if (msg.type !== 'state' || pendiente === null) {
        return;
      }
      const yo = msg.players && msg.players[playerId];
      if (!yo) {
        return;
      }
      // Solo cuenta el state en el que el servidor ya aplico MI movimiento.
      if (yo.x === pendiente.x && yo.y === pendiente.y) {
        moveLatency.add(Date.now() - pendiente.enviadoEn);
        moveFailed.add(false);
        pendiente = null;
      }
    });

    socket.setInterval(function () {
      // Si el anterior nunca se confirmo, el servidor va por detras.
      if (pendiente !== null) {
        movesOmitted.add(1);
        moveFailed.add(true);
      }
      const [x, y] = nextPosition(paso++);
      pendiente = { x, y, enviadoEn: Date.now() };
      socket.send(JSON.stringify({ type: 'move', playerId, x, y }));
    }, INTERVAL_MS);

    socket.setTimeout(function () {
      socket.close();
    }, durationToMs(DURATION));
  });

  check(res, { 'conexion establecida (101)': (r) => r && r.status === 101 });
}

function durationToMs(d) {
  const m = /^(\d+)(ms|s|m|h)$/.exec(String(d).trim());
  if (!m) return 60000;
  const n = Number(m[1]);
  return { ms: n, s: n * 1000, m: n * 60000, h: n * 3600000 }[m[2]];
}

export function handleSummary(data) {
  const p95 = data.metrics.move_latency
    ? Math.round(data.metrics.move_latency.values['p(95)'])
    : null;
  const resumen = {
    escenario: LABEL,
    jugadores: PLAYERS,
    intervalo_ms: INTERVAL_MS,
    duracion: DURATION,
    p95_ms: p95,
    movimientos_omitidos: data.metrics.moves_omitted
      ? data.metrics.moves_omitted.values.count
      : 0,
    tasa_error: data.metrics.move_failed ? data.metrics.move_failed.values.rate : null,
  };
  const out = {};
  out.stdout = JSON.stringify(resumen, null, 2);
  out[`perf/results/summary-${LABEL}.json`] = JSON.stringify(data, null, 2);
  return out;
}
