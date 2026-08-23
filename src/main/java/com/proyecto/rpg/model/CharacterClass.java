package com.proyecto.rpg.model;

/**
 * Representa una clase de personaje (Guerrero, Mago, Pícaro, etc.)
 * con sus bonificaciones. Instancias creadas vía CharacterClassFactory.
 */
public class CharacterClass {

    private final String name;
    private final int bonusStrength;
    private final int bonusDexterity;
    private final int bonusIntelligence;
    private final int bonusVitality;

    public CharacterClass(String name, int bonusStrength, int bonusDexterity, int bonusIntelligence, int bonusVitality) {
        this.name = name;
        this.bonusStrength = bonusStrength;
        this.bonusDexterity = bonusDexterity;
        this.bonusIntelligence = bonusIntelligence;
        this.bonusVitality = bonusVitality;
    }

    public String getName() {
        return name;
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

    @Override
    public String toString() {
        return name;
    }
}
