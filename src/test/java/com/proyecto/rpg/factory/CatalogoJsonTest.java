package com.proyecto.rpg.factory;

import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.model.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del catalogo en JSON (ADR-002).
 *
 * En el Corte 2 el reto "catalogo ampliable sin recompilar" no tenia prueba
 * automatica. Estas pruebas cargan los cuatro archivos reales de
 * src/main/resources/data/ y fallan si alguien deja un JSON mal escrito, un
 * identificador repetido o una restriccion que apunta a una raza o clase que
 * no existe: los errores que antes solo aparecian al abrir la aplicacion.
 */
class CatalogoJsonTest {

    @Test
    @DisplayName("Las razas se cargan del JSON con identificadores unicos")
    void lasRazasSeCarganConIdsUnicos() {
        List<Race> razas = RaceFactory.createAll();

        assertFalse(razas.isEmpty());
        assertEquals(razas.size(), idsUnicos(razas.stream().map(Race::getId).toList()));
        assertTrue(RaceFactory.availableIds().contains("elfo"));
    }

    @Test
    @DisplayName("Las clases se cargan del JSON con identificadores unicos")
    void lasClasesSeCarganConIdsUnicos() {
        List<CharacterClass> clases = CharacterClassFactory.createAll();

        assertFalse(clases.isEmpty());
        assertEquals(clases.size(), idsUnicos(clases.stream().map(CharacterClass::getId).toList()));
    }

    @Test
    @DisplayName("Habilidades y vestuario se cargan del JSON")
    void habilidadesYVestuarioSeCargan() {
        assertFalse(SkillFactory.createAll().isEmpty());
        assertFalse(OutfitFactory.createAll().isEmpty());
    }

    @Test
    @DisplayName("Cada raza tiene nombre y estadisticas base positivas")
    void cadaRazaTieneDatosValidos() {
        for (Race r : RaceFactory.createAll()) {
            assertNotNull(r.getName(), "raza sin nombre: " + r.getId());
            assertTrue(r.getBaseStrength() > 0, "fuerza base invalida en " + r.getId());
            assertTrue(r.getBaseVitality() > 0, "vitalidad base invalida en " + r.getId());
        }
    }

    @Test
    @DisplayName("Crear una raza por identificador devuelve esa raza")
    void crearRazaPorId() {
        assertEquals("elfo", RaceFactory.createRace("elfo").getId());
        assertEquals("mago", CharacterClassFactory.createClass("mago").getId());
    }

    @Test
    @DisplayName("Un identificador que no existe en el catalogo es un error")
    void idInexistenteEsError() {
        assertThrows(RuntimeException.class, () -> RaceFactory.createRace("no-existe"));
    }

    @Test
    @DisplayName("Las restricciones de habilidades se respetan por raza y clase")
    void restriccionesDeHabilidades() {
        Race elfo = RaceFactory.createRace("elfo");
        Race orco = RaceFactory.createRace("orco");
        CharacterClass mago = CharacterClassFactory.createClass("mago");
        CharacterClass guerrero = CharacterClassFactory.createClass("guerrero");

        Skill bolaDeFuego = SkillFactory.createSkill("bola_de_fuego");
        Skill furia = SkillFactory.createSkill("furia_sangrienta");
        Skill golpe = SkillFactory.createSkill("golpe_certero");

        assertTrue(bolaDeFuego.isAvailableFor(elfo, mago));
        assertFalse(bolaDeFuego.isAvailableFor(elfo, guerrero));
        assertTrue(furia.isAvailableFor(orco, guerrero));
        assertFalse(furia.isAvailableFor(elfo, guerrero));
        assertTrue(golpe.isAvailableFor(elfo, guerrero));
    }

    @Test
    @DisplayName("Cada prenda del vestuario tiene identificador unico")
    void vestuarioConIdsUnicos() {
        List<Outfit> prendas = OutfitFactory.createAll();
        assertEquals(prendas.size(), idsUnicos(OutfitFactory.availableIds()));
    }

    private static int idsUnicos(List<String> ids) {
        Set<String> unicos = new HashSet<>(ids);
        return unicos.size();
    }
}
