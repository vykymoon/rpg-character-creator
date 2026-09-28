package com.proyecto.rpg.adapters.client;

import java.net.URI;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "ws://localhost:8887";
        String playerId = args.length > 1 ? args[1] : "player-" + (System.currentTimeMillis() % 1000);

        com.proyecto.rpg.adapters.client.GameClient client = new com.proyecto.rpg.adapters.client.GameClient(new URI(url));
        client.connectBlocking();
        client.send("{\"type\":\"join\",\"playerId\":\"" + playerId + "\"}");

        System.out.println("Escribe: x y   (ej: 3 4) para moverte. 'salir' para terminar.");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.equalsIgnoreCase("salir")) break;
            String[] parts = line.split("\\s+");
            if (parts.length == 2) {
                client.send("{\"type\":\"move\",\"playerId\":\"" + playerId + "\",\"x\":" + parts[0] + ",\"y\":" + parts[1] + "}");
            }
        }
        client.closeBlocking();
    }
}