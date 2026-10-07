package com.proyecto.rpg.application;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de la fuente de verdad del juego (ADR-001).
 *
 * No se usa Mockito: GameSessionService no tiene colaboradores externos
 * que valga la pena simular, y el mapa se genera con semilla fija, asi que
 * se prueba contra el mapa real. Un mock del mapa solo confirmaria que el
 * servicio llama a isValidMove, no que respete el terreno.
 *
 * Las aserciones se hacen sobre el JSON que el servidor emite, que es el
 * contrato que ven los clientes, y no sobre campos internos.
 */
class GameSessionServiceTest {

    private static final int SPAWN_X = 2;
    private static final int SPAWN_Y = 2;

    private GameSessionService session;

    @BeforeEach
    void setUp() {
        session = new GameSessionService();
    }

    private JsonObject jugadores() {
        return JsonParser.parseString(session.stateAsJson())
                .getAsJsonObject()
                .getAsJsonObject("players");
    }

    private JsonObject jugador(String id) {
        return jugadores().getAsJsonObject(id);
    }

    @Test
    @DisplayName("Al unirse, el jugador aparece en la zona de aparicion")
    void joinColocaAlJugadorEnLaZonaDeAparicion() {
        session.join("p1");

        JsonObject p1 = jugador("p1");
        assertEquals(SPAWN_X, p1.get("x").getAsInt());
        assertEquals(SPAWN_Y, p1.get("y").getAsInt());
    }

    @Test
    @DisplayName("Al unirse con personaje, el estado lleva sus datos")
    void joinGuardaLosDatosDelPersonaje() {
        session.join("p1", "Aerin", "elfo", "mago", List.of("Capa Elfica"));

        JsonObject p1 = jugador("p1");
        assertEquals("Aerin", p1.get("name").getAsString());
        assertEquals("elfo", p1.get("race").getAsString());
        assertEquals("mago", p1.get("characterClass").getAsString());
        assertEquals(1, p1.getAsJsonArray("items").size());
    }

    @Test
    @DisplayName("Un movimiento valido se aplica")
    void unMovimientoValidoSeAplica() {
        session.join("p1");

        assertTrue(session.move("p1", 3, 3));

        JsonObject p1 = jugador("p1");
        assertEquals(3, p1.get("x").getAsInt());
        assertEquals(3, p1.get("y").getAsInt());
    }

    @Test
    @DisplayName("Un movimiento a una casilla bloqueada se rechaza y no mueve al jugador")
    void unMovimientoABloqueadaSeRechaza() {
        session.join("p1");

        assertFalse(session.move("p1", 0, 0));

        JsonObject p1 = jugador("p1");
        assertEquals(SPAWN_X, p1.get("x").getAsInt());
        assertEquals(SPAWN_Y, p1.get("y").getAsInt());
    }

    @Test
    @DisplayName("Un movimiento fuera del mapa se rechaza")
    void unMovimientoFueraDelMapaSeRechaza() {
        session.join("p1");

        assertFalse(session.move("p1", -1, -1));
        assertFalse(session.move("p1", 50, 50));
    }

    @Test
    @DisplayName("Quitar a un jugador lo saca del estado")
    void removeQuitaAlJugadorDelEstado() {
        session.join("p1");
        session.join("p2");

        session.remove("p1");

        assertFalse(jugadores().has("p1"));
        assertTrue(jugadores().has("p2"));
    }

    @Test
    @DisplayName("Unirse dos veces no duplica al jugador ni lo devuelve al inicio")
    void joinDosVecesNoDuplicaAlJugador() {
        session.join("p1");
        session.move("p1", 4, 4);

        session.join("p1", "Aerin", "elfo", "mago", List.of());

        assertEquals(1, jugadores().size());
        JsonObject p1 = jugador("p1");
        assertEquals(4, p1.get("x").getAsInt(), "reconectarse no deberia teletransportar al jugador");
        assertEquals(4, p1.get("y").getAsInt());
    }

    @Test
    @DisplayName("El mensaje de mapa trae tipo, dimensiones y filas")
    void elMensajeDeMapaTraeDimensionesYFilas() {
        JsonObject mapa = JsonParser.parseString(session.mapAsJson()).getAsJsonObject();

        assertEquals("map", mapa.get("type").getAsString());
        assertEquals(50, mapa.get("width").getAsInt());
        assertEquals(50, mapa.get("height").getAsInt());
        assertEquals(50, mapa.getAsJsonArray("rows").size());
    }

    // ------------------------------------------------------------------
    // Comportamiento que un servidor autoritativo deberia tener y el
    // codigo todavia no cumple. Estan escritas y desactivadas a proposito:
    // documentan la deuda y se activan quitando @Disabled cuando se
    // corrija GameSessionService.move(). Ver docs/pruebas.md.
    // ------------------------------------------------------------------

    @Test
    @Disabled("Pendiente: move() acepta saltar a cualquier casilla caminable, no solo a una vecina")
    @DisplayName("El servidor rechaza saltos a casillas no vecinas")
    void elServidorRechazaSaltosNoAdyacentes() {
        session.join("p1");

        // (2,2) -> (4,4) son dos casillas en diagonal: un cliente modificado
        // podria teletransportarse por todo el mapa de un solo mensaje.
        assertFalse(session.move("p1", 4, 4));
    }

    @Test
    @Disabled("Pendiente: move() crea al jugador aunque nunca haya hecho join")
    @DisplayName("Moverse sin haberse unido no crea al jugador")
    void moverSinHaberseUnidoNoCreaAlJugador() {
        session.move("fantasma", 3, 3);

        assertFalse(jugadores().has("fantasma"));
    }
}
