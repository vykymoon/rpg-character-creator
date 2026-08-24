# RPG Character Creator (Visual)

Aplicación de escritorio en JavaFX para la creación visual de personajes tipo RPG (razas, clases, habilidades, vestuario). Proyecto académico enfocado en la aplicación de patrones de diseño de software y trabajo colaborativo en Git.

## Stack

- Java 17+
- JavaFX 21 (Controls + FXML + Media)
- Maven
- Gson (persistencia y catálogos en JSON)

## Cómo correr el proyecto

Desde IntelliJ: abre el proyecto, espera a que Maven descargue las dependencias (barra de progreso abajo), y luego corre el objetivo `javafx:run` desde el panel de Maven (lateral derecho → `rpg-character-creator` → `Plugins` → `javafx` → `javafx:run`).

Desde terminal, parado en la carpeta del proyecto:

```bash
mvn clean javafx:run
```

## Qué hace la app

- **Galería de personajes** — pantalla inicial: lista los personajes guardados, permite ver el detalle o eliminarlos.
- **Wizard de creación en 5 pasos** — nombre y raza → clase → habilidades → vestuario → resumen y guardado.
- **Clonado de plantillas NPC** — 3 plantillas precargadas (Bandido, Guardia Real, Mago Errante) que se copian y se guardan con un nombre nuevo.
- **Ventana de detalle** — avatar según la raza, stats en barras de progreso y chips de habilidades y vestuario, con tema retro y sonido de apertura.
- **Exclusividad de ítems** — hay habilidades y prendas que solo están disponibles para ciertas razas o clases.

Toda la interfaz usa un tema pixel art (`css/retro-theme.css` + fuente Press Start 2P).

## Patrones de diseño implementados

| Patrón | Ubicación | Qué resuelve |
|---|---|---|
| **Factory** | `factory/RaceFactory`, `CharacterClassFactory`, `SkillFactory`, `OutfitFactory` | Único punto que instancia los objetos del catálogo. Lee las definiciones de JSON, así que el resto de la app nunca conoce los valores numéricos ni el formato del archivo. |
| **Builder** | `builder/CharacterBuilder.java` | Construye un `Character` paso a paso (nombre → raza → clase → habilidades → vestuario), conectado 1 a 1 con las pantallas del wizard. También valida que los ítems agregados sean compatibles con la raza y la clase. |
| **Prototype** | `prototype/CharacterPrototype.java` | Registro de plantillas de NPCs clonables sin reconstruirlas desde cero (usa `Character.clone()`). Las plantillas se arman con el propio `CharacterBuilder`. |
| **DAO** | `dao/CharacterDAO.java` (interfaz) + `dao/CharacterDAOJson.java` (implementación) | Separa la persistencia del resto de la app. Cambiar a SQLite implica solo crear otra clase que implemente la misma interfaz. |
| **Singleton** | `singleton/CatalogManager.java` | Única instancia en memoria del catálogo completo. No contiene datos del juego: solo cachea lo que le entregan las factories y lo expone. |

## Catálogo data-driven

Las razas, clases, habilidades y vestuario **no están hardcodeadas en el código**. Viven en `src/main/resources/data/`:

| Archivo | Contenido actual |
|---|---|
| `races.json` | 7 razas |
| `classes.json` | 6 clases |
| `skills.json` | 12 habilidades |
| `outfits.json` | 10 piezas de vestuario |

Para agregar contenido basta con editar el JSON correspondiente y volver a correr la app — no hay que tocar Java ni recompilar la lógica.

```json
{
  "id": "elfo",
  "name": "Elfo",
  "description": "Ágil e intelectual, pero físicamente frágil.",
  "baseStrength": 3,
  "baseDexterity": 7,
  "baseIntelligence": 6,
  "baseVitality": 4
}
```

Cada Factory lee su archivo con `getResourceAsStream` (no con rutas de archivo), lo valida al cargar —ids duplicados, campos faltantes— y lo cachea. Si el JSON está mal, la app falla al arrancar con un mensaje que dice qué archivo y qué entrada tienen el problema.

## Exclusividad por raza y clase

`Skill` y `Outfit` pueden declarar `allowedRaces` y `allowedClasses`. Lista vacía significa disponible para todos.

```json
{
  "id": "aliento_de_fuego",
  "name": "Aliento de Fuego",
  "description": "Cono de llamas a corta distancia.",
  "manaCost": 12,
  "allowedRaces": ["draconiano"],
  "allowedClasses": []
}
```

La restricción se aplica en dos niveles:

