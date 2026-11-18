package com.proyecto.rpg.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto unico donde se decide que implementacion de CharacterDAO usa la app
 * (ADR-003).
 *
 * Antes los controladores de la UI hacian {@code new CharacterDAOJson()}: la
 * interfaz existia, pero la UI seguia atada al archivo JSON. Ahora la UI solo
 * conoce {@link CharacterDAO} y este proveedor, y la regla de ArchitectureTest
 * hace fallar el pipeline si un controlador vuelve a instanciar la
 * implementacion. Cambiar a SQLite es cambiar una linea aqui.
 *
 * Donde se guardan los personajes:
 * <ol>
 *   <li>la variable de entorno o propiedad {@code RPG_DATA_FILE}, si existe;</li>
 *   <li>{@code src/main/resources/data/characters.json} cuando se ejecuta
 *       desde el repositorio (comportamiento del Corte 2);</li>
 *   <li>{@code ~/.rpg-character-creator/characters.json} cuando se ejecuta el
 *       cliente descargado de GitHub Releases.</li>
 * </ol>
 */
public final class CharacterDAOProvider {

    private static CharacterDAO instance;

    private CharacterDAOProvider() {
    }

    public static synchronized CharacterDAO get() {
        if (instance == null) {
            instance = new CharacterDAOJson(resolveStorageFile());
        }
        return instance;
    }

    /** Permite a las pruebas o a otra implementacion reemplazar el DAO. */
    public static synchronized void set(CharacterDAO dao) {
        instance = dao;
    }

    /**
     * Archivo donde el creador guarda los personajes. Lo usa tambien el
     * multijugador (CharacterLoader) para leer exactamente el mismo archivo,
     * se ejecute desde IntelliJ o desde el .jar de GitHub Releases.
     */
    public static Path storageFile() {
        return resolveStorageFile();
    }

    static Path resolveStorageFile() {
        String configured = System.getProperty("RPG_DATA_FILE", System.getenv("RPG_DATA_FILE"));
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        Path repoData = Paths.get("src", "main", "resources", "data");
        if (Files.isDirectory(repoData)) {
            return repoData.resolve("characters.json");
        }
        return Paths.get(System.getProperty("user.home"), ".rpg-character-creator", "characters.json");
    }
}
