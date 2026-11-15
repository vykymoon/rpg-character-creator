package com.proyecto.rpg.adapters.server;

/**
 * Punto de entrada del servidor de partida.
 *
 * Toda la configuracion llega por variables de entorno (ver ServerConfig y
 * .env.example). Se conserva el argumento opcional del Corte 2 para el puerto:
 * {@code mvn compile exec:java@server -Dexec.args=9000}.
 */
public class ServerMain {

    public static void main(String[] args) {
        ServerConfig config = ServerConfig.fromEnvironment();
        if (args.length > 0) {
            config = new ServerConfig(Integer.parseInt(args[0]), config.joinCode(), config.maxPlayers(),
                    config.maxMessagesPerSec(), config.maxMessageBytes(), config.version());
        }
        GameServer server = new GameServer(config).start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "server-shutdown"));
        System.out.println("Servidor de partida escuchando en ws://localhost:" + server.port()
                + "  (salud: http://localhost:" + server.port() + "/health)");
    }
}
