package com.proyecto.rpg.model;

/**
 * Representa una raza jugable (Elfo, Humano, Orco, etc.) junto con sus
 * stats base.
 *
 * Las instancias NO se crean directamente: las produce RaceFactory a
 * partir del catálogo declarado en src/main/resources/data/races.json.
 * El campo {@code id} es la clave estable que usa el catálogo (no cambia
 * aunque se traduzca o se reescriba el {@code name}).
 */
public class Race {

    private final String id;
    private final String name;
    private final String description;
    private final int baseStrength;
    private final int baseDexterity;
    private final int baseIntelligence;
    private final int baseVitality;

    public Race(String id,
                String name,
                String description,
                int baseStrength,
                int baseDexterity,
                int baseIntelligence,
                int baseVitality) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseStrength = baseStrength;
        this.baseDexterity = baseDexterity;
        this.baseIntelligence = baseIntelligence;
        this.baseVitality = baseVitality;
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

    /**
     * Dos razas son la misma si comparten id. Necesario para que el
     * ComboBox del wizard vuelva a seleccionar la raza correcta cuando el
     * objeto viene de otra instancia (por ejemplo al releer un personaje
     * guardado desde JSON, que no es la misma instancia del catálogo).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Race)) {
            return false;
        }
        Race other = (Race) o;
        return id != null ? id.equals(other.id) : other.id == null && name != null && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (name != null ? name.hashCode() : 0);
    }

    @Override
    public String toString() {
        return name + " [STR:" + baseStrength + " DEX:" + baseDexterity
                + " INT:" + baseIntelligence + " VIT:" + baseVitality + "]";
    }
}
