package com.proyecto.rpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma el texto corto que describe el requisito de un ítem, para poder
 * mostrarlo en la UI (ej. "Solo Elfo" o "Solo Mago, Druida").
 * Devuelve null si el ítem no tiene ninguna restricción.
 */
final class RequirementLabel {

    private RequirementLabel() {
    }

    static String of(List<String> allowedRaces, List<String> allowedClasses) {
        List<String> partes = new ArrayList<>();
        if (allowedRaces != null && !allowedRaces.isEmpty()) {
            partes.add(join(allowedRaces));
        }
        if (allowedClasses != null && !allowedClasses.isEmpty()) {
            partes.add(join(allowedClasses));
        }
        if (partes.isEmpty()) {
            return null;
        }
        return "Solo " + String.join(" / ", partes);
    }

    private static String join(List<String> valores) {
        List<String> bonitos = new ArrayList<>();
        for (String v : valores) {
            if (v == null || v.isBlank()) {
                continue;
            }
            String s = v.trim();
            // Ojo: NO usar Character.toUpperCase() acá. Este paquete tiene su
            // propia clase Character (el personaje), que le hace sombra a
            // java.lang.Character y el código no compila.
            bonitos.add(s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());
        }
        return String.join(", ", bonitos);
    }
}
