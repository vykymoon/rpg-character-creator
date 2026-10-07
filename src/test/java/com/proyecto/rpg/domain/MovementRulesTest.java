package com.proyecto.rpg.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de la regla de movimiento.
 *
 * MovementRules es la unica autoridad sobre si un movimiento se acepta, y
 * el servidor la consulta antes de mover a nadie (ADR-001). Por eso se
 * prueba aparte del servidor: si estas pruebas pasan, el servidor hereda
 * la garantia sin necesidad de levantar red.
 */
class MovementRulesTest {

    private final GameMap map = new GameMap(50, 50);
    private final MovementRules rules = new MovementRules(map);

    @Test
    @DisplayName("Acepta una casilla caminable")
    void aceptaCasillaCaminable() {
        assertTrue(rules.isValidMove(2, 2));
    }

    @Test
    @DisplayName("Acepta un camino")
    void aceptaCamino() {
        assertTrue(rules.isValidMove(10, 25));
    }

    @Test
    @DisplayName("Rechaza una casilla fuera del mapa")
    void rechazaFueraDelMapa() {
        assertFalse(rules.isValidMove(-1, 5));
        assertFalse(rules.isValidMove(5, -1));
        assertFalse(rules.isValidMove(50, 5));
        assertFalse(rules.isValidMove(5, 50));
    }

    @Test
    @DisplayName("Rechaza la piedra del borde")
    void rechazaPiedraDelBorde() {
        assertFalse(rules.isValidMove(0, 0));
        assertFalse(rules.isValidMove(49, 20));
    }

    @Test
    @DisplayName("Rechaza el agua")
    void rechazaAgua() {
        assertFalse(rules.isValidMove(12, 10));
        assertFalse(rules.isValidMove(35, 32));
    }
}
