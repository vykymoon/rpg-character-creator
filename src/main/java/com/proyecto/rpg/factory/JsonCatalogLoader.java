package com.proyecto.rpg.factory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utilidad interna del paquete factory: lee un archivo JSON del classpath
 * (src/main/resources/...) y lo convierte en una lista de "definiciones".
 *
 * Se usa getResourceAsStream y NO new File(...) a propósito: leer por
 * classpath funciona igual corriendo desde IntelliJ que desde un .jar
 * empaquetado, mientras que una ruta de archivo relativa se rompe en
 * cuanto la app se ejecuta desde otra carpeta de trabajo.
 */
final class JsonCatalogLoader {

    private static final Gson GSON = new Gson();

    private JsonCatalogLoader() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * @param resourcePath ruta absoluta dentro del classpath, ej. "/data/races.json"
     * @param listType     tipo de la lista destino, obtenido con TypeToken
     * @return la lista de definiciones leída del archivo (nunca vacía)
     */
    static <T> List<T> load(String resourcePath, Type listType) {
        try (InputStream in = JsonCatalogLoader.class.getResourceAsStream(resourcePath)) {

            if (in == null) {
                throw new IllegalStateException(
                        "No se encontró el catálogo '" + resourcePath + "'. "
                                + "Debe existir en src/main/resources" + resourcePath
                                + " (si lo acabas de crear, recompila para que Maven lo copie a target/classes).");
            }

            Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            List<T> entries = GSON.fromJson(reader, listType);

            if (entries == null || entries.isEmpty()) {
                throw new IllegalStateException(
                        "El catálogo '" + resourcePath + "' está vacío: define al menos una entrada.");
            }
            return entries;

        } catch (JsonParseException e) {
            throw new IllegalStateException(
                    "JSON inválido en '" + resourcePath + "': " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer el catálogo '" + resourcePath + "'.", e);
        }
    }
}
