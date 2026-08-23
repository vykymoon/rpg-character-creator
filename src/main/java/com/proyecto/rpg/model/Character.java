package com.proyecto.rpg.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Personaje jugable. Se construye normalmente a través de CharacterBuilder,
 * y puede clonarse mediante CharacterPrototype (patrón Prototype), ya que
 * implementa Cloneable.
 */
public class Character implements Cloneable {

    private String id;
    private String name;
    private Race race;
    private CharacterClass characterClass;
    private List<Skill> skills;
    private List<Outfit> outfits;

    // Stats derivados (raza + clase)
    private int strength;
    private int dexterity;
    private int intelligence;
    private int vitality;

    public Character() {
        this.id = UUID.randomUUID().toString();
        this.skills = new ArrayList<>();
        this.outfits = new ArrayList<>();
    }

    // --- Getters / Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(CharacterClass characterClass) {
        this.characterClass = characterClass;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    public void addSkill(Skill skill) {
        this.skills.add(skill);
    }

    public List<Outfit> getOutfits() {
        return outfits;
    }

    public void setOutfits(List<Outfit> outfits) {
        this.outfits = outfits;
    }

    public void addOutfit(Outfit outfit) {
        this.outfits.add(outfit);
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getVitality() {
        return vitality;
    }

    public void setVitality(int vitality) {
        this.vitality = vitality;
    }

    /**
     * Recalcula stats finales sumando base de raza + bonus de clase.
     * Se llama normalmente al final del CharacterBuilder.
     */
    public void recalculateStats() {
        if (race == null || characterClass == null) {
            return;
        }
        this.strength = race.getBaseStrength() + characterClass.getBonusStrength();
        this.dexterity = race.getBaseDexterity() + characterClass.getBonusDexterity();
        this.intelligence = race.getBaseIntelligence() + characterClass.getBonusIntelligence();
        this.vitality = race.getBaseVitality() + characterClass.getBonusVitality();
    }

    /**
     * Deep copy: listas nuevas (no comparten referencia con el original),
     * pero Race/CharacterClass/Skill/Outfit se comparten porque son
     * catálogos inmutables (no tiene sentido clonarlos).
     */
    @Override
    public Character clone() {
        try {
            Character copy = (Character) super.clone();
            copy.id = UUID.randomUUID().toString(); // un clon es un personaje nuevo
            copy.skills = new ArrayList<>(this.skills);
            copy.outfits = new ArrayList<>(this.outfits);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Character debe ser Cloneable", e);
        }
    }

    @Override
    public String toString() {
        return "Character{" +
                "name='" + name + '\'' +
                ", race=" + (race != null ? race.getName() : "null") +
                ", class=" + (characterClass != null ? characterClass.getName() : "null") +
                ", skills=" + skills.size() +
                ", outfits=" + outfits.size() +
                ", STR=" + strength + " DEX=" + dexterity + " INT=" + intelligence + " VIT=" + vitality +
                '}';
    }
}
