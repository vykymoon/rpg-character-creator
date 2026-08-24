package com.proyecto.rpg.prototype;

import com.proyecto.rpg.builder.CharacterBuilder;
import com.proyecto.rpg.factory.CharacterClassFactory;
import com.proyecto.rpg.factory.RaceFactory;
import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.Skill;

import java.util.HashMap;
import java.util.Map;

/**
 * Patrón Prototype.
 * Mantiene un registro de personajes/NPCs "plantilla" ya armados
 * (ej: "Bandido Base", "Guardia Real") que se pueden clonar y modificar
 * sin tener que reconstruirlos desde cero con el Builder cada vez.
 *
 * Uso típico:
 *   CharacterPrototype registry = CharacterPrototype.withDefaultTemplates();
 *   Character nuevoBandido = registry.clone("bandido");
 *   nuevoBandido.setName("Bandido #2");
 */
public class CharacterPrototype {

    private final Map<String, Character> templates = new HashMap<>();

    public void registerTemplate(String key, Character template) {
        templates.put(key, template);
    }

    public Character clone(String key) {
        Character template = templates.get(key);
        if (template == null) {
            throw new IllegalArgumentException("No existe plantilla registrada con la clave: " + key);
        }
        return template.clone();
    }

    public boolean hasTemplate(String key) {
        return templates.containsKey(key);
    }

    public Map<String, Character> getTemplates() {
        return templates;
    }

    /**
     * Fábrica de conveniencia: crea un registro ya precargado con NPCs
     * de ejemplo, construidos usando el propio CharacterBuilder (Builder
     * y Prototype trabajando juntos: primero se construye la plantilla
     * paso a paso, luego queda disponible para clonar).
     */
    public static CharacterPrototype withDefaultTemplates() {
        CharacterPrototype registry = new CharacterPrototype();

        Character bandido = new CharacterBuilder()
                .withName("Bandido Base")
                .withRace(RaceFactory.createRace("humano"))
                .withClass(CharacterClassFactory.createClass("picaro"))
                .addSkill(new Skill("Sigilo", "Reduce la probabilidad de ser detectado", 5))
                .build();
        registry.registerTemplate("bandido", bandido);

        Character guardia = new CharacterBuilder()
                .withName("Guardia Real")
                .withRace(RaceFactory.createRace("enano"))
                .withClass(CharacterClassFactory.createClass("guerrero"))
                .addSkill(new Skill("Golpe Certero", "Ataque físico con bono de precisión", 0))
                .build();
        registry.registerTemplate("guardia", guardia);

        Character mago = new CharacterBuilder()
                .withName("Mago Errante")
                .withRace(RaceFactory.createRace("elfo"))
                .withClass(CharacterClassFactory.createClass("mago"))
                .addSkill(new Skill("Bola de Fuego", "Daño mágico en área", 15))
                .build();
        registry.registerTemplate("mago", mago);

        return registry;
    }
}