package com.proyecto.rpg.model;

import java.util.List;

/**
 * Regla compartida por Skill y Outfit para decidir si un ítem está
 * disponible para una raza. Está aparte para que la lógica exista una
 * sola vez y no se desincronice entre las dos clases.
 *
 * Acepta que la restricción se escriba con el id del catálogo ("elfo") o
 * con el nombre visible ("Elfo"), ignorando mayúsculas y espacios.
 */
final class RaceRestriction {

    private RaceRestriction() {
        // Clase de utilidad: no se instancia.
    }

    static boolean allows(List<String> allowedRaces, Race race) {
        // null además de vacío: los personajes guardados antes de esta
        // función no tienen el campo en characters.json, y Gson deja el
        // atributo en null al releerlos.
        if (allowedRaces == null || allowedRaces.isEmpty()) {
            return true;
        }
        if (race == null) {
            return false;
        }

        for (String allowed : allowedRaces) {
            if (allowed == null) {
                continue;
            }
            String key = allowed.trim();
            if (key.equalsIgnoreCase(race.getId()) || key.equalsIgnoreCase(race.getName())) {
                return true;
            }
        }
        return false;
    }
}
