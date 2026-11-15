package com.proyecto.rpg.builder;

import com.proyecto.rpg.factory.CharacterClassFactory;
import com.proyecto.rpg.factory.RaceFactory;
import com.proyecto.rpg.factory.SkillFactory;
import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Race;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas del CharacterBuilder: la ultima barrera contra personajes invalidos.
 * Era el "hueco mas grande de la suite" que declaro docs/pruebas.md en el Corte 2.
 */
class CharacterBuilderTest {

    private final Race elfo = RaceFactory.createRace("elfo");
    private final CharacterClass mago = CharacterClassFactory.createClass("mago");
    private final CharacterClass guerrero = CharacterClassFactory.createClass("guerrero");

    @Test
    @DisplayName("Construye un personaje valido y calcula sus estadisticas")
    void construyePersonajeValido() {
        Character c = new CharacterBuilder()
                .withName("Aerin")
                .withRace(elfo)
                .withClass(mago)
                .addSkill(SkillFactory.createSkill("bola_de_fuego"))
                .build();

        assertEquals("Aerin", c.getName());
        assertEquals(elfo.getBaseStrength() + mago.getBonusStrength(), c.getStrength());
        assertEquals(elfo.getBaseIntelligence() + mago.getBonusIntelligence(), c.getIntelligence());
        assertEquals(1, c.getSkills().size());
    }

    @Test
    @DisplayName("Rechaza una habilidad que no corresponde a la clase")
    void rechazaHabilidadDeOtraClase() {
        CharacterBuilder b = new CharacterBuilder().withName("Brom").withRace(elfo).withClass(guerrero);

        assertThrows(IllegalStateException.class,
                () -> b.addSkill(SkillFactory.createSkill("bola_de_fuego")));
    }

    @Test
    @DisplayName("No construye un personaje sin nombre, raza o clase")
    void noConstruyeIncompleto() {
        assertThrows(IllegalStateException.class,
                () -> new CharacterBuilder().withRace(elfo).withClass(mago).build());
        assertThrows(IllegalStateException.class,
                () -> new CharacterBuilder().withName(" ").withRace(elfo).withClass(mago).build());
        assertThrows(IllegalStateException.class,
                () -> new CharacterBuilder().withName("Sin raza").withClass(mago).build());
        assertThrows(IllegalStateException.class,
                () -> new CharacterBuilder().withName("Sin clase").withRace(elfo).build());
    }
}
