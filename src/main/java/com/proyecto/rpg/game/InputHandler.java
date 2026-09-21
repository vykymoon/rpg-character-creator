package com.proyecto.rpg.game;

import com.proyecto.rpg.game.command.Command;
import com.proyecto.rpg.game.command.MoveCommand;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Traduce teclas activas a Command (patrón Command). WASD se resuelve
 * cada frame del game loop; E es un evento puntual (toggle inventario).
 */
public class InputHandler {

    private final Set<KeyCode> pressed = new HashSet<>();
    private final Map<KeyCode, Command> movementCommands = new EnumMap<>(KeyCode.class);
    private Runnable onToggleInventory = () -> {};

    public InputHandler() {
        movementCommands.put(KeyCode.W, new MoveCommand(0, -1));
        movementCommands.put(KeyCode.S, new MoveCommand(0, 1));
        movementCommands.put(KeyCode.A, new MoveCommand(-1, 0));
        movementCommands.put(KeyCode.D, new MoveCommand(1, 0));
    }

    public void setOnToggleInventory(Runnable callback) {
        this.onToggleInventory = callback;
    }

    public void attach(Scene scene) {
        scene.setOnKeyPressed(event -> {
            pressed.add(event.getCode());
            if (event.getCode() == KeyCode.E) {
                onToggleInventory.run();
            }
        });
        scene.setOnKeyReleased(event -> pressed.remove(event.getCode()));
    }

    public void tick(PlayContext context, double deltaSeconds) {
        for (KeyCode code : pressed) {
            Command command = movementCommands.get(code);
            if (command != null) {
                command.execute(context, deltaSeconds);
            }
        }
    }
}