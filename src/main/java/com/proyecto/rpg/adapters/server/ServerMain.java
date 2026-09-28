package com.proyecto.rpg.adapters.server;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8887;
        GameServer server = new GameServer(port);
        server.start();
        System.out.println("Servidor host levantado en ws://localhost:" + port);
        System.out.println("Presiona Ctrl+C para detenerlo.");
    }
}