package com.proyecto.rpg.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.proyecto.rpg.adapters.client.GameClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Vista JavaFX del multijugador LAN: mapa con terreno + personaje del JSON. */
public class GameApp extends Application {

    private static final int GRID = 50;
    private static final int CELL = 14;

    private record PV(int x, int y, String name, String cls) {}

    private final Gson gson = new Gson();
    private final Map<String, PV> players = new ConcurrentHashMap<>();
    private volatile String[] mapRows;
    private GameClient client;
    private String playerId;
    private Canvas canvas;
    private javafx.scene.image.Image mapImage;

    @Override
    public void start(Stage stage) throws Exception {
        CharacterLoader.Info hero = pickCharacter();
        if (hero == null) { Platform.exit(); return; }

        String url = ask("Servidor", "URL del servidor:", "ws://localhost:8887");
        if (url == null) { Platform.exit(); return; }

        playerId = hero.name + "#" + (System.currentTimeMillis() % 1000);

        client = new GameClient(new URI(url));
        client.setOnState(this::handleMessage);
        if (!client.connectBlocking(3, TimeUnit.SECONDS)) {
            new Alert(Alert.AlertType.ERROR, "No se pudo conectar a " + url).showAndWait();
            Platform.exit();
            return;
        }

        JsonObject join = new JsonObject();
        join.addProperty("type", "join");
        join.addProperty("playerId", playerId);
        join.addProperty("name", hero.name);
        join.addProperty("race", hero.race);
        join.addProperty("characterClass", hero.characterClass);
        JsonArray items = new JsonArray();
        hero.items.forEach(items::add);
        join.add("items", items);
        client.send(gson.toJson(join));

        canvas = new Canvas(GRID * CELL, GRID * CELL);
        BorderPane root = new BorderPane(canvas);
        root.setRight(buildSidebar(hero));

        Scene scene = new Scene(root);
        // filter: evita que las flechas muevan la seleccion de las listas
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode k = e.getCode();
            boolean used = true;
            if (k == KeyCode.UP || k == KeyCode.W) move(0, -1);
            else if (k == KeyCode.DOWN || k == KeyCode.S) move(0, 1);
            else if (k == KeyCode.LEFT || k == KeyCode.A) move(-1, 0);
            else if (k == KeyCode.RIGHT || k == KeyCode.D) move(1, 0);
            else used = false;
            if (used) e.consume();
        });

        stage.setTitle("RPG LAN - " + hero.name);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> { client.close(); Platform.exit(); });
        stage.show();

        // --- CÓDIGO A PRUEBA DE BALAS ---
        String[] posiblesRutas = {
                "/map/background_multiplayer.jpg",
                "/battle/map/background_multiplayer.jpg",
                "/background_multiplayer.jpg"
        };

        for (String ruta : posiblesRutas) {
            java.net.URL imageUrl = getClass().getResource(ruta);
            if (imageUrl != null) {
                mapImage = new javafx.scene.image.Image(imageUrl.toExternalForm());
                System.out.println("¡ÉXITO! Imagen encontrada en el proyecto: " + ruta);
                break;
            }
        }

        // Plan de emergencia: Si no la encuentra en el proyecto, la jala de tus Descargas directo
        if (mapImage == null) {
            System.err.println("No se encontró en resources, forzando carga desde Descargas...");
            java.io.File file = new java.io.File("C:\\Users\\AsusZenbook\\Downloads\\background_multiplayer.jpg");
            if (file.exists()) {
                mapImage = new javafx.scene.image.Image(file.toURI().toString());
                System.out.println("¡ÉXITO! Imagen cargada desde tus Descargas.");
            } else {
                System.err.println("ERROR CRÍTICO: Tampoco existe en Descargas.");
            }
        }
        // --------------------------------

        draw();
    }

    // ---------- UI lateral ----------

    private VBox buildSidebar(CharacterLoader.Info hero) {
        VBox side = new VBox(6);
        side.setPadding(new Insets(10));
        side.setPrefWidth(220);
        side.setStyle("-fx-background-color:#2a3036;");

        Label name = new Label(hero.name);
        name.setFont(Font.font("System", FontWeight.BOLD, 18));
        name.setStyle("-fx-text-fill:white;");
        Label rc = new Label(hero.race + " / " + hero.characterClass);
        rc.setStyle("-fx-text-fill:#bbb;");
        side.getChildren().addAll(name, rc);

        if (!hero.stats.isEmpty()) {
            side.getChildren().add(heading("Stats"));
            hero.stats.forEach((k, v) -> {
                Label l = new Label(k + ": " + v);
                l.setStyle("-fx-text-fill:white;");
                side.getChildren().add(l);
            });
        }

        side.getChildren().add(heading("Items"));
        side.getChildren().add(list(hero.items));
        side.getChildren().add(heading("Habilidades"));
        side.getChildren().add(list(hero.skills));

        Label help = new Label("Mover: flechas / WASD");
        help.setStyle("-fx-text-fill:#888;");
        side.getChildren().add(help);
        return side;
    }

    private Label heading(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setStyle("-fx-text-fill:gold;");
        l.setPadding(new Insets(8, 0, 0, 0));
        return l;
    }

    private ListView<String> list(List<String> data) {
        ListView<String> lv = new ListView<>(FXCollections.observableArrayList(data));
        lv.setPrefHeight(120);
        lv.setFocusTraversable(false);
        return lv;
    }

    // ---------- Seleccion de personaje ----------

    private CharacterLoader.Info pickCharacter() {
        List<CharacterLoader.Info> list = new ArrayList<>();
        Path p = CharacterLoader.findDefault();
        if (p == null) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Selecciona characters.json");
            File f = fc.showOpenDialog(null);
            if (f != null) p = f.toPath();
        }
        if (p != null) {
            try {
                list = CharacterLoader.load(p);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error leyendo JSON: " + ex.getMessage()).showAndWait();
            }
        }
        if (list.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "No hay personajes guardados. Crea uno en el wizard primero.").showAndWait();
            return null;
        }
        ChoiceDialog<CharacterLoader.Info> d = new ChoiceDialog<>(list.get(0), list);
        d.setTitle("Personaje");
        d.setHeaderText("Elige tu personaje");
        return d.showAndWait().orElse(null);
    }

    // ---------- Red ----------

    private void handleMessage(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            String type = root.get("type").getAsString();

            if ("map".equals(type)) {
                JsonArray rows = root.getAsJsonArray("rows");
                String[] m = new String[rows.size()];
                for (int i = 0; i < m.length; i++) m[i] = rows.get(i).getAsString();
                mapRows = m;
                Platform.runLater(this::draw);

            } else if ("state".equals(type)) {
                JsonObject ps = root.getAsJsonObject("players");
                Map<String, PV> fresh = new ConcurrentHashMap<>();
                for (String id : ps.keySet()) {
                    JsonObject p = ps.getAsJsonObject(id);
                    String n = p.has("name") ? p.get("name").getAsString() : id;
                    String c = p.has("characterClass") ? p.get("characterClass").getAsString() : "";
                    fresh.put(id, new PV(p.get("x").getAsInt(), p.get("y").getAsInt(), n, c));
                }
                Platform.runLater(() -> {
                    players.clear();
                    players.putAll(fresh);
                    draw();
                });
            }
        } catch (Exception ex) {
            System.err.println("[ui] mensaje invalido: " + ex.getMessage());
        }
    }

    private void move(int dx, int dy) {
        PV me = players.get(playerId);
        if (me == null) return;
        int nx = me.x() + dx, ny = me.y() + dy;
        if (!walkable(nx, ny)) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "move");
        msg.addProperty("playerId", playerId);
        msg.addProperty("x", nx);
        msg.addProperty("y", ny);
        client.send(gson.toJson(msg));
    }

    private boolean walkable(int x, int y) {
        String[] m = mapRows;
        if (m == null || y < 0 || y >= m.length || x < 0 || x >= m[y].length()) return false;
        char c = m[y].charAt(x);
        return c == '.' || c == '=';
    }

    // ---------- Dibujo ----------

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        if (mapImage != null && !mapImage.isError()) {
            g.drawImage(mapImage, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            g.setFill(Color.web("#1e2327"));
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }

        for (Map.Entry<String, PV> e : players.entrySet()) {
            PV p = e.getValue();
            boolean mine = e.getKey().equals(playerId);
            double hue = Math.abs(p.cls().hashCode()) % 360;
            g.setFill(Color.hsb(hue, 0.6, 0.95));
            g.fillOval(p.x() * CELL + 1, p.y() * CELL + 1, CELL - 2, CELL - 2);
            if (mine) {
                g.setStroke(Color.GOLD);
                g.setLineWidth(2);
                g.strokeOval(p.x() * CELL, p.y() * CELL, CELL, CELL);
                g.setLineWidth(1);
            }
            g.setFill(Color.WHITE);
            g.fillText(p.name(), p.x() * CELL - 4, p.y() * CELL - 2);
        }
    }

    private String ask(String title, String header, String def) {
        TextInputDialog d = new TextInputDialog(def);
        d.setTitle(title);
        d.setHeaderText(header);
        Optional<String> r = d.showAndWait();
        return r.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}