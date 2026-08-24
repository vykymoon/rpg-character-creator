package com.proyecto.rpg.model;

import java.util.List;

/**
 * Regla compartida por Skill y Outfit para decidir si un ítem está
 * disponible para una raza y/o una clase. Está aparte para que la lógica
 * exista una sola vez y no se desincronice entre las dos clases.
 *
 * Una lista vacía (o null) significa "sin restricción". Se acepta que la
 * restricción se escriba con el id del catálogo ("elfo") o con el nombre
 * visible ("Elfo"), ignorando mayúsculas y espacios.
 */
final class CatalogRestriction {

    private CatalogRestriction() {
        // Clase de utilidad: no se instancia.
    }

    static boolean allowsRace(List<String> allowedRaces, Race race) {
        if (isUnrestricted(allowedRaces)) {
            return true;
        }
        return race != null && matches(allowedRaces, race.getId(), race.getName());
    }

    static boolean allowsClass(List<String> allowedClasses, CharacterClass characterClass) {
        if (isUnrestricted(allowedClasses)) {
            return true;
        }
        return characterClass != null
                && matches(allowedClasses, characterClass.getId(), characterClass.getName());
    }

    /**
     * null además de vacío: los personajes guardados antes de esta función
     * no tienen el campo en characters.json, y Gson deja el atributo en
     * null al releerlos.
     */
    private static boolean isUnrestricted(List<String> allowed) {
        return allowed == null || allowed.isEmpty();
    }

    private static boolean matches(List<String> allowed, String id, String name) {
        for (String entry : allowed) {
            if (entry == null) {
                continue;
            }
            String key = entry.trim();
            if (key.equalsIgnoreCase(id) || key.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
