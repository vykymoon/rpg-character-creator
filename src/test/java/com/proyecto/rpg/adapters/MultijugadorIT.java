package com.proyecto.rpg.adapters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.proyecto.rpg.adapters.client.GameClient;
import com.proyecto.rpg.adapters.server.GameServer;
import com.proyecto.rpg.adapters.server.ServerConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRUEBA DE INTEGRACION del multijugador LAN (ADR-001).
 *
 * Levanta el GameServer real en un puerto libre y conecta clientes
 * GameClient reales por WebSocket. Es caja negra: solo usa los mensajes
 * del protocolo (join y move del cliente; map y state del servidor), sin
 * tocar GameSessionService por dentro.
 *
 * Lo que cubre y las unitarias no pueden cubrir: que el estado que calcula
 * el servidor llegue de verdad a los demas jugadores. Una unitaria puede
 * demostrar que move() rechaza una casilla bloqueada, pero no que el
 * rechazo se refleje en la pantalla del otro jugador.
 *
 * Termina en IT y no en Test porque abre puertos de red: la ejecuta
 * Failsafe en "mvn verify", no Surefire en "mvn test".
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class MultijugadorIT {

    /** Margen para que un mensaje cruce la red local. Generoso a proposito. */
    private static final long ESPERA_MS = 5_000;

    private GameServer servidor;
    private int puerto;
    private final List<Cliente> abiertos = new ArrayList<>();

    @BeforeEach
    void levantarServidor() throws Exception {
        puerto = puertoLibre();
        servidor = new GameServer(ServerConfig.lan(puerto)).start();
        esperarAQueEscuche(puerto);
    }

    @AfterEach
    void cerrarTodo() throws Exception {
        for (Cliente c : abiertos) {
            if (c.ws.isOpen()) {
                c.ws.closeBlocking();
            }
        }
        abiertos.clear();
        servidor.stop();
    }

    // ------------------------------------------------------------------
    // Pruebas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Al unirse, el jugador recibe el mapa del servidor")
    void alUnirseElJugadorRecibeElMapa() throws Exception {
        Cliente p1 = conectar("p1");
        p1.unirse();

        JsonObject mapa = esperarMensaje(p1, "map");

        assertEquals(50, mapa.get("width").getAsInt());
        assertEquals(50, mapa.get("height").getAsInt());
        assertEquals(50, mapa.getAsJsonArray("rows").size());
    }

    @Test
    @DisplayName("Cuando un jugador se mueve, el otro recibe la posicion nueva")
    void cuandoUnJugadorSeMueveElOtroRecibeLaPosicionNueva() throws Exception {
        Cliente p1 = conectar("p1");
        Cliente p2 = conectar("p2");
        p1.unirse();
        p2.unirse();

        p1.recibidos.clear();
        p2.recibidos.clear();

        p1.mover(3, 3);

        JsonObject jugadores = esperarEstado(p2,
                players -> enPosicion(players, "p1", 3, 3),
                "que p2 vea a p1 en (3,3)");

        assertTrue(jugadores.has("p2"), "p2 deberia seguir en el estado");
    }

    @Test
    @DisplayName("Un movimiento invalido no cambia la posicion que ven los demas")
    void unMovimientoInvalidoNoCambiaLaPosicionQueVenLosDemas() throws Exception {
        Cliente p1 = conectar("p1");
        Cliente p2 = conectar("p2");
        p1.unirse();
        p2.unirse();

        p1.recibidos.clear();
        p2.recibidos.clear();

        // (0,0) es piedra del borde: el servidor debe descartarlo.
        p1.mover(0, 0);
        // Movimiento valido posterior, que sirve de marca para saber que el
        // servidor ya proceso el anterior.
        p1.mover(3, 2);

        esperarEstado(p2, players -> {
            JsonObject j = players.getAsJsonObject("p1");
            if (j == null) {
                return false;
            }
            int x = j.get("x").getAsInt();
            int y = j.get("y").getAsInt();
            assertFalse(x == 0 && y == 0,
                    "el servidor acepto un movimiento a una casilla bloqueada");
            return x == 3 && y == 2;
        }, "que p2 vea a p1 en (3,2) sin haber pasado por (0,0)");
    }

    @Test
    @DisplayName("Cuando un jugador se desconecta, desaparece del estado de los demas")
    void cuandoUnJugadorSeDesconectaDesapareceDelEstado() throws Exception {
        Cliente p1 = conectar("p1");
        Cliente p2 = conectar("p2");
        p1.unirse();
        p2.unirse();

        esperarEstado(p1, players -> players.has("p2"), "que p1 vea a p2 conectado");
        p1.recibidos.clear();

        p2.ws.closeBlocking();

        esperarEstado(p1, players -> !players.has("p2"),
                "que p2 desaparezca del estado que ve p1");
    }


    // ------------------------------------------------------------------
    // Corte 3: controles del modelo de amenazas (docs/seguridad.md)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A-01: un jugador no puede mover a otro enviando su playerId")
    void unJugadorNoPuedeMoverAOtro() throws Exception {
        Cliente p1 = conectar("p1");
        Cliente p2 = conectar("p2");
        p1.unirse();
        p2.unirse();
        esperarEstado(p1, players -> players.has("p2"), "que p1 vea a p2");

        // p2 intenta mover a p1 suplantando su identificador.
        p2.ws.send("{\"type\":\"move\",\"playerId\":\"p1\",\"x\":3,\"y\":3}");
        JsonObject error = esperarMensaje(p2, "error");
        assertEquals("player_id_mismatch", error.get("code").getAsString());

        // Marca: p1 se mueve de verdad a (3,2); p1 nunca debe haber pasado por (3,3).
        p1.mover(3, 2);
        esperarEstado(p2, players -> {
            JsonObject j = players.getAsJsonObject("p1");
            assertFalse(j.get("x").getAsInt() == 3 && j.get("y").getAsInt() == 3,
                    "el servidor dejo que p2 moviera a p1");
            return j.get("x").getAsInt() == 3 && j.get("y").getAsInt() == 2;
        }, "que p1 quede en (3,2) movido por si mismo");
    }

    @Test
    @DisplayName("A-03: una segunda conexion no puede apropiarse de un jugador ya unido")
    void unaSegundaConexionNoPuedeApropiarseDeUnJugador() throws Exception {
        Cliente original = conectar("p1");
        original.unirse();
        esperarMensaje(original, "map");

        Cliente impostor = conectar("p1");
        impostor.unirse();

        JsonObject error = esperarMensaje(impostor, "error");
        assertEquals("player_id_in_use", error.get("code").getAsString());
    }

    @Test
    @DisplayName("A-04: moverse sin haberse unido se rechaza y no crea jugador")
    void moverseSinUnirseSeRechaza() throws Exception {
        Cliente fantasma = conectar("fantasma");
        fantasma.mover(3, 3);

        JsonObject error = esperarMensaje(fantasma, "error");
        assertEquals("not_joined", error.get("code").getAsString());
    }

    @Test
    @DisplayName("A-02: con codigo de sala, un join sin el codigo correcto se rechaza")
    void conCodigoDeSalaElJoinSinCodigoSeRechaza() throws Exception {
        int otroPuerto = puertoLibre();
        GameServer protegido = new GameServer(
                new ServerConfig(otroPuerto, "sala-secreta", 8, 20, 4096, "test")).start();
        try {
            esperarAQueEscuche(otroPuerto);

            Cliente sinCodigo = new Cliente("intruso", otroPuerto);
            assertTrue(sinCodigo.ws.connectBlocking(ESPERA_MS, TimeUnit.MILLISECONDS));
            abiertos.add(sinCodigo);
            sinCodigo.unirse();
            JsonObject error = esperarMensaje(sinCodigo, "error");
            assertEquals("invalid_code", error.get("code").getAsString());

            Cliente conCodigo = new Cliente("invitado", otroPuerto);
            assertTrue(conCodigo.ws.connectBlocking(ESPERA_MS, TimeUnit.MILLISECONDS));
            abiertos.add(conCodigo);
            conCodigo.ws.send("{\"type\":\"join\",\"playerId\":\"invitado\",\"code\":\"sala-secreta\"}");
            JsonObject mapa = esperarMensaje(conCodigo, "map");
            assertEquals(50, mapa.get("width").getAsInt());
        } finally {
            protegido.stop();
        }
    }

    @Test
    @DisplayName("A-05: un cliente que inunda el servidor ve sus mensajes descartados")
    void unClienteQueInundaElServidorEsLimitado() throws Exception {
        int otroPuerto = puertoLibre();
        GameServer limitado = new GameServer(
                new ServerConfig(otroPuerto, "", 8, 5, 4096, "test")).start();
        try {
            esperarAQueEscuche(otroPuerto);
            Cliente ruidoso = new Cliente("ruidoso", otroPuerto);
            assertTrue(ruidoso.ws.connectBlocking(ESPERA_MS, TimeUnit.MILLISECONDS));
            abiertos.add(ruidoso);
            ruidoso.unirse();
            esperarMensaje(ruidoso, "map");
            for (int i = 0; i < 40; i++) {
                ruidoso.mover(2, 2);
            }
            long limite = System.currentTimeMillis() + ESPERA_MS;
            while (limitado.metrics().droppedCount("rate_limit") == 0
                    && System.currentTimeMillis() < limite) {
                Thread.sleep(50);
            }
            assertTrue(limitado.metrics().droppedCount("rate_limit") > 0,
                    "el limite de mensajes por segundo no descarto nada");
        } finally {
            limitado.stop();
        }
    }

    @Test
    @DisplayName("El healthcheck HTTP responde UP en el mismo puerto del WebSocket")
    void elHealthcheckRespondeUp() throws Exception {
        HttpResponse<String> r = get("/health");

        assertEquals(200, r.statusCode());
        JsonObject body = JsonParser.parseString(r.body()).getAsJsonObject();
        assertEquals("UP", body.get("status").getAsString());
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertFalse(r.headers().firstValue("Server").orElse("").contains("Jetty("),
                "el servidor no debe anunciar su version");
    }

    @Test
    @DisplayName("Las metricas de Prometheus cuentan los movimientos procesados")
    void lasMetricasCuentanLosMovimientos() throws Exception {
        Cliente p1 = conectar("p1");
        p1.unirse();
        esperarMensaje(p1, "map");
        p1.mover(3, 3);
        esperarEstado(p1, players -> enPosicion(players, "p1", 3, 3), "que p1 llegue a (3,3)");

        HttpResponse<String> r = get("/metrics");

        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("rpg_moves_total{result=\"accepted\"} 1"), r.body());
        assertTrue(r.body().contains("rpg_move_duration_seconds_bucket{le=\"0.15\"}"));
        assertNotNull(r.headers().firstValue("Content-Type").orElse(null));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + path)).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private static boolean enPosicion(JsonObject players, String id, int x, int y) {
        JsonObject j = players.getAsJsonObject(id);
        return j != null && j.get("x").getAsInt() == x && j.get("y").getAsInt() == y;
    }

    private Cliente conectar(String id) throws Exception {
        Cliente c = new Cliente(id, puerto);
        assertTrue(c.ws.connectBlocking(ESPERA_MS, TimeUnit.MILLISECONDS),
                "El cliente " + id + " no logro conectarse al servidor");
        abiertos.add(c);
        return c;
    }

    /** Espera el primer mensaje del tipo pedido, descartando los demas. */
    private JsonObject esperarMensaje(Cliente c, String tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (true) {
            long restante = limite - System.currentTimeMillis();
            if (restante <= 0) {
                break;
            }
            String crudo = c.recibidos.poll(restante, TimeUnit.MILLISECONDS);
            if (crudo == null) {
                break;
            }
            JsonObject o = JsonParser.parseString(crudo).getAsJsonObject();
            if (tipo.equals(o.get("type").getAsString())) {
                return o;
            }
        }
        throw new AssertionError(
                "No llego ningun mensaje de tipo '" + tipo + "' en " + ESPERA_MS + " ms");
    }

    /**
     * Espera un mensaje de estado que cumpla la condicion. Descarta los
     * estados anteriores, que pueden ser de antes del movimiento: el
     * servidor difunde el estado en cada evento, no solo en el que nos
     * interesa.
     */
    private JsonObject esperarEstado(Cliente c, Predicate<JsonObject> condicion, String queEsperaba)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (true) {
            long restante = limite - System.currentTimeMillis();
            if (restante <= 0) {
                break;
            }
            String crudo = c.recibidos.poll(restante, TimeUnit.MILLISECONDS);
            if (crudo == null) {
                break;
            }
            JsonObject o = JsonParser.parseString(crudo).getAsJsonObject();
            if (!"state".equals(o.get("type").getAsString())) {
                continue;
            }
            JsonObject players = o.getAsJsonObject("players");
            if (condicion.test(players)) {
                return players;
            }
        }
        throw new AssertionError("Tiempo agotado esperando " + queEsperaba);
    }

    private static int puertoLibre() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /**
     * start() del servidor WebSocket no bloquea, asi que hay que esperar a
     * que el puerto acepte conexiones antes de lanzar clientes.
     */
    private static void esperarAQueEscuche(int puerto) throws Exception {
        for (int intento = 0; intento < 100; intento++) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("localhost", puerto), 100);
                return;
            } catch (IOException noEscuchaTodavia) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("El servidor no arranco en el puerto " + puerto);
    }

    /** Cliente real del juego, con los mensajes recibidos en una cola. */
    private static final class Cliente {
        private final String id;
        private final GameClient ws;
        private final BlockingQueue<String> recibidos = new LinkedBlockingQueue<>();

        Cliente(String id, int puerto) throws Exception {
            this.id = id;
            this.ws = new GameClient(new URI("ws://localhost:" + puerto));
            this.ws.setOnState(recibidos::add);
        }

        void unirse() {
            ws.send("{\"type\":\"join\",\"playerId\":\"" + id + "\"}");
        }

        void mover(int x, int y) {
            ws.send("{\"type\":\"move\",\"playerId\":\"" + id
                    + "\",\"x\":" + x + ",\"y\":" + y + "}");
        }
    }
}
