package com.proyecto.rpg.singleton;

import com.proyecto.rpg.factory.CharacterClassFactory;
import com.proyecto.rpg.factory.OutfitFactory;
import com.proyecto.rpg.factory.RaceFactory;
import com.proyecto.rpg.factory.SkillFactory;
import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.model.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Patrón Singleton.
 * Única instancia en memoria del catálogo completo del juego: razas,
 * clases, habilidades y vestuario. Evita releer estos datos en cada
 * pantalla de la UI.
 *
 * Los cuatro catálogos son data-driven: cada uno lo produce su Factory
 * leyendo un archivo en src/main/resources/data/. Este Singleton no
 * conoce ningún dato del juego, solo los cachea y los expone.
 *
 * Ojo: expone las listas COMPLETAS, sin filtrar. El filtrado por raza o
 * clase es responsabilidad de quien las consume (los pasos 3 y 4 del
 * wizard), no del catálogo.
 *
 * Uso: CatalogManager.getInstance().getAvailableRaces()
 */
public class CatalogManager {

    private static CatalogManager instance;

    private final List<Race> availableRaces = new ArrayList<>();
    private final List<CharacterClass> availableClasses = new ArrayList<>();
    private final List<Skill> availableSkills = new ArrayList<>();
    private final List<Outfit> availableOutfits = new ArrayList<>();

    private CatalogManager() {
        loadCatalog();
    }

    public static synchronized CatalogManager getInstance() {
        if (instance == null) {
            instance = new CatalogManager();
        }
        return instance;
    }

    private void loadCatalog() {
        availableRaces.addAll(RaceFactory.createAll());
        availableClasses.addAll(CharacterClassFactory.createAll());
        availableSkills.addAll(SkillFactory.createAll());
        availableOutfits.addAll(OutfitFactory.createAll());
    }

    /**
     * Vuelve a leer los cuatro catálogos desde disco sin reiniciar la app.
     * Útil para probar cambios en los JSON en caliente.
     */
    public synchronized void reload() {
        RaceFactory.reload();
        CharacterClassFactory.reload();
        SkillFactory.reload();
        OutfitFactory.reload();

        availableRaces.clear();
        availableClasses.clear();
        availableSkills.clear();
        availableOutfits.clear();

        loadCatalog();
    }

    public List<Race> getAvailableRaces() {
        return Collections.unmodifiableList(availableRaces);
    }

    public List<CharacterClass> getAvailableClasses() {
        return Collections.unmodifiableList(availableClasses);
    }

    public List<Skill> getAvailableSkills() {
        return Collections.unmodifiableList(availableSkills);
    }

    public List<Outfit> getAvailableOutfits() {
        return Collections.unmodifiableList(availableOutfits);
    }

    /**
     * Busca una raza del catálogo por id. Sirve para reconciliar un
     * personaje leído del JSON con la instancia viva del catálogo.
     */
    public Optional<Race> findRaceById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return availableRaces.stream()
                .filter(r -> id.equalsIgnoreCase(r.getId()))
                .findFirst();
    }

    public Optional<CharacterClass> findClassById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return availableClasses.stream()
                .filter(c -> id.equalsIgnoreCase(c.getId()))
                .findFirst();
    }

    public Optional<Skill> findSkillById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return availableSkills.stream()
                .filter(s -> id.equalsIgnoreCase(s.getId()))
                .findFirst();
    }

    public Optional<Outfit> findOutfitById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return availableOutfits.stream()
                .filter(o -> id.equalsIgnoreCase(o.getId()))
                .findFirst();
    }
}
