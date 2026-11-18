package com.proyecto.rpg.adapters.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.proyecto.rpg.application.GameSessionService;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Adaptador de entrada del servidor autoritativo (ADR-001, ADR-006).
 *
 * En el Corte 2 era un WebSocketServer de Java-WebSocket, que solo habla
 * WebSocket. Para desplegarlo hacia falta HTTP en el mismo puerto: el
 * healthcheck de Compose, Kubernetes y Render, la prueba de humo, el escaneo
 * DAST y las metricas de Prometheus. Por eso se cambio a Javalin (Jetty 12),
 * que sirve las dos cosas en un solo puerto. Solo cambio este adaptador: el
 * caso de uso GameSessionService y el dominio no se tocaron, que es lo que la
 * arquitectura de puertos y adaptadores promete y ArchitectureTest verifica.
 *
 * Rutas:
 * <ul>
 *   <li>WebSocket en "/" (compatible con los clientes del Corte 2) y en "/ws";</li>
 *   <li>GET /health: estado y version desplegada;</li>
 *   <li>GET /metrics: metricas en formato Prometheus;</li>
 *   <li>GET /: pagina informativa minima.</li>
 * </ul>
 *
 * Controles de seguridad del modelo STRIDE (docs/seguridad.md):
 * <ul>
 *   <li>A-01 suplantacion: el playerId queda atado a la conexion en el join; un
 *       move con el playerId de otro jugador se rechaza.</li>
 *   <li>A-02 autenticacion: codigo de sala JOIN_CODE, comparado en tiempo constante.</li>
 *   <li>A-03 elevacion: dos conexiones no pueden reclamar el mismo jugador.</li>
 *   <li>A-05 denegacion de servicio: limite de mensajes por segundo, tamano maximo
 *       de mensaje y cupo de jugadores.</li>
 *   <li>A-06 divulgacion: los errores no devuelven el mensaje recibido ni trazas,
 *       y las respuestas HTTP llevan cabeceras de seguridad.</li>
 *   <li>A-07 repudio: cada evento queda en el log con jugador, IP y resultado.</li>
 * </ul>
 */
public class GameServer {

    /** Codigo de cierre WebSocket para "politica violada" (RFC 6455). */
    static final int CLOSE_POLICY = 1008;
    /** Codigo de aplicacion para "no autorizado" (rango 4000-4999). */
    static final int CLOSE_UNAUTHORIZED = 4401;

    private static final Pattern PLAYER_ID = Pattern.compile("^[\\p{L}\\p{N} _#.\\-]{1,40}$");
    private static final int MAX_ITEMS = 32;
    private static final int MAX_TEXT = 60;

    private final ServerConfig config;
    private final GameSessionService sessionService;
    private final ServerMetrics metrics;
    private final Gson gson = new Gson();
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Javalin app;

    public GameServer(int port) {
        this(ServerConfig.lan(port));
    }

    public GameServer(ServerConfig config) {
        this.config = config;
        this.sessionService = new GameSessionService(config.maxPlayers());
        this.metrics = new ServerMetrics(sessionService::playerCount, config.version());
        this.app = Javalin.create(this::configure);
    }

