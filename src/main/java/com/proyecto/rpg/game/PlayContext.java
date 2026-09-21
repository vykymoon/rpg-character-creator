package com.proyecto.rpg.game;

public class PlayContext {
    private final Player player;
    private final GameMap map;
    private boolean inventoryOpen = false;

    public PlayContext(Player player, GameMap map) {
        this.player = player;
        this.map = map;
    }

    public Player getPlayer() { return player; }
    public GameMap getMap() { return map; }
    public boolean isInventoryOpen() { return inventoryOpen; }
    public void toggleInventory() { inventoryOpen = !inventoryOpen; }
}