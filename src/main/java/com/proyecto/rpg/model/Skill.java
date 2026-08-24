package com.proyecto.rpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una habilidad que puede tener un personaje.
 *
 * Puede estar restringida a ciertas razas y/o clases. Lista vacía o null
 * = disponible para todos, así los ítems sin restricción se declaran sin
 * ruido extra.
 *
 * Las instancias las produce SkillFactory a partir de
 * src/main/resources/data/skills.json.
 */
public class Skill {

    private final String id;
    private final String name;
    private final String description;
    private final int manaCost;
    private final List<String> allowedRaces;
    private final List<String> allowedClasses;

    /** Habilidad suelta sin restricción (usada por las plantillas del Prototype). */
    public Skill(String name, String description, int manaCost) {
        this(null, name, description, manaCost, null, null);
    }

    public Skill(String id,
                 String name,
                 String description,
                 int manaCost,
                 List<String> allowedRaces,
                 List<String> allowedClasses) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.manaCost = manaCost;
        this.allowedRaces = allowedRaces != null ? new ArrayList<>(allowedRaces) : new ArrayList<>();
        this.allowedClasses = allowedClasses != null ? new ArrayList<>(allowedClasses) : new ArrayList<>();
    }

    public String getId() {
        return id;
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

    public List<String> getAllowedRaces() {
        return allowedRaces != null ? List.copyOf(allowedRaces) : List.of();
    }

    public List<String> getAllowedClasses() {
        return allowedClasses != null ? List.copyOf(allowedClasses) : List.of();
    }

    public boolean isAvailableFor(Race race) {
        return CatalogRestriction.allowsRace(allowedRaces, race);
    }

    /** Disponible solo si pasa la restricción de raza Y la de clase. */
    public boolean isAvailableFor(Race race, CharacterClass characterClass) {
        return CatalogRestriction.allowsRace(allowedRaces, race)
                && CatalogRestriction.allowsClass(allowedClasses, characterClass);
    }

    /** Texto corto del requisito, o null si no tiene ninguno. */
    public String getRequirementLabel() {
        return RequirementLabel.of(allowedRaces, allowedClasses);
    }

    @Override
    public String toString() {
        return name + " (costo: " + manaCost + " maná)";
    }
}
