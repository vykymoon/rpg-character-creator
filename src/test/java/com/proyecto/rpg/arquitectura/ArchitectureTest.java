package com.proyecto.rpg.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Funciones de aptitud (fitness functions) de la arquitectura, Corte 3.
 *
 * No prueban logica de negocio: prueban que el codigo respete las fronteras
 * que fijaron los ADR. Corren en la etapa "Reglas de arquitectura" del
 * pipeline (ci.yml) y lo detienen si alguien rompe una frontera. Cada regla
 * dice de que ADR sale y que pasaria si se violara.
 */
@AnalyzeClasses(packages = "com.proyecto.rpg", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * ADR-001. El dominio del multijugador solo depende de si mismo y del JDK.
     * Es lo que permite probar las reglas del juego sin red ni interfaz: si una
     * clase de domain importa Javalin, Gson o JavaFX, el pipeline se detiene.
     */
    @ArchTest
    static final ArchRule dominio_no_depende_de_infraestructura =
            classes().that().resideInAPackage("com.proyecto.rpg.domain..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("com.proyecto.rpg.domain..", "java..")
                    .because("ADR-001: las reglas del juego se prueban sin red ni interfaz grafica");

    /**
     * ADR-001. El caso de uso (fuente de verdad) no conoce el transporte. Cambiar
     * Java-WebSocket por Javalin en el Corte 3 (ADR-006) solo toco adapters/
     * gracias a esta frontera.
     */
    @ArchTest
    static final ArchRule caso_de_uso_no_conoce_los_adaptadores =
            noClasses().that().resideInAPackage("com.proyecto.rpg.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.proyecto.rpg.adapters..",
                            "com.proyecto.rpg.ui..",
                            "io.javalin..",
                            "org.eclipse.jetty..",
                            "org.java_websocket..",
                            "javafx..")
                    .because("ADR-001: el transporte es un adaptador intercambiable");

    /**
     * ADR-001 y ADR-004. El servidor se despliega en un contenedor sin JavaFX
     * (el Dockerfile excluye org.openjfx). Si el adaptador del servidor
     * dependiera de la interfaz grafica, la imagen no arrancaria.
     */
    @ArchTest
    static final ArchRule servidor_no_depende_de_la_interfaz_grafica =
            noClasses().that().resideInAPackage("com.proyecto.rpg.adapters.server..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("javafx..", "com.proyecto.rpg.ui..", "com.proyecto.rpg.game..",
                            "com.proyecto.rpg.adapters.client..")
                    .because("ADR-004: la imagen del servidor no incluye JavaFX");

    /**
     * ADR-003. La interfaz grafica conoce la interfaz CharacterDAO, no el archivo
     * JSON. En el Corte 2 tres controladores hacian new CharacterDAOJson(); el
     * Corte 3 los paso a CharacterDAOProvider y esta regla impide que vuelva.
     */
    @ArchTest
    static final ArchRule ui_no_depende_de_la_persistencia_concreta =
            noClasses().that().resideInAPackage("com.proyecto.rpg.ui..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.proyecto.rpg.dao.CharacterDAOJson")
                    .because("ADR-003: cambiar a SQLite no debe tocar la interfaz grafica");

    /**
     * Arquitectura en capas del creador (docs/arquitectura.md). El modelo es la
     * capa de abajo: no sabe nada de la interfaz, del almacenamiento ni del
     * formato JSON.
     */
    @ArchTest
    static final ArchRule modelo_no_depende_de_capas_superiores =
            noClasses().that().resideInAPackage("com.proyecto.rpg.model..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.proyecto.rpg.ui..",
                            "com.proyecto.rpg.dao..",
                            "com.proyecto.rpg.factory..",
                            "javafx..",
                            "com.google.gson..")
                    .because("el modelo es la capa inferior del creador de personajes");
}
