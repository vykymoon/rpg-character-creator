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

    public GameSessionService() {
        this(new GameMap(50, 50));
    }

    public GameSessionService(GameMap map) {
        this.map = map;
        this.movementRules = new MovementRules(map);
    }

    public synchronized void join(String playerId) {
        join(playerId, null, null, null, new ArrayList<>());
    }

    public synchronized void join(String playerId, String name, String race,
                                  String characterClass, List<String> items) {
        PlayerState s = players.computeIfAbsent(playerId,
                id -> new PlayerState(id, SPAWN_X, SPAWN_Y));
        s.name = name;
        s.race = race;
        s.characterClass = characterClass;
        s.items = items;
    }

    public synchronized boolean move(String playerId, int x, int y) {
        if (!movementRules.isValidMove(x, y)) {
            return false;
        }
        PlayerState state = players.get(playerId);
        if (state == null) {
            players.put(playerId, new PlayerState(playerId, x, y));
        } else {
            state.x = x;
            state.y = y;
        }
        return true;
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