package com.proyecto.rpg.ui;

import javafx.application.Application;

/**
 * Punto de entrada del cliente empaquetado en GitHub Releases.
 *
 * Un .jar con JavaFX adentro no puede arrancar directamente una clase que
 * extienda Application ("JavaFX runtime components are missing"); por eso
 * este lanzador, que no extiende Application, decide cual abrir:
 * <pre>
 *   java -jar rpg-cliente.jar          creador de personajes (MainApp)
 *   java -jar rpg-cliente.jar --jugar  partida multijugador (GameApp)
 * </pre>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        boolean jugar = false;
        for (String a : args) {
            if ("--jugar".equals(a) || "--game".equals(a)) {
                jugar = true;
            }
        }
        if (jugar) {
            Application.launch(GameApp.class, args);
        } else {
            Application.launch(MainApp.class, args);
        }
    }
}
