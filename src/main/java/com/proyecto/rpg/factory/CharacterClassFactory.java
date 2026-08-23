package com.proyecto.rpg.factory;

import com.proyecto.rpg.model.CharacterClass;

/**
 * Patrón Factory (Factory Method).
 * Centraliza la creación de clases de personaje con sus bonus.
 *
 * Persona A: aquí puedes agregar más clases (Arquero, Clérigo, etc.)
 */
public class CharacterClassFactory {

    public enum ClassType {
        GUERRERO, MAGO, PICARO
    }

    public static CharacterClass createClass(ClassType type) {
        switch (type) {
            case GUERRERO:
                return new CharacterClass("Guerrero", 5, 2, 0, 4);
            case MAGO:
                return new CharacterClass("Mago", 0, 2, 6, 1);
            case PICARO:
                return new CharacterClass("Pícaro", 2, 6, 1, 2);
            default:
                throw new IllegalArgumentException("Clase no soportada: " + type);
        }
    }
}
