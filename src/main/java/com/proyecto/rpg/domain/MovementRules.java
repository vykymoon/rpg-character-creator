package com.proyecto.rpg.domain;

public class MovementRules {

    private final GameMap map;

    public MovementRules(GameMap map) {
        this.map = map;
    }

    public boolean isValidMove(int x, int y) {
        return map.isWalkable(x, y);
    }
}