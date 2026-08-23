package com.proyecto.rpg.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.proyecto.rpg.model.Character;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de CharacterDAO que persiste los personajes en un
 * único archivo JSON local (una "base de datos" simple basada en archivo).
 *
 * Persona C: si más adelante quieres SQLite, deja esta clase intacta
 * (sirve de referencia/fallback) y crea CharacterDAOSqlite aparte
 * implementando la misma interfaz CharacterDAO.
 */
public class CharacterDAOJson implements CharacterDAO {

    private final Path storageFile;
    private final Gson gson;

    public CharacterDAOJson() {
        this(Paths.get("src", "main", "resources", "data", "characters.json"));
    }

    public CharacterDAOJson(Path storageFile) {
        this.storageFile = storageFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (storageFile.getParent() != null) {
                Files.createDirectories(storageFile.getParent());
            }
            if (!Files.exists(storageFile)) {
                Files.writeString(storageFile, "[]", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el archivo de personajes: " + storageFile, e);
        }
    }

    private List<Character> readAll() {
        try (Reader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Character>>() {}.getType();
            List<Character> characters = gson.fromJson(reader, listType);
            return characters != null ? characters : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo personajes desde " + storageFile, e);
        }
    }

    private void writeAll(List<Character> characters) {
        try (Writer writer = Files.newBufferedWriter(storageFile, StandardCharsets.UTF_8)) {
            gson.toJson(characters, writer);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando personajes en " + storageFile, e);
        }
    }

    @Override
    public void save(Character character) {
        List<Character> all = readAll();
        all.removeIf(c -> c.getId().equals(character.getId()));
        all.add(character);
        writeAll(all);
    }

    @Override
    public Optional<Character> findById(String id) {
        return readAll().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Character> findAll() {
        return readAll();
    }

    @Override
    public void delete(String id) {
        List<Character> all = readAll();
        all.removeIf(c -> c.getId().equals(id));
        writeAll(all);
    }

    @Override
    public boolean existsByName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.trim().toLowerCase();
        return readAll().stream()
                .anyMatch(c -> c.getName() != null && c.getName().trim().toLowerCase().equals(normalized));
    }
}