package com.proyecto.rpg.adapters.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.proyecto.rpg.application.GameSessionService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer extends WebSocketServer {

    private final GameSessionService sessionService = new GameSessionService();
    private final Gson gson = new Gson();
    private final Map<WebSocket, String> playerIdByConnection = new ConcurrentHashMap<>();

    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[server] cliente conectado: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String playerId = playerIdByConnection.remove(conn);
        if (playerId != null) {
            sessionService.remove(playerId);
            broadcastState();
            System.out.println("[server] jugador desconectado: " + playerId);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.get("type").getAsString();
            String playerId = json.get("playerId").getAsString();

            switch (type) {
                case "join" -> {
                    playerIdByConnection.put(conn, playerId);
                    List<String> items = new ArrayList<>();
                    if (json.has("items") && json.get("items").isJsonArray()) {
                        for (JsonElement e : json.getAsJsonArray("items")) {
                            if (e.isJsonPrimitive()) items.add(e.getAsString());
                        }
                    }
                    sessionService.join(playerId, str(json, "name"), str(json, "race"),
                            str(json, "characterClass"), items);
                    conn.send(sessionService.mapAsJson());
                    broadcastState();
                }
                case "move" -> {
                    int x = json.get("x").getAsInt();
                    int y = json.get("y").getAsInt();
                    sessionService.move(playerId, x, y);
                    broadcastState();
                }
                default -> System.err.println("[server] tipo de mensaje desconocido: " + type);
            }
        } catch (Exception e) {
            System.err.println("[server] mensaje invalido (" + e.getMessage() + "): " + message);
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[server] error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[server] GameServer autoritativo escuchando en puerto " + getPort());
    }

    private void broadcastState() {
        String json = sessionService.stateAsJson();
        for (WebSocket conn : getConnections()) {
            conn.send(json);
        }
    }
}