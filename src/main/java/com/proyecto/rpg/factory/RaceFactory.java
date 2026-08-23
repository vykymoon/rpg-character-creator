package com.proyecto.rpg.factory;

import com.proyecto.rpg.model.Race;

/**
 * Patrón Factory (Factory Method).
 * Centraliza la creación de razas con sus stats base, evitando que el
 * resto de la app tenga que conocer los valores numéricos de cada raza.
 *
 * Persona A: aquí puedes agregar más razas o pasar a leer los stats
 * desde src/main/resources/data/races.json si prefieren data-driven.
 */
public class RaceFactory {

    public enum RaceType {
        HUMANO, ELFO, ORCO, ENANO
    }

    public static Race createRace(RaceType type) {
        switch (type) {
            case HUMANO:
                return new Race("Humano", 5, 5, 5, 5);
            case ELFO:
                return new Race("Elfo", 3, 7, 6, 4);
            case ORCO:
                return new Race("Orco", 8, 3, 2, 7);
            case ENANO:
                return new Race("Enano", 6, 3, 4, 7);
            default:
                throw new IllegalArgumentException("Raza no soportada: " + type);
        }
    }
}
