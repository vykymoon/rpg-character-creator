package com.proyecto.rpg.domain;

import io.javalin.Javalin;

/**
 * DEMO (no fusionar): una clase del dominio que depende de la
 * infraestructura de red. Rompe la regla dominio_no_depende_de_infraestructura
 * (ADR-001) y el pipeline debe detenerse en "Reglas de arquitectura".
 */
public class AtajoDeRed {
    public static Javalin servidorDirecto() {
        return Javalin.create();
    }
}
