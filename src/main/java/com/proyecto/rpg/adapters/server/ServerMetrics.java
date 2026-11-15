package com.proyecto.rpg.adapters.server;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.IntSupplier;

/**
 * Metricas del servidor en formato de texto de Prometheus, expuestas en
 * GET /metrics (bonificacion de observabilidad del Corte 3).
 *
 * Se escribieron a mano, sin librerias, para no sumar dependencias (y CVE)
 * a la imagen: el formato de exposicion es texto plano y estable. Las tres
 * familias que alimentan el tablero de Grafana son las del SLO del ADR-001:
 * <ul>
 *   <li>latencia: rpg_move_duration_seconds (histograma, p95 con histogram_quantile);</li>
 *   <li>tasa de error: rpg_moves_total{result="error"} y los mensajes descartados;</li>
 *   <li>throughput: rate(rpg_moves_total[1m]).</li>
 * </ul>
 */
public final class ServerMetrics {

    /** Limites de las cubetas en segundos. 0.15 es el SLO (p95 < 150 ms). */
    static final double[] BUCKETS = {0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.15, 0.25, 0.5, 1.0, 2.5};

    private final AtomicLongArray bucketCounts = new AtomicLongArray(BUCKETS.length);
    private final AtomicLong durationCount = new AtomicLong();
    private final AtomicLong durationSumNanos = new AtomicLong();

    private final Map<String, AtomicLong> moves = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> joins = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> dropped = new ConcurrentHashMap<>();
    private final AtomicLong connectionsOpened = new AtomicLong();
    private final AtomicLong openConnections = new AtomicLong();

    private final IntSupplier playersOnline;
    private final String version;

    public ServerMetrics(IntSupplier playersOnline, String version) {
        this.playersOnline = playersOnline;
        this.version = version;
    }

    public void observeMove(String result, long nanos) {
        counter(moves, result).incrementAndGet();
        double seconds = nanos / 1_000_000_000.0;
        for (int i = 0; i < BUCKETS.length; i++) {
            if (seconds <= BUCKETS[i]) {
                bucketCounts.incrementAndGet(i);
            }
        }
        durationCount.incrementAndGet();
        durationSumNanos.addAndGet(nanos);
    }

    public void join(String result) {
        counter(joins, result).incrementAndGet();
    }

    public void dropped(String reason) {
        counter(dropped, reason).incrementAndGet();
    }

    public void connectionOpened() {
        connectionsOpened.incrementAndGet();
        openConnections.incrementAndGet();
    }

    public void connectionClosed() {
        openConnections.decrementAndGet();
    }

    public long movesCount(String result) {
        AtomicLong c = moves.get(result);
        return c == null ? 0 : c.get();
    }

    public long droppedCount(String reason) {
        AtomicLong c = dropped.get(reason);
        return c == null ? 0 : c.get();
    }

    private static AtomicLong counter(Map<String, AtomicLong> family, String label) {
        return family.computeIfAbsent(label, k -> new AtomicLong());
    }

    /** Texto en formato de exposicion de Prometheus 0.0.4. */
    public String scrape() {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("# HELP rpg_build_info Version desplegada del servidor.\n");
        sb.append("# TYPE rpg_build_info gauge\n");
        sb.append("rpg_build_info{version=\"").append(escape(version)).append("\"} 1\n");

        sb.append("# HELP rpg_players_online Jugadores unidos a la partida.\n");
        sb.append("# TYPE rpg_players_online gauge\n");
        sb.append("rpg_players_online ").append(playersOnline.getAsInt()).append('\n');

        sb.append("# HELP rpg_connections_open Conexiones WebSocket abiertas.\n");
        sb.append("# TYPE rpg_connections_open gauge\n");
        sb.append("rpg_connections_open ").append(openConnections.get()).append('\n');

        sb.append("# HELP rpg_connections_total Conexiones WebSocket aceptadas desde el arranque.\n");
        sb.append("# TYPE rpg_connections_total counter\n");
        sb.append("rpg_connections_total ").append(connectionsOpened.get()).append('\n');

        family(sb, "rpg_moves_total", "Movimientos procesados por resultado.", "result", moves);
        family(sb, "rpg_joins_total", "Intentos de union por resultado.", "result", joins);
        family(sb, "rpg_messages_dropped_total", "Mensajes descartados por motivo.", "reason", dropped);

        sb.append("# HELP rpg_move_duration_seconds Tiempo de procesar un move y difundir el estado.\n");
        sb.append("# TYPE rpg_move_duration_seconds histogram\n");
        for (int i = 0; i < BUCKETS.length; i++) {
            sb.append("rpg_move_duration_seconds_bucket{le=\"").append(BUCKETS[i]).append("\"} ")
                    .append(bucketCounts.get(i)).append('\n');
        }
        long count = durationCount.get();
        sb.append("rpg_move_duration_seconds_bucket{le=\"+Inf\"} ").append(count).append('\n');
        sb.append("rpg_move_duration_seconds_sum ").append(durationSumNanos.get() / 1_000_000_000.0).append('\n');
        sb.append("rpg_move_duration_seconds_count ").append(count).append('\n');
        return sb.toString();
    }

    private static void family(StringBuilder sb, String name, String help, String label,
                               Map<String, AtomicLong> values) {
        sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
        sb.append("# TYPE ").append(name).append(" counter\n");
        for (Map.Entry<String, AtomicLong> e : new TreeMap<>(values).entrySet()) {
            sb.append(name).append('{').append(label).append("=\"").append(escape(e.getKey())).append("\"} ")
                    .append(e.getValue().get()).append('\n');
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
