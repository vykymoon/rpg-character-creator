package com.proyecto.rpg.model;

/**
 * Representa una clase de personaje (Guerrero, Mago, Pícaro, etc.) con
 * sus bonificaciones a los stats base de la raza.
 *
 * Las instancias NO se crean directamente: las produce
 * CharacterClassFactory a partir del catálogo declarado en
 * src/main/resources/data/classes.json.
 */
public class CharacterClass {

    private final String id;
    private final String name;
    private final String description;
    private final int bonusStrength;
    private final int bonusDexterity;
    private final int bonusIntelligence;
    private final int bonusVitality;

    public CharacterClass(String id,
                          String name,
                          String description,
                          int bonusStrength,
                          int bonusDexterity,
                          int bonusIntelligence,
                          int bonusVitality) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.bonusStrength = bonusStrength;
        this.bonusDexterity = bonusDexterity;
        this.bonusIntelligence = bonusIntelligence;
        this.bonusVitality = bonusVitality;
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

    public int getBonusStrength() {
        return bonusStrength;
    }

    public int getBonusDexterity() {
        return bonusDexterity;
    }

    public int getBonusIntelligence() {
        return bonusIntelligence;
    }

    public int getBonusVitality() {
        return bonusVitality;
    }

    /** Ver la nota en Race.equals(): la igualdad va por id, no por instancia. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CharacterClass)) {
            return false;
        }
        CharacterClass other = (CharacterClass) o;
        return id != null ? id.equals(other.id) : other.id == null && name != null && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (name != null ? name.hashCode() : 0);
    }

    @Override
    public String toString() {
        return name;
    }
}
