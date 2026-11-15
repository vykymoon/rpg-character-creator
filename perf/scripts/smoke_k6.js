import ws from 'k6/ws';
import http from 'k6/http';
import { check, fail } from 'k6';

// Prueba de humo del flujo principal (Corte 3, cd.yml).
//
// Corre despues de cada despliegue y antes de la prueba de carga. Verifica,
// contra el entorno desplegado, el recorrido que hace un jugador real:
//   1. GET /health responde 200, status UP y la version recien desplegada;
//   2. abre el WebSocket y hace join (con el codigo de sala);
//   3. recibe el mapa de 50x50;
//   4. se mueve a una casilla vecina y recibe el estado con su nueva posicion.
// Si cualquiera falla, el threshold de checks hace que k6 termine con error y
// el pipeline se detiene.
//
// Uso:
//   k6 run --env HTTP_URL=https://<servicio> --env WS_URL=wss://<servicio>/ws \
//          --env JOIN_CODE=<codigo> --env EXPECTED_VERSION=<sha> perf/scripts/smoke_k6.js

const HTTP_URL = __ENV.HTTP_URL || 'http://localhost:8887';
const WS_URL = __ENV.WS_URL || 'ws://localhost:8887/ws';
const JOIN_CODE = __ENV.JOIN_CODE || '';
const EXPECTED_VERSION = __ENV.EXPECTED_VERSION || '';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

export default function () {
  const health = http.get(`${HTTP_URL}/health`);
  const body = health.status === 200 ? health.json() : {};
  check(health, {
    'health responde 200': (r) => r.status === 200,
    'health dice UP': () => body.status === 'UP',
    'responde la version desplegada': () => !EXPECTED_VERSION || body.version === EXPECTED_VERSION,
  });

  const playerId = `humo-${Date.now() % 100000}`;
  let recibioMapa = false;
  let seMovio = false;
  let error = null;

  const res = ws.connect(WS_URL, {}, function (socket) {
    socket.on('open', function () {
      const join = { type: 'join', playerId };
      if (JOIN_CODE) {
        join.code = JOIN_CODE;
      }
      socket.send(JSON.stringify(join));
    });

    socket.on('message', function (raw) {
      const msg = JSON.parse(raw);
      if (msg.type === 'error') {
        error = msg.code;
        socket.close();
      } else if (msg.type === 'map') {
        recibioMapa = msg.width === 50 && msg.height === 50 && msg.rows.length === 50;
        // Paso a una casilla vecina de la zona de aparicion (2,2).
        socket.send(JSON.stringify({ type: 'move', x: 3, y: 2 }));
      } else if (msg.type === 'state') {
        const yo = msg.players && msg.players[playerId];
        if (yo && yo.x === 3 && yo.y === 2) {
          seMovio = true;
          socket.close();
        }
      }
    });

    socket.setTimeout(function () {
      socket.close();
    }, 15000);
  });

  check(res, { 'websocket conectado (101)': (r) => r && r.status === 101 });
  check(null, {
    'el servidor no respondio con error': () => error === null,
    'recibe el mapa al unirse': () => recibioMapa,
    'el movimiento se refleja en el estado': () => seMovio,
  });
  if (error) {
    fail(`el servidor respondio con error: ${error}`);
  }
}
