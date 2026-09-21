package com.proyecto.rpg.game;

public class GameMap {
    private final double width;
    private final double height;

    public GameMap(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public double clampX(double x, double entitySize) {
        return Math.max(0, Math.min(width - entitySize, x));
    }

    public double clampY(double y, double entitySize) {
        return Math.max(0, Math.min(height - entitySize, y));
    }
}