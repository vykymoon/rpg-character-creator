package com.proyecto.rpg.application;

import com.google.gson.Gson;
import com.proyecto.rpg.domain.GameMap;
import com.proyecto.rpg.domain.MovementRules;
import com.proyecto.rpg.domain.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Fuente de verdad del juego (servidor autoritativo, ADR-001). */
public class GameSessionService {

    private static final int SPAWN_X = 2;
    private static final int SPAWN_Y = 2;

    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();
    private final GameMap map;
    private final MovementRules movementRules;
    private final Gson gson = new Gson();

    /** Sin limite practico de jugadores: lo usan las pruebas y el modo LAN. */
    public static final int SIN_LIMITE = Integer.MAX_VALUE;

    private final int maxPlayers;

    public GameSessionService() {
        this(new GameMap(50, 50), SIN_LIMITE);
    }

    public GameSessionService(int maxPlayers) {
        this(new GameMap(50, 50), maxPlayers);
    }

    public GameSessionService(GameMap map) {
        this(map, SIN_LIMITE);
    }

    public GameSessionService(GameMap map, int maxPlayers) {
        if (maxPlayers < 1) {
            throw new IllegalArgumentException("maxPlayers debe ser al menos 1");
        }
        this.map = map;
        this.movementRules = new MovementRules(map);
        this.maxPlayers = maxPlayers;
    }

    public synchronized boolean join(String playerId) {
        return join(playerId, null, null, null, new ArrayList<>());
    }

    /**
     * Registra al jugador o actualiza sus datos si ya estaba.
     *
     * @return false si la partida esta llena y el jugador es nuevo.
     */
    public synchronized boolean join(String playerId, String name, String race,
                                     String characterClass, List<String> items) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        if (!players.containsKey(playerId) && players.size() >= maxPlayers) {
            return false;
        }
        PlayerState s = players.computeIfAbsent(playerId,
                id -> new PlayerState(id, SPAWN_X, SPAWN_Y));
        s.name = name;
        s.race = race;
        s.characterClass = characterClass;
        s.items = items != null ? items : new ArrayList<>();
        return true;
    }

    /**
     * Mueve al jugador si el paso es valido.
     *
     * Rechaza: jugadores que nunca hicieron join (antes se creaban solos),
     * destinos bloqueados o fuera del mapa, y saltos a casillas que no son
     * vecinas (antes un cliente modificado podia teletransportarse). Son los
     * dos pendientes que el Corte 2 dejo documentados con pruebas @Disabled.
     */
    public synchronized boolean move(String playerId, int x, int y) {
        PlayerState state = players.get(playerId);
        if (state == null) {
            return false;
        }
        if (!movementRules.isValidStep(state.x, state.y, x, y)) {
            return false;
        }
        state.x = x;
        state.y = y;
        return true;
    }

    public synchronized boolean isJoined(String playerId) {
        return players.containsKey(playerId);
    }

    public synchronized int playerCount() {
        return players.size();
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public synchronized void remove(String playerId) {
        players.remove(playerId);
    }

    public synchronized String stateAsJson() {
        return gson.toJson(new StatePayload(players));
    }

    public String mapAsJson() {
        return gson.toJson(new MapPayload(map.width, map.height, map.rows()));
    }

    private static class StatePayload {
        final String type = "state";
        final Map<String, PlayerState> players;

        StatePayload(Map<String, PlayerState> players) {
            this.players = players;
        }
    }

    private static class MapPayload {
        final String type = "map";
        final int width;
        final int height;
        final String[] rows;

        MapPayload(int width, int height, String[] rows) {
            this.width = width;
            this.height = height;
            this.rows = rows;
        }
    }
}