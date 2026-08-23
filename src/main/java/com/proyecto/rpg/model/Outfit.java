package com.proyecto.rpg.model;

/**
 * Representa una pieza de vestuario/equipamiento visual del personaje.
 */
public class Outfit {

    private final String name;
    private final String slot; // ej: "armadura", "casco", "arma", "capa"
    private final String spriteRef; // referencia a imagen/recurso visual

    public Outfit(String name, String slot, String spriteRef) {
        this.name = name;
        this.slot = slot;
        this.spriteRef = spriteRef;
    }

    public String getName() {
        return name;
    }

    public String getSlot() {
        return slot;
    }

    public String getSpriteRef() {
        return spriteRef;
    }

    @Override
    public String toString() {
        return name + " [" + slot + "]";
    }
}
