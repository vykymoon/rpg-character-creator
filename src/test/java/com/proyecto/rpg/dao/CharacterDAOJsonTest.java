package com.proyecto.rpg.dao;

import com.proyecto.rpg.model.Character;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de ida y vuelta de la persistencia (ADR-003) sobre un archivo
 * temporal: lo que se guarda es lo que se lee. Era un pendiente del Corte 2.
 */
class CharacterDAOJsonTest {

    @TempDir
    Path carpeta;

    private CharacterDAO dao;

    @BeforeEach
    void setUp() {
        dao = new CharacterDAOJson(carpeta.resolve("personajes.json"));
    }

    private static Character personaje(String nombre) {
        Character c = new Character();
        c.setName(nombre);
        return c;
    }

    @Test
    @DisplayName("Crea el archivo vacio si no existe")
    void creaArchivoSiNoExiste() {
        assertTrue(Files.exists(carpeta.resolve("personajes.json")));
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    @DisplayName("Lo que se guarda es lo que se lee")
    void idaYVuelta() {
        Character aerin = personaje("Aerin");
        dao.save(aerin);

        Character leido = dao.findById(aerin.getId()).orElseThrow();
        assertEquals("Aerin", leido.getName());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    @DisplayName("Guardar dos veces el mismo personaje lo actualiza, no lo duplica")
    void guardarDosVecesActualiza() {
        Character aerin = personaje("Aerin");
        dao.save(aerin);
        aerin.setName("Aerin la Sabia");
        dao.save(aerin);

        assertEquals(1, dao.findAll().size());
        assertEquals("Aerin la Sabia", dao.findById(aerin.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("Los datos sobreviven a una nueva instancia del DAO")
    void sobreviveANuevaInstancia() {
        Character aerin = personaje("Aerin");
        dao.save(aerin);

        CharacterDAO otro = new CharacterDAOJson(carpeta.resolve("personajes.json"));
        assertTrue(otro.findById(aerin.getId()).isPresent());
    }

    @Test
    @DisplayName("Borrar elimina solo al personaje indicado")
    void borrar() {
        Character a = personaje("A");
        Character b = personaje("B");
        dao.save(a);
        dao.save(b);

        dao.delete(a.getId());

        assertFalse(dao.findById(a.getId()).isPresent());
        assertTrue(dao.findById(b.getId()).isPresent());
    }

    @Test
    @DisplayName("existsByName ignora mayusculas y espacios")
    void existsByNameNormaliza() {
        dao.save(personaje("Aerin"));

        assertTrue(dao.existsByName("  aerin "));
        assertFalse(dao.existsByName("Brom"));
        assertFalse(dao.existsByName(null));
    }
}
