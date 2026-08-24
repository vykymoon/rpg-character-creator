package com.proyecto.rpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una pieza de vestuario/equipamiento visual del personaje.
 * Igual que Skill, puede restringirse por raza y/o clase.
 *
 * Las instancias las produce OutfitFactory a partir de
 * src/main/resources/data/outfits.json.
 */
public class Outfit {

    private final String id;
    private final String name;
    private final String slot; // ej: "armadura", "casco", "capa", "escudo", "botas"
    private final String spriteRef;
    private final List<String> allowedRaces;
    private final List<String> allowedClasses;

    /** Pieza suelta sin restricción. */
    public Outfit(String name, String slot, String spriteRef) {
        this(null, name, slot, spriteRef, null, null);
    }

    public Outfit(String id,
                  String name,
                  String slot,
                  String spriteRef,
                  List<String> allowedRaces,
                  List<String> allowedClasses) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.spriteRef = spriteRef;
        this.allowedRaces = allowedRaces != null ? new ArrayList<>(allowedRaces) : new ArrayList<>();
        this.allowedClasses = allowedClasses != null ? new ArrayList<>(allowedClasses) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlot() {
        return slot;
    }

    public String getSpriteRef() {
        return spriteRef;
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

    public boolean isAvailableFor(Race race, CharacterClass characterClass) {
        return CatalogRestriction.allowsRace(allowedRaces, race)
                && CatalogRestriction.allowsClass(allowedClasses, characterClass);
    }

    public String getRequirementLabel() {
        return RequirementLabel.of(allowedRaces, allowedClasses);
    }

    @Override
    public String toString() {
        return name + " [" + slot + "]";
    }
}
