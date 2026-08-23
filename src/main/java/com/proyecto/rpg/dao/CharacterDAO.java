package com.proyecto.rpg.dao;

import com.proyecto.rpg.model.Character;

import java.util.List;
import java.util.Optional;

/**
 * Patrón DAO (Data Access Object).
 * Separa la lógica de persistencia del resto de la app: la UI y la
 * lógica de negocio solo conocen esta interfaz, nunca el detalle de
 * si se guarda en JSON, SQLite, o cualquier otra cosa.
 *
 * Persona C: esta es tu interfaz de referencia. Ya hay una implementación
 * en JSON (CharacterDAOJson) lista para usar. Si prefieren SQLite,
 * crea CharacterDAOSqlite implementando esta misma interfaz — el resto
 * de la app no debería necesitar cambios.
 */
public interface CharacterDAO {

    void save(Character character);

    Optional<Character> findById(String id);

    List<Character> findAll();

    void delete(String id);

    /**
     * Verifica si ya existe un personaje guardado con ese nombre
     * (comparación sin distinguir mayúsculas/minúsculas ni espacios
     * al inicio/final). Usado para evitar nombres duplicados al crear
     * o clonar personajes.
     */
    boolean existsByName(String name);
}