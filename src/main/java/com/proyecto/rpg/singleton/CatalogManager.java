package com.proyecto.rpg.singleton;

import com.proyecto.rpg.factory.CharacterClassFactory;
import com.proyecto.rpg.factory.RaceFactory;
import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.model.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Singleton.
 * Única instancia en memoria del catálogo de razas, clases y habilidades
 * disponibles en el juego. Evita crear/recargar estos datos repetidamente
 * en cada pantalla de la UI.
 *
 * Uso: CatalogManager.getInstance().getAvailableRaces()
 */
public class CatalogManager {

    private static CatalogManager instance;

    private final List<Race> availableRaces;
    private final List<CharacterClass> availableClasses;
    private final List<Skill> availableSkills;
    private final List<Outfit> availableOutfits;

    private CatalogManager() {
        availableRaces = new ArrayList<>();
        availableClasses = new ArrayList<>();
        availableSkills = new ArrayList<>();
        availableOutfits = new ArrayList<>();
        loadCatalog();
    }

    public static synchronized CatalogManager getInstance() {
        if (instance == null) {
            instance = new CatalogManager();
        }
        return instance;
    }

    private void loadCatalog() {
        // Razas y clases vía Factory (mínimo funcional).
        for (RaceFactory.RaceType type : RaceFactory.RaceType.values()) {
            availableRaces.add(RaceFactory.createRace(type));
        }
        for (CharacterClassFactory.ClassType type : CharacterClassFactory.ClassType.values()) {
            availableClasses.add(CharacterClassFactory.createClass(type));
        }

        // Habilidades base de ejemplo.
        // Persona A/B: pueden mover esto a resources/data/skills.json
        // y cargarlo con Gson si prefieren catálogo data-driven.
        availableSkills.add(new Skill("Golpe Certero", "Ataque físico con bono de precisión", 0));
        availableSkills.add(new Skill("Bola de Fuego", "Daño mágico en área", 15));
        availableSkills.add(new Skill("Sigilo", "Reduce la probabilidad de ser detectado", 5));
        availableSkills.add(new Skill("Curación Menor", "Restaura una pequeña cantidad de vida", 10));

        // Vestuario base de ejemplo.
        // Persona B: reemplazar spriteRef por rutas reales cuando haya arte.
        availableOutfits.add(new Outfit("Armadura de Cuero", "armadura", "sprites/armor_leather.png"));
        availableOutfits.add(new Outfit("Armadura de Placas", "armadura", "sprites/armor_plate.png"));
        availableOutfits.add(new Outfit("Túnica de Mago", "armadura", "sprites/robe_mage.png"));
        availableOutfits.add(new Outfit("Casco de Guerra", "casco", "sprites/helmet_war.png"));
        availableOutfits.add(new Outfit("Capa Élfica", "capa", "sprites/cape_elven.png"));
    }

    public List<Race> getAvailableRaces() {
        return availableRaces;
    }

    public List<CharacterClass> getAvailableClasses() {
        return availableClasses;
    }

    public List<Skill> getAvailableSkills() {
        return availableSkills;
    }

    public List<Outfit> getAvailableOutfits() {
        return availableOutfits;
    }
}