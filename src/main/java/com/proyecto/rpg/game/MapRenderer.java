package com.proyecto.rpg.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.InputStream;

public final class MapRenderer {

    private static Image background;

    private MapRenderer() {}

    public static void draw(GraphicsContext gc, double width, double height) {
        Image bg = loadBackground();
        if (bg != null) {
            gc.drawImage(bg, 0, 0, width, height);
        } else {
            gc.clearRect(0, 0, width, height);
        }
    }

    private static Image loadBackground() {
        if (background != null) return background;
        try (InputStream in = MapRenderer.class.getResourceAsStream("/sprites/map/background.jpg")) {
            if (in != null) background = new Image(in);
        } catch (Exception e) {
            background = null;
        }
        return background;
    }
}