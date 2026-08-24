package com.proyecto.rpg.factory;

import com.google.gson.reflect.TypeToken;
import com.proyecto.rpg.model.Skill;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón Factory, mismo criterio que RaceFactory: único punto que crea
 * objetos Skill, leyendo el catálogo de
 * src/main/resources/data/skills.json.
 *
 * Cada habilidad puede declarar allowedRaces y/o allowedClasses; vacío
 * significa disponible para todos.
 */
public class SkillFactory {

    private static final String CATALOG_RESOURCE = "/data/skills.json";

    private static Map<String, SkillDefinition> definitions;

    private SkillFactory() {
    }

    public static Skill createSkill(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de habilidad no puede estar vacío.");
        }
        SkillDefinition definition = definitions().get(normalize(id));
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Habilidad no soportada: '" + id + "'. Disponibles: " + availableIds());
        }
        return build(definition);
    }

    public static List<Skill> createAll() {
        List<Skill> skills = new ArrayList<>();
        for (SkillDefinition definition : definitions().values()) {
            skills.add(build(definition));
        }
        return skills;
    }

    public static List<String> availableIds() {
        return new ArrayList<>(definitions().keySet());
    }

    public static synchronized void reload() {
        definitions = null;
    }

    private static Skill build(SkillDefinition d) {
        return new Skill(normalize(d.id), d.name, d.description, d.manaCost,
                d.allowedRaces, d.allowedClasses);
    }

    private static synchronized Map<String, SkillDefinition> definitions() {
        if (definitions == null) {
            definitions = loadDefinitions();
        }
        return definitions;
    }

    private static Map<String, SkillDefinition> loadDefinitions() {
        Type listType = new TypeToken<List<SkillDefinition>>() {}.getType();
        List<SkillDefinition> entries = JsonCatalogLoader.load(CATALOG_RESOURCE, listType);

        Map<String, SkillDefinition> byId = new LinkedHashMap<>();
        for (SkillDefinition entry : entries) {
            validate(entry);
            if (byId.put(normalize(entry.id), entry) != null) {
                throw new IllegalStateException(
                        "Id de habilidad duplicado en " + CATALOG_RESOURCE + ": '" + entry.id + "'.");
            }
        }
        return byId;
    }

    private static void validate(SkillDefinition d) {
        if (d == null) {
            throw new IllegalStateException("Hay una entrada nula en " + CATALOG_RESOURCE + ".");
        }
        if (d.id == null || d.id.isBlank()) {
            throw new IllegalStateException("Hay una habilidad sin 'id' en " + CATALOG_RESOURCE + ".");
        }
        if (d.name == null || d.name.isBlank()) {
            throw new IllegalStateException(
                    "La habilidad '" + d.id + "' no tiene 'name' en " + CATALOG_RESOURCE + ".");
        }
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase();
    }

    /** Espejo 1:1 de un objeto de skills.json. */
    private static class SkillDefinition {
        String id;
        String name;
        String description;
        int manaCost;
        List<String> allowedRaces;
        List<String> allowedClasses;
    }
}
