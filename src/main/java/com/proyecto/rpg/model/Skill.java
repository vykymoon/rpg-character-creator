package com.proyecto.rpg.model;

/**
 * Representa una habilidad que puede tener un personaje.
 */
public class Skill {

    private final String name;
    private final String description;
    private final int manaCost;

    public Skill(String name, String description, int manaCost) {
        this.name = name;
        this.description = description;
        this.manaCost = manaCost;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getManaCost() {
        return manaCost;
    }

    @Override
    public String toString() {
        return name + " (costo: " + manaCost + " maná)";
    }
}
