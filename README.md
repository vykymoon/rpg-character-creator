# RPG Character Creator (Visual)

Aplicación de escritorio en JavaFX para la creación visual de personajes tipo RPG (razas, clases, habilidades, vestuario). Proyecto académico enfocado en la aplicación de patrones de diseño de software y trabajo colaborativo en Git.

## Stack

- Java 17+
- JavaFX 21 (Controls + FXML)
- Maven
- Gson (persistencia en JSON)

## Cómo correr el proyecto

Desde IntelliJ: abre el proyecto, espera a que Maven descargue las dependencias (barra de progreso abajo), y luego corre el objetivo `javafx:run` desde el panel de Maven (lateral derecho → `rpg-character-creator` → `Plugins` → `javafx` → `javafx:run`).

Desde terminal, parado en la carpeta del proyecto:

```bash
mvn clean javafx:run
```

## Patrones de diseño implementados

| Patrón | Ubicación | Qué resuelve |
|---|---|---|
| **Factory** | `factory/RaceFactory.java`, `factory/CharacterClassFactory.java` | Crea razas y clases con stats predefinidos sin exponer los valores numéricos al resto de la app. |
| **Builder** | `builder/CharacterBuilder.java` | Construye un `Character` paso a paso (nombre → raza → clase → habilidades → vestuario), pensado para conectarse con un wizard visual. |
| **Prototype** | `prototype/CharacterPrototype.java` | Registro de plantillas de personajes/NPCs clonables sin reconstruir desde cero (usa `Character.clone()`). |
| **DAO** | `dao/CharacterDAO.java` (interfaz) + `dao/CharacterDAOJson.java` (implementación) | Separa la persistencia (JSON) del resto de la app. Cambiar a SQLite implica solo crear otra clase que implemente la misma interfaz. |
| **Singleton** | `singleton/CatalogManager.java` | Única instancia en memoria del catálogo de razas, clases y habilidades. |

## Estructura del proyecto

```
src/main/java/com/proyecto/rpg/
├── model/          Character, Race, CharacterClass, Skill, Outfit
├── factory/         RaceFactory, CharacterClassFactory
├── builder/          CharacterBuilder
├── prototype/        CharacterPrototype
├── dao/             CharacterDAO (interfaz) + CharacterDAOJson
├── singleton/        CatalogManager
└── ui/              MainApp, WizardController
src/main/resources/
├── fxml/            wizard.fxml
└── data/            characters.json (se genera al guardar, ignorado por git)
```


## Flujo de Git

1. Cada quien trabaja solo en su rama (`feature/factory`, `feature/builder`, `feature/dao`).
2. Commits pequeños y descriptivos.
3. Al terminar una parte funcional: Pull Request hacia `main`, revisión cruzada por al menos 1 compañero, luego merge.
4. Nunca hacer push directo a `main`.
5. Si necesitas algo de otra rama antes de que se mergee a `main` (ej. Persona B necesita un cambio de Persona A en modelos), avisen por el grupo antes de hacer merge manual entre ramas para evitar conflictos duplicados.

## Notas técnicas

- `Character` implementa `Cloneable` para soportar el patrón Prototype directamente (`character.clone()` hace deep copy de las listas de skills/outfits).
- `characters.json` está en `.gitignore` — cada quien genera su propia data local de prueba, no se versiona.
- Los stats finales de un personaje se calculan en `Character.recalculateStats()` (suma stats base de raza + bonus de clase), llamado automáticamente al final de `CharacterBuilder.build()`.
