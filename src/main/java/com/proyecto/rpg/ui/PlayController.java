package com.proyecto.rpg.ui;

import com.proyecto.rpg.game.EncounterZone;
import com.proyecto.rpg.game.GameMap;
import com.proyecto.rpg.game.InputHandler;
import com.proyecto.rpg.game.MapRenderer;
import com.proyecto.rpg.game.PlayContext;
import com.proyecto.rpg.game.Player;
import com.proyecto.rpg.model.Character;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class PlayController {

    @FXML private StackPane root;
    @FXML private Canvas canvas;

    private final Character character;
    private final InputHandler inputHandler = new InputHandler();
    private PlayContext context;
    private InventoryOverlay inventoryOverlay;
    private EncounterZone encounterZone;
    private AnimationTimer loop;
    private long lastNanos = -1;
    private boolean encounterTriggered = false;

    public PlayController(Character character) {
        this.character = character;
    }

    @FXML
    public void initialize() {
        GameMap map = new GameMap(canvas.getWidth(), canvas.getHeight());
        Player player = new Player(character, map.getWidth() / 2, map.getHeight() / 2);
        context = new PlayContext(player, map);

        // Zona de encuentro al final del camino (salida inferior del mapa).
        double zoneWidth = 90;
        double zoneHeight = 70;
        encounterZone = new EncounterZone(
                map.getWidth() / 2 - zoneWidth / 2,
                map.getHeight() - zoneHeight - 20,
                zoneWidth,
                zoneHeight);

        inventoryOverlay = new InventoryOverlay(character);
        root.getChildren().add(inventoryOverlay);

        inputHandler.setOnToggleInventory(() -> {
            context.toggleInventory();
            inventoryOverlay.toggle();
        });

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                inputHandler.attach(newScene);
                newScene.getRoot().requestFocus();
            }
        });

        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos < 0) {
                    lastNanos = now;
                    return;
                }
                double deltaSeconds = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;

                inputHandler.tick(context, deltaSeconds);
                render();
                checkEncounter();
            }
        };
        loop.start();
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        MapRenderer.draw(gc, canvas.getWidth(), canvas.getHeight());
        encounterZone.draw(gc);

        Player player = context.getPlayer();
        Image avatar = RaceAvatarLoader.load(character.getRace());
        if (avatar != null) {
            gc.drawImage(avatar, player.getX(), player.getY(), Player.SIZE, Player.SIZE);
        }
    }

    private void checkEncounter() {
        if (encounterTriggered) return;
        if (encounterZone.contains(context.getPlayer())) {
            encounterTriggered = true;
            loop.stop();
            Stage stage = (Stage) canvas.getScene().getWindow();
            SceneNavigator.goTo(stage, "/fxml/battle.fxml", new BattleController(character));
        }
    }

    @FXML
    public void onBack(ActionEvent event) {
        loop.stop();
        SceneNavigator.goTo(event, "/fxml/gallery.fxml", new GalleryController());
    }
}