    private void configure(JavalinConfig cfg) {
        cfg.startup.showJavalinBanner = false;
        cfg.http.maxRequestSize = 16_384L;
        cfg.jetty.modifyHttpConfiguration(http -> http.setSendServerVersion(false));
        cfg.jetty.modifyWebSocketServletFactory(factory -> {
            factory.setMaxTextMessageSize(config.maxMessageBytes());
            factory.setMaxBinaryMessageSize(1024);
            factory.setIdleTimeout(Duration.ofMinutes(10));
        });

        cfg.routes.after(GameServer::securityHeaders);
        cfg.routes.get("/health", this::health);
        cfg.routes.get("/metrics", this::metricsEndpoint);
        cfg.routes.get("/", ctx -> ctx.contentType("text/plain; charset=utf-8")
                .result("RPG Character Creator - servidor de partida (Corte 3). WebSocket en /ws, estado en /health.\n"));

        cfg.routes.ws("/", this::websocket);
        cfg.routes.ws("/ws", this::websocket);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    public GameServer start() {
        app.start(config.port());
        log("server_started", "port=" + port() + " version=" + config.version()
                + " joinCode=" + (config.requiresJoinCode() ? "on" : "off")
                + " maxPlayers=" + config.maxPlayers());
        return this;
    }

    public void stop() {
        app.stop();
    }

    /** Puerto real; util cuando se arranca con puerto 0 en las pruebas. */
    public int port() {
        return app.port();
    }

    public ServerMetrics metrics() {
        return metrics;
    }

    // ------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------

    private static void securityHeaders(Context ctx) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        ctx.header("Referrer-Policy", "no-referrer");
        ctx.header("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        ctx.header("Cross-Origin-Resource-Policy", "same-origin");
        ctx.header("Cross-Origin-Opener-Policy", "same-origin");
        ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
        ctx.header("Pragma", "no-cache");
        ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }

    private void health(Context ctx) {
        JsonObject body = new JsonObject();
        body.addProperty("status", "UP");
        body.addProperty("version", config.version());
        body.addProperty("players", sessionService.playerCount());
        body.addProperty("maxPlayers", config.maxPlayers());
        ctx.contentType("application/json").result(gson.toJson(body));
    }

    private void metricsEndpoint(Context ctx) {
        ctx.contentType("text/plain; version=0.0.4; charset=utf-8").result(metrics.scrape());
    }

    // ------------------------------------------------------------------
    // WebSocket
    // ------------------------------------------------------------------

    private void websocket(WsConfig ws) {
        ws.onConnect(this::onConnect);
        ws.onMessage(this::onMessage);
        ws.onClose(this::onClose);
        ws.onError(this::onError);
    }

    private void onConnect(WsConnectContext ctx) {
        connections.put(ctx.sessionId(), new Connection(ctx, remoteAddress(ctx)));
        metrics.connectionOpened();
        log("connect", "ip=" + remoteAddress(ctx));
    }

    private void onClose(WsCloseContext ctx) {
        Connection c = connections.remove(ctx.sessionId());
        if (c == null) {
            return;
        }
        metrics.connectionClosed();
        if (c.playerId != null) {
            sessionService.remove(c.playerId);
            log("leave", "player=" + c.playerId + " ip=" + c.ip);
            broadcastState();
        }
    }

    private void onError(WsErrorContext ctx) {
        Throwable error = ctx.error();
        log("ws_error", "reason=" + (error == null ? "desconocido" : error.getClass().getSimpleName()));
    }

    private void onMessage(WsMessageContext ctx) {
        Connection c = connections.get(ctx.sessionId());
        if (c == null) {
            return;
        }
        if (!c.allowMessage(System.nanoTime(), config.maxMessagesPerSec())) {
            metrics.dropped("rate_limit");
            if (c.rateViolations > config.maxMessagesPerSec() * 10L) {
                log("rate_limit_close", "player=" + c.playerId + " ip=" + c.ip);
                ctx.closeSession(CLOSE_POLICY, "demasiados mensajes");
            }
            return;
        }

        JsonObject json = parse(ctx.message());
        if (json == null) {
            metrics.dropped("invalid");
            sendError(ctx, "invalid_message");
            return;
        }
        String type = text(json, "type");
        if ("join".equals(type)) {
            handleJoin(ctx, c, json);
        } else if ("move".equals(type)) {
            handleMove(ctx, c, json);
        } else {
            metrics.dropped("unknown_type");
            sendError(ctx, "unknown_type");
        }
    }

    private void handleJoin(WsMessageContext ctx, Connection c, JsonObject json) {
        if (config.requiresJoinCode() && !codeMatches(text(json, "code"))) {
            metrics.join("bad_code");
            log("join_rejected", "reason=bad_code ip=" + c.ip);
            sendError(ctx, "invalid_code");
            ctx.closeSession(CLOSE_UNAUTHORIZED, "codigo de sala invalido");
            return;
        }

        String playerId = text(json, "playerId");
        if (playerId == null || !PLAYER_ID.matcher(playerId).matches()) {
            metrics.join("invalid_player_id");
            sendError(ctx, "invalid_player_id");
            return;
        }
        if (c.playerId != null && !c.playerId.equals(playerId)) {
            metrics.join("already_joined");
            sendError(ctx, "already_joined");
            return;
        }
        if (ownedByAnotherConnection(playerId, c)) {
            metrics.join("player_id_in_use");
            log("join_rejected", "reason=player_id_in_use player=" + playerId + " ip=" + c.ip);
            sendError(ctx, "player_id_in_use");
            return;
        }

        boolean accepted = sessionService.join(playerId,
                limit(text(json, "name")), limit(text(json, "race")),
                limit(text(json, "characterClass")), items(json));
        if (!accepted) {
            metrics.join("server_full");
            log("join_rejected", "reason=server_full player=" + playerId + " ip=" + c.ip);
            sendError(ctx, "server_full");
            return;
        }

        c.playerId = playerId;
        metrics.join("ok");
        log("join", "player=" + playerId + " ip=" + c.ip);
        ctx.send(sessionService.mapAsJson());
        broadcastState();
    }

    private void handleMove(WsMessageContext ctx, Connection c, JsonObject json) {
        long start = System.nanoTime();
        if (c.playerId == null) {
            metrics.observeMove("not_joined", System.nanoTime() - start);
            sendError(ctx, "not_joined");
            return;
        }
        String claimed = text(json, "playerId");
        if (claimed != null && !claimed.equals(c.playerId)) {
            metrics.observeMove("spoofed", System.nanoTime() - start);
            log("move_rejected", "reason=spoofed player=" + c.playerId + " claimed=" + claimed + " ip=" + c.ip);
            sendError(ctx, "player_id_mismatch");
            return;
        }
        Integer x = integer(json, "x");
        Integer y = integer(json, "y");
        if (x == null || y == null) {
            metrics.observeMove("error", System.nanoTime() - start);
            sendError(ctx, "invalid_message");
            return;
        }
        boolean accepted = sessionService.move(c.playerId, x, y);
        broadcastState();
        metrics.observeMove(accepted ? "accepted" : "rejected", System.nanoTime() - start);
    }

    private boolean ownedByAnotherConnection(String playerId, Connection self) {
        for (Connection other : connections.values()) {
            if (other != self && playerId.equals(other.playerId)) {
                return true;
            }
        }
        return false;
    }

    private boolean codeMatches(String received) {
        if (received == null) {
            return false;
        }
        return MessageDigest.isEqual(
                config.joinCode().getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }

    /** Difunde el estado completo a los jugadores unidos (modelo del ADR-001). */
    private synchronized void broadcastState() {
        String state = sessionService.stateAsJson();
        for (Connection c : connections.values()) {
            if (c.playerId != null && c.ctx.session.isOpen()) {
                c.ctx.send(state);
            }
        }
    }

    private void sendError(WsContext ctx, String code) {
        JsonObject err = new JsonObject();
        err.addProperty("type", "error");
        err.addProperty("code", code);
        if (ctx.session.isOpen()) {
            ctx.send(gson.toJson(err));
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private JsonObject parse(String raw) {
        try {
            JsonElement e = gson.fromJson(raw, JsonElement.class);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static Integer integer(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            return e.getAsInt();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String limit(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) : s;
    }

    private static List<String> items(JsonObject json) {
        List<String> items = new ArrayList<>();
        JsonElement raw = json.get("items");
        if (raw != null && raw.isJsonArray()) {
            for (JsonElement e : raw.getAsJsonArray()) {
                if (items.size() >= MAX_ITEMS) {
                    break;
                }
                if (e.isJsonPrimitive()) {
                    items.add(limit(e.getAsString()));
                }
            }
        }
        return items;
    }

    private static String remoteAddress(WsContext ctx) {
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        Object address = ctx.session.getRemoteSocketAddress();
        return address == null ? "desconocida" : address.toString();
    }

    private static void log(String event, String details) {
        System.out.println("[server] event=" + event + " " + details);
    }

    /** Estado de una conexion: a que jugador quedo atada y su ventana de mensajes. */
    private static final class Connection {
        final WsContext ctx;
        final String ip;
        volatile String playerId;
        long windowStart;
        int windowCount;
        long rateViolations;

        Connection(WsContext ctx, String ip) {
            this.ctx = ctx;
            this.ip = ip;
        }

        synchronized boolean allowMessage(long nowNanos, int maxPerSecond) {
            if (nowNanos - windowStart >= 1_000_000_000L) {
                windowStart = nowNanos;
                windowCount = 0;
            }
            windowCount++;
            if (windowCount > maxPerSecond) {
                rateViolations++;
                return false;
            }
            return true;
        }
    }
}
