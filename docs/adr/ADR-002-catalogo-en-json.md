# ADR-002: El catálogo del juego vive en archivos JSON, no en código

Estado: aceptado e implementado.

Fecha: agosto de 2026.

## Contexto

Las razas, clases, habilidades y piezas de vestuario son el contenido del juego. Al principio estaban escritas en Java: un `enum RaceType` y un `switch` dentro de `RaceFactory` con las estadísticas de cada raza como literales.

El problema apareció al ampliar el contenido. Pasar de 4 razas a 7 y de 3 clases a 6 significaba editar y recompilar las factories, y cada ajuste de un número obligaba a un nuevo build. Además, dos de los tres integrantes no estaban tocando código Java en ese momento, y el contenido era justamente lo que querían ajustar.

## Opciones consideradas

### A. Dejarlo en el código

A favor: el compilador detecta cualquier error de escritura, y el autocompletado del editor ofrece las opciones válidas.

En contra: agregar contenido exige recompilar, y mezcla los datos del juego con la lógica que los construye. El `switch` crece sin límite.

### B. Catálogo en archivos JSON, leído por las factories

A favor: agregar una raza es añadir una entrada a `races.json`. `RaceFactory.java` no cambia. Cualquiera del equipo puede ajustar contenido sin abrir un archivo Java.

En contra: se pierde el chequeo del compilador. Un `itnelligence` en vez de `intelligence` solo se nota al arrancar la aplicación.

## Decisión

Se elige la opción B. Los cuatro catálogos viven en `src/main/resources/data/`: `races.json`, `classes.json`, `skills.json` y `outfits.json`.

Las factories siguen siendo el único sitio que construye los objetos de dominio, así que el patrón Factory Method queda intacto: lo único que cambió es de dónde salen los datos. `CatalogManager` quedó sin ningún dato del juego dentro: solo cachea lo que le dan las factories.

Esto es el principio abierto/cerrado aplicado al contenido. `RaceFactory` está cerrada a modificación y abierta a extensión del catálogo.

## Consecuencias

El contenido creció sin tocar Java: 7 razas, 6 clases, 12 habilidades y 10 piezas de vestuario.

La API de las factories cambió, y fue un cambio incompatible:

| Antes | Ahora |
|---|---|
| `RaceFactory.createRace(RaceType.ELFO)` | `RaceFactory.createRace("elfo")` |
| `CharacterClassFactory.createClass(ClassType.MAGO)` | `CharacterClassFactory.createClass("mago")` |

Los `enum RaceType` y `ClassType` desaparecieron. Cualquier rama sin fusionar en ese momento tuvo que adaptarse.

El catálogo no se valida contra un esquema. Un JSON mal formado tumba la aplicación al arrancar, no antes. Un esquema JSON validado en el build cerraría ese hueco y está pendiente.

Las restricciones de raza y clase por ítem (`allowedRaces`, `allowedClasses`) viajan también en el JSON, y la regla que las interpreta está en un solo sitio, `model/CatalogRestriction`, para que no se desincronice entre `Skill` y `Outfit`.
