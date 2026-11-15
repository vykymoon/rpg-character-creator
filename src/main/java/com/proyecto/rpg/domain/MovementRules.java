package com.proyecto.rpg.domain;

/**
 * Reglas de movimiento del dominio (ADR-001).
 *
 * No conoce la red ni JavaFX: las aplica el servidor autoritativo antes de
 * aceptar cada movimiento. La regla de arquitectura de ArchitectureTest
 * impide que este paquete dependa de adaptadores o de librerias de red.
 */
public class MovementRules {

    private final GameMap map;

    public MovementRules(GameMap map) {
        this.map = map;
    }

    /** La casilla de destino existe y se puede pisar. */
    public boolean isValidMove(int x, int y) {
        return map.isWalkable(x, y);
    }

    /**
     * Un paso valido: el destino es caminable y es vecino de la posicion
     * actual (incluye diagonales). Cierra el pendiente del Corte 2 en el que
     * un cliente modificado podia cruzar el mapa con un solo mensaje.
     */
    public boolean isValidStep(int fromX, int fromY, int toX, int toY) {
        if (!isValidMove(toX, toY)) {
            return false;
        }
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        return dx <= 1 && dy <= 1;
    }
}
