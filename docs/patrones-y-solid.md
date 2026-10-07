# Patrones de diseño y principios SOLID

Documentación de diseño del creador de personajes (Corte 1). Estaba en el README y se movió aquí al reorganizar la documentación.

## Atributos de calidad y lo que costó cada uno

| Atributo | Cómo se sostiene en el diseño | Qué se sacrificó |
|---|---|---|
| Mantenibilidad | La interfaz `CharacterDAO` separa la lógica de negocio de la persistencia. Cambiar de JSON a SQLite es crear `CharacterDAOSqlite implements CharacterDAO`, sin tocar la interfaz gráfica ni el `CharacterBuilder`. | Una indirección más: para entender cómo se guarda un personaje hay que seguir la interfaz hasta su implementación. |
| Extensibilidad | Las cuatro factories leen su catálogo de archivos JSON en vez de tener un `switch` escrito en Java. Agregar una raza es agregar una entrada al archivo. | El catálogo pierde el chequeo del compilador: un `itnelligence` solo se detecta al arrancar. |
| Consistencia de datos | `CatalogManager` es un Singleton que carga el catálogo una vez y lo comparte entre todas las pantallas. Ninguna pantalla puede tener una copia desactualizada. | Estado global compartido: si algo muta esas listas en memoria, todas las pantallas lo ven. |
| Corrección de las reglas | `CatalogRestriction` concentra la validación de si una habilidad o una pieza de vestuario está permitida para una raza o clase, en vez de repetirla en cada controlador. | Un punto único de fallo: un error ahí afecta a la vez la validación de habilidades y de vestuario. |

## Principios SOLID

### Abierto/cerrado

Antes, `RaceFactory` tenía las razas escritas en un `switch`:

```java
public static Race createRace(String id) {
    switch (id) {
        case "humano": return new Race("humano", "Humano", 10, 10, 10, 10);
        case "elfo":   return new Race("elfo", "Elfo", 8, 15, 12, 8);
        // cada raza nueva obligaba a editar y recompilar esta clase
    }
}
```

Ahora lee el catálogo:

```java
public static Race createRace(String id) {
    RaceDefinition definition = definitions().get(normalize(id));
    if (definition == null) {
        throw new IllegalArgumentException("Raza no soportada: '" + id + "'.");
    }
    return build(definition);
}
```

Las razas viven en `src/main/resources/data/races.json`. Agregar, quitar o ajustar una raza es editar el archivo: `RaceFactory.java` no cambia. La clase queda cerrada a modificación y abierta a extensión del catálogo. La decisión completa está en [ADR-002](adr/ADR-002-catalogo-en-json.md).

### Inversión de dependencias

`CharacterDAO` es la abstracción de la que depende el resto del sistema; `CharacterDAOJson` es un detalle:

```java
public interface CharacterDAO {
    void save(Character character);
    Optional<Character> findById(String id);
    List<Character> findAll();
    void delete(String id);
    boolean existsByName(String name);
}
```

Los controladores declaran el tipo abstracto:

```java
private final CharacterDAO characterDAO = new CharacterDAOJson();
```

`CharacterDAOJson` solo aparece a la derecha del `new`. Ningún controlador pregunta si el guardado es en JSON. Aplica en `GalleryController`, `Step5SummaryController` y `TemplateGalleryController`. Ver [ADR-003](adr/ADR-003-persistencia-en-archivo.md).

### Responsabilidad única

El wizard está partido en un controlador por pantalla en vez de un `WizardController` gigante. `Step2ClassController` solo sabe llenar el combo de clases, validar la selección y avanzar: no sabe nada de JSON ni de las otras pantallas.

## Patrones de diseño

