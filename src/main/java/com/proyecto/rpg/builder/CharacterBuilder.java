package com.proyecto.rpg.builder;

import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.model.Skill;

/**
 * Patrón Builder.
 * Permite construir un Character paso a paso: nombre -> raza -> clase ->
 * habilidades -> vestuario. Pensado para conectarse 1 a 1 con las
 * pantallas del wizard de creación en JavaFX (cada pantalla del wizard
 * llama a un método distinto del builder).
 *
 * Persona B: este es tu archivo principal. El wizard visual en
 * resources/fxml/wizard.fxml + WizardController deben ir llamando estos
 * métodos en orden, uno por cada paso/pantalla.
 */
public class CharacterBuilder {

    private final Character character;

    public CharacterBuilder() {
        this.character = new Character();
    }

    public CharacterBuilder withName(String name) {
        character.setName(name);
        return this;
    }

    public CharacterBuilder withRace(Race race) {
        character.setRace(race);
        return this;
    }

    public CharacterBuilder withClass(CharacterClass characterClass) {
        character.setCharacterClass(characterClass);
        return this;
    }

    public CharacterBuilder addSkill(Skill skill) {
        character.addSkill(skill);
        return this;
    }

    public CharacterBuilder addOutfit(Outfit outfit) {
        character.addOutfit(outfit);
        return this;
    }

    /**
     * Finaliza la construcción: recalcula stats (raza + clase) y
     * devuelve el personaje listo para guardar.
     */
    public Character build() {
        if (character.getName() == null || character.getName().isBlank()) {
            throw new IllegalStateException("El personaje necesita un nombre antes de construirse.");
        }
        if (character.getRace() == null) {
            throw new IllegalStateException("El personaje necesita una raza antes de construirse.");
        }
        if (character.getCharacterClass() == null) {
            throw new IllegalStateException("El personaje necesita una clase antes de construirse.");
        }
        character.recalculateStats();
        return character;
    }
}
