package com.proyecto.rpg.prototype;

import com.proyecto.rpg.model.Character;

import java.util.HashMap;
import java.util.Map;

/**
 * Patrón Prototype.
 * Mantiene un registro de personajes/NPCs "plantilla" ya armados
 * (ej: "Bandido Base", "Guardia Real") que se pueden clonar y modificar
 * sin tener que reconstruirlos desde cero con el Builder cada vez.
 *
 * Uso típico:
 *   CharacterPrototype registry = new CharacterPrototype();
 *   registry.registerTemplate("bandido", personajeBanditoBase);
 *   Character nuevoBandido = registry.clone("bandido");
 *   nuevoBandido.setName("Bandido #2");
 *
 * Persona B: aquí puedes precargar plantillas de NPCs comunes al iniciar
 * la app (por ejemplo desde el CatalogManager / singleton).
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
}
