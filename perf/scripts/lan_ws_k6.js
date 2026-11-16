import ws from 'k6/ws';
import { check, sleep } from 'k6';
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
//   JOIN_CODE    codigo de sala del servidor desplegado (vacio en LAN)
//
// Corte 3: el mismo script corre en el pipeline despues de cada despliegue
// (cd.yml, etapa "Prueba de carga") contra wss://<servicio>/ws. Los
// thresholds son el SLO del ADR-001: si no se cumple, k6 termina con codigo
// distinto de cero y el pipeline falla.
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
const JOIN_CODE = __ENV.JOIN_CODE || '';
const SUMMARY_DIR = __ENV.SUMMARY_DIR || 'perf/results';
// Un movimiento cuenta como "sin confirmar" si el servidor no lo refleja en
// el estado dentro de este plazo. Ver la nota de medicion en docs/pruebas.md.
const CONFIRM_TIMEOUT_MS = Number(__ENV.CONFIRM_TIMEOUT_MS || 1000);
const CONNECT_ATTEMPTS = 3;

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
    // Sin conexiones fallidas: un 4401 (codigo de sala) o un 404 tumba la prueba.
    checks: ['rate==1'],
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
  const playerId = `k6-${__VU}-${__ITER}`;
  let res = null;

  // Hasta 3 intentos de conexion: el proxy de Render puede rechazar un
  // handshake suelto cuando llegan varios a la vez. Si los 3 fallan, el check
  // de conexion falla y el threshold "checks" detiene el pipeline.
  for (let intento = 1; intento <= CONNECT_ATTEMPTS; intento++) {
    res = ws.connect(BASE_URL, {}, function (socket) {
      // Movimientos enviados y aun no reflejados en el estado, en orden.
      const pendientes = [];
      let paso = 0;

      function vencer(ahora) {
        while (pendientes.length && ahora - pendientes[0].enviadoEn > CONFIRM_TIMEOUT_MS) {
          pendientes.shift();
          movesOmitted.add(1);
          moveFailed.add(true);
        }
      }

      socket.on('open', function () {
        const join = { type: 'join', playerId };
        if (JOIN_CODE) {
          join.code = JOIN_CODE;
        }
        socket.send(JSON.stringify(join));
      });

      socket.on('message', function (raw) {
        let msg;
        try {
          msg = JSON.parse(raw);
        } catch (e) {
          return;
        }
        if (msg.type !== 'state' || pendientes.length === 0) {
          return;
        }
        const yo = msg.players && msg.players[playerId];
        if (!yo) {
          return;
        }
        // El servidor aplica los movimientos en orden: el estado confirma el
        // primer pendiente cuyo destino coincide con mi posicion actual. Los
        // anteriores a el se dan por perdidos.
        const i = pendientes.findIndex((p) => p.x === yo.x && p.y === yo.y);
        if (i < 0) {
          return;
        }
        for (let k = 0; k < i; k++) {
          movesOmitted.add(1);
          moveFailed.add(true);
        }
        moveLatency.add(Date.now() - pendientes[i].enviadoEn);
        moveFailed.add(false);
        pendientes.splice(0, i + 1);
      });

      socket.setInterval(function () {
        const ahora = Date.now();
        vencer(ahora);
        const [x, y] = nextPosition(paso++);
        pendientes.push({ x, y, enviadoEn: ahora });
        socket.send(JSON.stringify({ type: 'move', playerId, x, y }));
      }, INTERVAL_MS);

      socket.setTimeout(function () {
        socket.close();
      }, durationToMs(DURATION));
    });
    if (res && res.status === 101) {
      break;
    }
    sleep(2);
  }

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
  out[`${SUMMARY_DIR}/summary-${LABEL}.json`] = JSON.stringify(data, null, 2);
  return out;
}
