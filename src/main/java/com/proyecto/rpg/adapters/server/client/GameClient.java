package com.proyecto.rpg.adapters.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class GameClient extends WebSocketClient {

    // Por defecto imprime en consola (ClientMain sigue funcionando igual)
    private volatile Consumer<String> onState =
            msg -> System.out.println("[client] estado recibido: " + msg);

    public GameClient(URI serverUri) {
        super(serverUri);
    }

    public void setOnState(Consumer<String> onState) {
        this.onState = onState;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[client] conectado al servidor");
    }

    @Override
    public void onMessage(String message) {
        onState.accept(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[client] conexion cerrada: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[client] error: " + ex.getMessage());
    }
}