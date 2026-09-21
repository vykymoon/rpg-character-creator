package com.proyecto.rpg.game.command;

import com.proyecto.rpg.game.PlayContext;
import com.proyecto.rpg.game.Player;

public class MoveCommand implements Command {
    private final double dx;
    private final double dy;

    public MoveCommand(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void execute(PlayContext context, double deltaSeconds) {
        if (context.isInventoryOpen()) return;
        Player player = context.getPlayer();
        double distance = player.getSpeed() * deltaSeconds;
        player.moveBy(dx * distance, dy * distance, context.getMap());
    }
}