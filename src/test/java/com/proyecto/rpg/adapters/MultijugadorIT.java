package com.proyecto.rpg.adapters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.proyecto.rpg.adapters.client.GameClient;
import com.proyecto.rpg.adapters.server.GameServer;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        servidor = new GameServer(puerto);
        // Sin esto, dos pruebas seguidas pueden chocar con el puerto en TIME_WAIT.
        servidor.setReuseAddr(true);
        servidor.start();
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
        servidor.stop(1_000);
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
