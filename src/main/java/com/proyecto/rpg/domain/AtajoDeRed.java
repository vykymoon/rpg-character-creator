package com.proyecto.rpg.domain;

import io.javalin.http.Context;

/**
 * DEMO - NO FUSIONAR. Rompe la regla del ADR-001 a proposito:
 * el dominio no puede depender de la libreria de red.
 */
public final class AtajoDeRed {

    private AtajoDeRed() {
    }

    /** Lee el jugador directo de la peticion HTTP, saltandose el caso de uso. */
    public static String jugadorDesdeLaPeticion(Context ctx) {
        return ctx.queryParam("playerId");
    }
}
