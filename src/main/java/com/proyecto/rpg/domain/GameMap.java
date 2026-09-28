package com.proyecto.rpg.domain;

import java.util.Random;

/**
 * Mapa con terreno. '.' pasto, '=' camino (caminables);
 * '#' piedra, 'T' arbol, '~' agua (bloqueados).
 */
public class GameMap {
    public final int width;
    public final int height;
    private final char[][] tiles;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new char[height][width];
        generate();
    }

    private void generate() {
        Random r = new Random(42);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                if (border) tiles[y][x] = '#';
                else tiles[y][x] = r.nextInt(100) < 8 ? 'T' : '.';
            }
        }
        lake(10, 8, 8, 6);
        lake(32, 30, 10, 8);
        // caminos en cruz
        for (int i = 1; i < width - 1; i++) tiles[height / 2][i] = '=';
        for (int i = 1; i < height - 1; i++) tiles[i][width / 2] = '=';
        // zona de aparicion despejada
        for (int y = 1; y <= 5; y++)
            for (int x = 1; x <= 5; x++) tiles[y][x] = '.';
    }

    private void lake(int x0, int y0, int w, int h) {
        for (int y = y0; y < y0 + h && y < height - 1; y++)
            for (int x = x0; x < x0 + w && x < width - 1; x++) tiles[y][x] = '~';
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean isWalkable(int x, int y) {
        if (!isWithinBounds(x, y)) return false;
        char c = tiles[y][x];
        return c == '.' || c == '=';
    }

    public String[] rows() {
        String[] out = new String[height];
        for (int y = 0; y < height; y++) out[y] = new String(tiles[y]);
        return out;
    }
}