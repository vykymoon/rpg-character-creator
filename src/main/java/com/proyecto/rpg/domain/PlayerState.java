package com.proyecto.rpg.domain;

import java.util.ArrayList;
import java.util.List;

public class PlayerState {
    public String playerId;
    public int x;
    public int y;
    public String name;
    public String race;
    public String characterClass;
    public List<String> items = new ArrayList<>();

    public PlayerState(String playerId, int x, int y) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
    }
}