- **En la UI** — los pasos 3 y 4 filtran la lista según la raza (paso 1) y la clase (paso 2), y avisan cuántos ítems quedaron ocultos.
- **En el Builder** — `addSkill()` y `addOutfit()` lanzan `IllegalStateException` si el ítem no aplica, por si algo llega hasta ahí sin pasar por el filtro. `Step5SummaryController` captura esa excepción y la muestra en un diálogo.

Se acepta escribir la restricción con el id (`"elfo"`) o con el nombre visible (`"Elfo"`), sin distinguir mayúsculas.

## Estructura del proyecto

```
src/main/java/com/proyecto/rpg/
├── model/        Character, Race, CharacterClass, Skill, Outfit,
│                 CatalogRestriction (regla de exclusividad), RequirementLabel
├── factory/      RaceFactory, CharacterClassFactory, SkillFactory,
│                 OutfitFactory, JsonCatalogLoader (lectura común de JSON)
├── builder/      CharacterBuilder
├── prototype/    CharacterPrototype
├── dao/          CharacterDAO (interfaz) + CharacterDAOJson
├── singleton/    CatalogManager
└── ui/
    ├── MainApp                    arranca en la galería
    ├── SceneNavigator             navegación entre pantallas
    ├── WizardSession              estado temporal del wizard
    ├── DialogUtils                diálogos con el tema retro aplicado
    ├── SpriteLoader               carga de sprites con fallback
    ├── GalleryController          pantalla principal
    ├── CharacterDetailController  ventana de detalle
    ├── TemplateGalleryController  clonado de plantillas
    └── Step1..Step5Controller     las 5 pantallas del wizard

src/main/resources/
├── data/      races.json, classes.json, skills.json, outfits.json
│              (characters.json se genera al guardar, ignorado por git)
├── fxml/      gallery, character_detail, template_gallery, step1..step5
├── css/       retro-theme.css
├── fonts/     PressStart2P-Regular.ttf
├── sprites/   10 PNG de vestuario + avatars/ (uno por raza)
└── sounds/    open_detail.wav
```

## Flujo de pantallas

```
Galería (gallery.fxml)
 ├── "+ Crear nuevo personaje" → Wizard de 5 pasos
 │     Step1 (nombre+raza) → Step2 (clase) → Step3 (habilidades)
 │     → Step4 (vestuario) → Step5 (resumen y guardar) → vuelve a Galería
 │
 ├── "Ver detalle" → ventana modal con avatar, stats y equipamiento
 │
 └── "Clonar plantilla" → Pantalla de plantillas (template_gallery.fxml)
       Elegir NPC base → nombre nuevo → clonar y guardar → vuelve a Galería
```

La navegación usa `ui/SceneNavigator`, que reemplaza el contenido de la ventana actual en vez de abrir ventanas nuevas. El estado del personaje mientras se recorre el wizard se guarda en `ui/WizardSession`.

## Notas técnicas

- Los `.fxml` **no declaran `fx:controller`**: el controlador se asigna en Java con `loader.setController(...)`. Es intencional, permite pasarle la `WizardSession` compartida por constructor. IntelliJ marca un falso "no controller specified" que no afecta la compilación ni la ejecución.
- `Character` implementa `Cloneable` para soportar Prototype (`clone()` hace deep copy de las listas de skills y outfits, y genera un id nuevo).
- Los stats finales se calculan en `Character.recalculateStats()` (stats base de raza + bonus de clase), llamado automáticamente al final de `CharacterBuilder.build()`.
- `SpriteLoader` cachea las imágenes y tiene fallback: si un PNG falta o está corrupto, dibuja un recuadro de color con la inicial de la pieza en lugar de fallar.
- Los sprites de vestuario son placeholders de pixel art. Para poner arte definitivo basta con reemplazar el archivo manteniendo el mismo nombre.
- `characters.json` está en `.gitignore` — cada quien genera su propia data local de prueba.
- No hay tests unitarios todavía.

## Pendientes conocidos

- `model/RaceRestriction.java` y `resources/fxml/wizard.fxml` quedaron sin uso de versiones anteriores; se pueden borrar.
- `CharacterDAOJson` guarda en una ruta relativa (`src/main/resources/data/characters.json`), lo que funciona corriendo con `mvn javafx:run` desde la raíz del proyecto pero no si se empaqueta en un `.jar`.
- La clase Mago suma 9 puntos de bonus mientras las otras cinco suman 11.

## Flujo de Git

1. Cada quien trabaja solo en su rama (`feature/...`).
2. Commits pequeños y descriptivos.
3. Al terminar una parte funcional: Pull Request hacia `main`, revisión cruzada por al menos 1 compañero, luego merge.
4. Nunca hacer push directo a `main`.
5. Si necesitas algo de otra rama antes de que se mergee a `main`, avisen por el grupo antes de hacer merge manual entre ramas para evitar conflictos duplicados.
