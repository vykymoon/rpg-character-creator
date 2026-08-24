package com.proyecto.rpg.factory;

import com.google.gson.reflect.TypeToken;
import com.proyecto.rpg.model.Race;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón Factory (Factory Method) — versión data-driven.
 *
 * Sigue siendo el UNICO punto de la app que construye objetos Race: el
 * resto del código pide razas por id y nunca ve los valores numéricos ni
 * sabe de dónde salen. Lo que cambió es el origen de esos datos: antes
 * estaban hardcodeados en un switch sobre un enum, ahora se leen de
 * src/main/resources/data/races.json.
 *
 * Consecuencia práctica: agregar una raza nueva ya no requiere tocar
 * código Java — basta con agregar un objeto al JSON.
 *
 * Uso:
 *   Race elfo = RaceFactory.createRace("elfo");
 *   List&lt;Race&gt; todas = RaceFactory.createAll();
 */
public class RaceFactory {

    private static final String CATALOG_RESOURCE = "/data/races.json";

    /** Definiciones leídas del JSON. Se cargan una sola vez (lazy). */
    private static Map<String, RaceDefinition> definitions;

    private RaceFactory() {
        // Factory estática: no se instancia.
    }

    /**
     * Crea una raza a partir de su id en el catálogo (ej. "elfo").
     * Devuelve siempre una instancia nueva.
     */
    public static Race createRace(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de raza no puede estar vacío.");
        }

        RaceDefinition definition = definitions().get(normalize(id));
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Raza no soportada: '" + id + "'. Disponibles: " + availableIds());
        }
        return build(definition);
    }

    /** Crea todas las razas del catálogo, en el orden del archivo JSON. */
    public static List<Race> createAll() {
        List<Race> races = new ArrayList<>();
        for (RaceDefinition definition : definitions().values()) {
            races.add(build(definition));
        }
        return races;
    }

    /** Ids disponibles, útil para mensajes de error y para depurar. */
    public static List<String> availableIds() {
        return new ArrayList<>(definitions().keySet());
    }

    /**
     * Descarta el catálogo cacheado para que la próxima llamada lo vuelva
     * a leer. Sirve para recargar en caliente durante el desarrollo.
     */
    public static synchronized void reload() {
        definitions = null;
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    /** El paso Factory propiamente dicho: definición (datos) -> Race (modelo). */
    private static Race build(RaceDefinition d) {
        return new Race(
                normalize(d.id),
                d.name,
                d.description,
                d.baseStrength,
                d.baseDexterity,
                d.baseIntelligence,
                d.baseVitality);
    }

    private static synchronized Map<String, RaceDefinition> definitions() {
        if (definitions == null) {
            definitions = loadDefinitions();
        }
        return definitions;
    }

    private static Map<String, RaceDefinition> loadDefinitions() {
        Type listType = new TypeToken<List<RaceDefinition>>() {}.getType();
        List<RaceDefinition> entries = JsonCatalogLoader.load(CATALOG_RESOURCE, listType);

        Map<String, RaceDefinition> byId = new LinkedHashMap<>();
        for (RaceDefinition entry : entries) {
            validate(entry);
            if (byId.put(normalize(entry.id), entry) != null) {
                throw new IllegalStateException(
                        "Id de raza duplicado en " + CATALOG_RESOURCE + ": '" + entry.id + "'.");
            }
        }
        return byId;
    }

    private static void validate(RaceDefinition d) {
        if (d == null) {
            throw new IllegalStateException("Hay una entrada nula en " + CATALOG_RESOURCE + ".");
        }
        if (d.id == null || d.id.isBlank()) {
            throw new IllegalStateException("Hay una raza sin 'id' en " + CATALOG_RESOURCE + ".");
        }
        if (d.name == null || d.name.isBlank()) {
            throw new IllegalStateException(
                    "La raza '" + d.id + "' no tiene 'name' en " + CATALOG_RESOURCE + ".");
        }
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase();
    }

    /**
     * Espejo 1:1 de un objeto del archivo races.json.
     * Es un DTO deliberadamente separado de Race: Gson rellena esto, y la
     * Factory decide cómo convertirlo en un Race. Así el modelo del
     * dominio no queda atado al formato del archivo.
     */
    private static class RaceDefinition {
        String id;
        String name;
        String description;
        int baseStrength;
        int baseDexterity;
        int baseIntelligence;
        int baseVitality;
    }
}
