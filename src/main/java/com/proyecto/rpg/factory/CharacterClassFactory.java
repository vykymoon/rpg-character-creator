package com.proyecto.rpg.factory;

import com.google.gson.reflect.TypeToken;
import com.proyecto.rpg.model.CharacterClass;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón Factory (Factory Method) — versión data-driven.
 * Mismo criterio que RaceFactory, pero para las clases de personaje:
 * el catálogo vive en src/main/resources/data/classes.json y esta clase
 * es el único punto que instancia CharacterClass.
 *
 * Uso:
 *   CharacterClass mago = CharacterClassFactory.createClass("mago");
 */
public class CharacterClassFactory {

    private static final String CATALOG_RESOURCE = "/data/classes.json";

    private static Map<String, ClassDefinition> definitions;

    private CharacterClassFactory() {
        // Factory estática: no se instancia.
    }

    /** Crea una clase a partir de su id en el catálogo (ej. "guerrero"). */
    public static CharacterClass createClass(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de clase no puede estar vacío.");
        }

        ClassDefinition definition = definitions().get(normalize(id));
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Clase no soportada: '" + id + "'. Disponibles: " + availableIds());
        }
        return build(definition);
    }

    /** Crea todas las clases del catálogo, en el orden del archivo JSON. */
    public static List<CharacterClass> createAll() {
        List<CharacterClass> classes = new ArrayList<>();
        for (ClassDefinition definition : definitions().values()) {
            classes.add(build(definition));
        }
        return classes;
    }

    public static List<String> availableIds() {
        return new ArrayList<>(definitions().keySet());
    }

    public static synchronized void reload() {
        definitions = null;
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    private static CharacterClass build(ClassDefinition d) {
        return new CharacterClass(
                normalize(d.id),
                d.name,
                d.description,
                d.bonusStrength,
                d.bonusDexterity,
                d.bonusIntelligence,
                d.bonusVitality);
    }

    private static synchronized Map<String, ClassDefinition> definitions() {
        if (definitions == null) {
            definitions = loadDefinitions();
        }
        return definitions;
    }

    private static Map<String, ClassDefinition> loadDefinitions() {
        Type listType = new TypeToken<List<ClassDefinition>>() {}.getType();
        List<ClassDefinition> entries = JsonCatalogLoader.load(CATALOG_RESOURCE, listType);

        Map<String, ClassDefinition> byId = new LinkedHashMap<>();
        for (ClassDefinition entry : entries) {
            validate(entry);
            if (byId.put(normalize(entry.id), entry) != null) {
                throw new IllegalStateException(
                        "Id de clase duplicado en " + CATALOG_RESOURCE + ": '" + entry.id + "'.");
            }
        }
        return byId;
    }

    private static void validate(ClassDefinition d) {
        if (d == null) {
            throw new IllegalStateException("Hay una entrada nula en " + CATALOG_RESOURCE + ".");
        }
        if (d.id == null || d.id.isBlank()) {
            throw new IllegalStateException("Hay una clase sin 'id' en " + CATALOG_RESOURCE + ".");
        }
        if (d.name == null || d.name.isBlank()) {
            throw new IllegalStateException(
                    "La clase '" + d.id + "' no tiene 'name' en " + CATALOG_RESOURCE + ".");
        }
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase();
    }

    /** Espejo 1:1 de un objeto del archivo classes.json. */
    private static class ClassDefinition {
        String id;
        String name;
        String description;
        int bonusStrength;
        int bonusDexterity;
        int bonusIntelligence;
        int bonusVitality;
    }
}