| Patrón | Categoría | Qué resuelve aquí | Por qué no la alternativa |
|---|---|---|---|
| Builder (`CharacterBuilder`) | Creacional | `Character` tiene muchos campos que se van llenando pantalla por pantalla del wizard. El builder permite construirlo por pasos y validar antes de `build()`. | Un constructor con todos los parámetros se vuelve ilegible con cinco campos opcionales, y no valida el orden que impone el wizard. |
| Factory Method (las cuatro factories) | Creacional | Crear `Race`, `CharacterClass`, `Skill` y `Outfit` sin acoplar la interfaz gráfica a la construcción concreta, y dejando que el catálogo crezca vía JSON. | Abstract Factory no aporta: no hay familias de objetos que deban crearse juntas, cada factory produce un tipo independiente. |
| Prototype (`CharacterPrototype`) | Creacional | Clonar un personaje ya creado para generar plantillas o NPCs rápido, sin repetir el wizard. | Reconstruirlo con el builder sería repetir trabajo hecho cuando lo que se necesita es una copia con ajustes. |
| Singleton (`CatalogManager`) | Creacional | Una única instancia cacheada del catálogo, compartida por todas las pantallas, sin releer los JSON en cada una. | Pasar el catálogo como parámetro obligaría a inyectarlo en cada `Step*Controller`, añadiendo acoplamiento sin necesidad. |
| DAO (`CharacterDAO` / `CharacterDAOJson`) | Estructural | Separar la lógica y la interfaz del detalle de cómo se persisten los personajes. | El acceso directo a archivos desde los controladores acoplaría la interfaz al formato y rompería la inversión de dependencias. |

## Cohesión y acoplamiento

Cada clase de `model` contiene los datos y el comportamiento de un solo concepto: ninguna sabe cómo se persiste ni cómo se dibuja.

El paquete `ui` nunca importa `CharacterDAOJson` como tipo declarado, solo `CharacterDAO`. Se podría reemplazar la persistencia sin recompilar un solo archivo de `ui`.

`CatalogRestriction` extrae la regla de disponibilidad a un solo sitio en vez de duplicarla entre `Skill` y `Outfit`. Si la regla cambia, se edita un archivo.

## Trazabilidad entre el diagrama de clases y el código

| Clase en el diagrama | Archivo |
|---|---|
| `Character` | `model/Character.java` |
| `Race` | `model/Race.java` |
| `CharacterClass` | `model/CharacterClass.java` |
| `Skill` | `model/Skill.java` |
| `Outfit` | `model/Outfit.java` |
| `CatalogRestriction` | `model/CatalogRestriction.java` |
| `CharacterBuilder` | `builder/CharacterBuilder.java` |
| `CharacterPrototype` | `prototype/CharacterPrototype.java` |
| `CatalogManager` | `singleton/CatalogManager.java` |
| `RaceFactory` | `factory/RaceFactory.java` |
| `CharacterClassFactory` | `factory/CharacterClassFactory.java` |
| `SkillFactory` | `factory/SkillFactory.java` |
| `OutfitFactory` | `factory/OutfitFactory.java` |
| `JsonCatalogLoader` | `factory/JsonCatalogLoader.java` |
| `CharacterDAO` | `dao/CharacterDAO.java` |
| `CharacterDAOJson` | `dao/CharacterDAOJson.java` |
| `WizardSession` | `ui/WizardSession.java` |
| `StepControllers (1..5)` | `ui/Step1NameRaceController.java` a `ui/Step5SummaryController.java` |
| `GalleryController` | `ui/GalleryController.java` |
| `SceneNavigator` | `ui/SceneNavigator.java` |
| `MainApp` | `ui/MainApp.java` |

El diagrama está en [`diagramas/UML.png`](diagramas/UML.png).

## Límites del diseño del creador

El catálogo no se valida contra un esquema formal: un JSON mal formado solo se detecta al arrancar.

`CatalogManager` cachea el catálogo en memoria, así que no refleja cambios en los archivos si se editan con la aplicación abierta.

No hay una capa de reglas de negocio separada de `model/`. Agregar una validación compleja probablemente seguiría requiriendo tocar `CatalogRestriction` en vez de extenderla.
