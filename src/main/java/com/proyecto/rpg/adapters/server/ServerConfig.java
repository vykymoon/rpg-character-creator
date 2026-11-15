package com.proyecto.rpg.adapters.server;

import java.util.Map;

/**
 * Configuracion del servidor leida de variables de entorno (Corte 3).
 *
 * Ningun valor sensible vive en el codigo ni en la imagen: el codigo de sala
 * llega por JOIN_CODE desde el archivo .env (Compose), los secretos del
 * proveedor de nube o un Secret de Kubernetes. Los nombres son los mismos de
 * .env.example y de docs/implementacion.md.
 *
 * @param port              puerto HTTP y WebSocket. Render y otros proveedores lo
 *                          inyectan como PORT; en local se usa SERVER_PORT.
 * @param joinCode          codigo de sala que exige el join. Vacio = sala abierta
 *                          (modo LAN del Corte 2 y pruebas).
 * @param maxPlayers        jugadores simultaneos. El valor por defecto sale de las
 *                          mediciones del Corte 2: 32 es el ultimo punto sano.
 * @param maxMessagesPerSec mensajes por segundo y conexion antes de descartar.
 * @param maxMessageBytes   tamano maximo de un mensaje WebSocket.
 * @param version           version desplegada (el SHA del commit en la imagen).
 */
public record ServerConfig(int port,
                           String joinCode,
                           int maxPlayers,
                           int maxMessagesPerSec,
                           int maxMessageBytes,
                           String version) {

    public static final int DEFAULT_PORT = 8887;

    public ServerConfig {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Puerto fuera de rango: " + port);
        }
        if (maxPlayers < 1) {
            throw new IllegalArgumentException("MAX_PLAYERS debe ser al menos 1");
        }
        if (maxMessagesPerSec < 1) {
            throw new IllegalArgumentException("MAX_MESSAGES_PER_SECOND debe ser al menos 1");
        }
        if (maxMessageBytes < 256) {
            throw new IllegalArgumentException("MAX_MESSAGE_BYTES debe ser al menos 256");
        }
        joinCode = joinCode == null ? "" : joinCode.trim();
        version = version == null || version.isBlank() ? "dev" : version.trim();
    }

    /** Configuracion abierta, sin codigo de sala: modo LAN y pruebas. */
    public static ServerConfig lan(int port) {
        return new ServerConfig(port, "", 64, 50, 4096, "dev");
    }

    public static ServerConfig fromEnvironment() {
        return fromMap(System.getenv());
    }

    static ServerConfig fromMap(Map<String, String> env) {
        int port = intValue(env, "PORT", intValue(env, "SERVER_PORT", DEFAULT_PORT));
        return new ServerConfig(
                port,
                env.getOrDefault("JOIN_CODE", ""),
                intValue(env, "MAX_PLAYERS", 32),
                intValue(env, "MAX_MESSAGES_PER_SECOND", 20),
                intValue(env, "MAX_MESSAGE_BYTES", 4096),
                env.getOrDefault("APP_VERSION", "dev"));
    }

    public boolean requiresJoinCode() {
        return !joinCode.isEmpty();
    }

    private static int intValue(Map<String, String> env, String key, int fallback) {
        String raw = env.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La variable " + key + " debe ser un numero: " + raw, e);
        }
    }
}
