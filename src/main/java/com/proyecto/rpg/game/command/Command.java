package com.proyecto.rpg.game.command;

import com.proyecto.rpg.game.PlayContext;

public interface Command {
    void execute(PlayContext context, double deltaSeconds);
}