package com.proyecto.rpg.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Zona rectangular del mapa (la sombra al final del camino) que dispara
 * un encuentro cuando el jugador entra en ella.
 */
public class EncounterZone {

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public EncounterZone(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(Player player) {
        double centerX = player.getX() + Player.SIZE / 2;
        double centerY = player.getY() + Player.SIZE / 2;
        return centerX >= x && centerX <= x + width
                && centerY >= y && centerY <= y + height;
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillOval(x - 6, y - 6, width + 12, height + 12);
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillOval(x, y, width, height);
    }
}