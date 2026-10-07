package com.proyecto.rpg.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias del mapa. No tocan red ni JavaFX.
 *
 * El mapa se genera con semilla fija (Random(42)), asi que las casillas
 * que se escriben despues del relleno aleatorio (borde, lagos, caminos y
 * zona de aparicion) estan siempre en el mismo sitio. Las aserciones solo
 * usan esas, nunca una casilla que dependa del azar.
 */
class GameMapTest {

    private final GameMap map = new GameMap(50, 50);

    @Test
    @DisplayName("El mapa tiene las dimensiones pedidas")
    void elMapaTieneLasDimensionesPedidas() {
        assertEquals(50, map.width);
        assertEquals(50, map.height);

        String[] filas = map.rows();
        assertEquals(50, filas.length);
        assertEquals(50, filas[0].length());
    }

    @Test
    @DisplayName("El borde es piedra y no se camina")
    void elBordeEsPiedraYNoSeCamina() {
        assertFalse(map.isWalkable(0, 0));
        assertFalse(map.isWalkable(49, 49));
        assertFalse(map.isWalkable(0, 25));
        assertFalse(map.isWalkable(25, 0));
    }

    @Test
    @DisplayName("La zona de aparicion esta despejada")
    void laZonaDeAparicionEstaDespejada() {
        // generate() limpia el rectangulo 1..5 en ambos ejes al final,
        // para que un jugador nunca aparezca dentro de un arbol.
        for (int y = 1; y <= 5; y++) {
            for (int x = 1; x <= 5; x++) {
                assertTrue(map.isWalkable(x, y),
                        "La casilla de aparicion (" + x + "," + y + ") deberia ser caminable");
            }
        }
    }

    @Test
    @DisplayName("El agua bloquea el paso")
    void elAguaBloqueaElPaso() {
        // lake(10, 8, 8, 6) cubre x 10..17, y 8..13
        assertFalse(map.isWalkable(12, 10));
        // lake(32, 30, 10, 8) cubre x 32..41, y 30..37
        assertFalse(map.isWalkable(35, 32));
    }

    @Test
    @DisplayName("Los caminos en cruz son transitables")
    void losCaminosSonTransitables() {
        // Camino horizontal en la fila height/2 y vertical en la columna width/2.
        assertTrue(map.isWalkable(10, 25));
        assertTrue(map.isWalkable(25, 10));
    }

    @Test
    @DisplayName("Las coordenadas fuera del mapa no son caminables")
    void lasCoordenadasFueraDelMapaNoSonCaminables() {
        assertFalse(map.isWithinBounds(-1, 0));
        assertFalse(map.isWithinBounds(0, -1));
        assertFalse(map.isWithinBounds(50, 0));
        assertFalse(map.isWithinBounds(0, 50));

        assertFalse(map.isWalkable(-1, 0));
        assertFalse(map.isWalkable(50, 50));
    }

    @Test
    @DisplayName("El mapa se genera siempre igual")
    void elMapaSeGeneraSiempreIgual() {
        // La semilla fija es lo que permite probar el dominio sin simular
        // el azar: dos partidas distintas ven el mismo terreno.
        assertArrayEquals(new GameMap(50, 50).rows(), new GameMap(50, 50).rows());
    }
}
