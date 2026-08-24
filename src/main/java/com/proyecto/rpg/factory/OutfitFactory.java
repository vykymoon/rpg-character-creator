package com.proyecto.rpg.factory;

import com.google.gson.reflect.TypeToken;
import com.proyecto.rpg.model.Outfit;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón Factory para el vestuario. Lee
 * src/main/resources/data/outfits.json.
 */
public class OutfitFactory {

    private static final String CATALOG_RESOURCE = "/data/outfits.json";

    private static Map<String, OutfitDefinition> definitions;

    private OutfitFactory() {
    }

    public static Outfit createOutfit(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de vestuario no puede estar vacío.");
        }
        OutfitDefinition definition = definitions().get(normalize(id));
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Vestuario no soportado: '" + id + "'. Disponibles: " + availableIds());
        }
        return build(definition);
    }

    public static List<Outfit> createAll() {
        List<Outfit> outfits = new ArrayList<>();
        for (OutfitDefinition definition : definitions().values()) {
            outfits.add(build(definition));
        }
        return outfits;
    }

    public static List<String> availableIds() {
        return new ArrayList<>(definitions().keySet());
    }

    public static synchronized void reload() {
        definitions = null;
    }

    private static Outfit build(OutfitDefinition d) {
        return new Outfit(normalize(d.id), d.name, d.slot, d.spriteRef,
                d.allowedRaces, d.allowedClasses);
    }

    private static synchronized Map<String, OutfitDefinition> definitions() {
        if (definitions == null) {
            definitions = loadDefinitions();
        }
        return definitions;
    }

    private static Map<String, OutfitDefinition> loadDefinitions() {
        Type listType = new TypeToken<List<OutfitDefinition>>() {}.getType();
        List<OutfitDefinition> entries = JsonCatalogLoader.load(CATALOG_RESOURCE, listType);

        Map<String, OutfitDefinition> byId = new LinkedHashMap<>();
        for (OutfitDefinition entry : entries) {
            validate(entry);
            if (byId.put(normalize(entry.id), entry) != null) {
                throw new IllegalStateException(
                        "Id de vestuario duplicado en " + CATALOG_RESOURCE + ": '" + entry.id + "'.");
            }
        }
        return byId;
    }

    private static void validate(OutfitDefinition d) {
        if (d == null) {
            throw new IllegalStateException("Hay una entrada nula en " + CATALOG_RESOURCE + ".");
        }
        if (d.id == null || d.id.isBlank()) {
            throw new IllegalStateException("Hay un vestuario sin 'id' en " + CATALOG_RESOURCE + ".");
        }
        if (d.name == null || d.name.isBlank()) {
            throw new IllegalStateException(
                    "El vestuario '" + d.id + "' no tiene 'name' en " + CATALOG_RESOURCE + ".");
        }
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase();
    }

    /** Espejo 1:1 de un objeto de outfits.json. */
    private static class OutfitDefinition {
        String id;
        String name;
        String slot;
        String spriteRef;
        List<String> allowedRaces;
        List<String> allowedClasses;
    }
}
