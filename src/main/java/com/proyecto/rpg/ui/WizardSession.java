package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.model.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado temporal del personaje mientras se recorre el wizard.
 * Cada pantalla (Step1, Step2, ...) lee y escribe aquí. Al llegar al
 * resumen final, estos datos se pasan al CharacterBuilder para
 * construir el Character definitivo.
 */
public class WizardSession {

    private String name;
    private Race race;
    private CharacterClass characterClass;
    private final List<Skill> selectedSkills = new ArrayList<>();
    private final List<Outfit> selectedOutfits = new ArrayList<>();

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

    public List<Skill> getSelectedSkills() {
        return selectedSkills;
    }

    public void setSelectedSkills(List<Skill> skills) {
        this.selectedSkills.clear();
        this.selectedSkills.addAll(skills);
    }

    public List<Outfit> getSelectedOutfits() {
        return selectedOutfits;
    }

    public void setSelectedOutfits(List<Outfit> outfits) {
        this.selectedOutfits.clear();
        this.selectedOutfits.addAll(outfits);
    }
}