package com.proyecto.rpg.game;

import com.proyecto.rpg.model.Character;

public class Player {
    public static final double SIZE = 40;

    private final Character character;
    private double x;
    private double y;
    private final double speed = 180; // px/segundo

    public Player(Character character, double startX, double startY) {
        this.character = character;
        this.x = startX;
        this.y = startY;
    }

    public Character getCharacter() { return character; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }

    public void moveBy(double dx, double dy, GameMap map) {
        this.x = map.clampX(x + dx, SIZE);
        this.y = map.clampY(y + dy, SIZE);
    }
}