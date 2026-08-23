package com.proyecto.rpg.model;

/**
 * Representa una raza jugable (Elfo, Humano, Orco, etc.)
 * junto con sus stats base. Instancias creadas vía RaceFactory.
 */
public class Race {

    private final String name;
    private final int baseStrength;
    private final int baseDexterity;
    private final int baseIntelligence;
    private final int baseVitality;

    public Race(String name, int baseStrength, int baseDexterity, int baseIntelligence, int baseVitality) {
        this.name = name;
        this.baseStrength = baseStrength;
        this.baseDexterity = baseDexterity;
        this.baseIntelligence = baseIntelligence;
        this.baseVitality = baseVitality;
    }

    public String getName() {
        return name;
    }

    public int getBaseStrength() {
        return baseStrength;
    }

    public int getBaseDexterity() {
        return baseDexterity;
    }

    public int getBaseIntelligence() {
        return baseIntelligence;
    }

    public int getBaseVitality() {
        return baseVitality;
    }

    @Override
    public String toString() {
        return name + " [STR:" + baseStrength + " DEX:" + baseDexterity
                + " INT:" + baseIntelligence + " VIT:" + baseVitality + "]";
    }
}
