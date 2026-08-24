# Cambios sobre `main`

Tres tandas de trabajo, todas verificadas sobre el árbol ya fusionado con
`8d2fbdf` (hotfix estética del Paso 5).

Extraer el zip sobre la raíz del repo (sobrescribe y agrega archivos).

---

## 1. Catálogo data-driven

Antes: razas y clases eran un `enum` + `switch` con los stats
hardcodeados; habilidades y vestuario eran `new Skill(...)` sueltos
dentro de `CatalogManager`.

Ahora los cuatro catálogos viven en `src/main/resources/data/`:
`races.json`, `classes.json`, `skills.json`, `outfits.json`. Cada uno
tiene su Factory, que sigue siendo el único punto que instancia el objeto
de dominio — el patrón queda intacto, solo cambió de dónde salen los
datos. Agregar contenido ya no requiere tocar Java.

`CatalogManager` quedó sin ningún dato del juego adentro: solo cachea lo
que le dan las factories y lo expone.

**API que cambió** (importante si alguien tiene ramas sin mergear):

```java
// antes
RaceFactory.createRace(RaceFactory.RaceType.ELFO)
CharacterClassFactory.createClass(CharacterClassFactory.ClassType.MAGO)

// ahora
RaceFactory.createRace("elfo")
CharacterClassFactory.createClass("mago")
```

El `enum RaceType` / `ClassType` ya no existe.

## 2. Sprites de vestuario

Los `spriteRef` apuntaban a PNG inexistentes. Ahora hay 10 PNG reales en
`src/main/resources/sprites/` (pixel art 64×64, fondo transparente): son
placeholders, se reemplazan por arte definitivo manteniendo el nombre de
archivo, sin tocar código.

Además `ui/SpriteLoader.java` tiene fallback: si un PNG falta o está
corrupto, dibuja un recuadro de color según el slot con la inicial de la
pieza, en vez de fallar o dejar el hueco. El Paso 4 muestra sprite +
nombre + slot en cada fila.

## 3. Exclusividad por raza y por clase

Implementa `handoff-exclusividad-items.md` y además la parte que ese
handoff dejaba fuera de alcance (exclusividad por clase), porque con solo
dos ítems restringidos casi no se notaba la funcionalidad.

- `Skill` y `Outfit` tienen `allowedRaces` y `allowedClasses`. Vacío o
  null = disponible para todos.
- `model/CatalogRestriction.java` concentra la regla, para que no esté
  duplicada entre Skill y Outfit.
- Pasos 3 y 4: filtran la lista por la raza (Paso 1) y la clase (Paso 2),
  y avisan cuántos ítems quedaron ocultos.
- `CharacterBuilder.addSkill/addOutfit`: validación dura, lanza
  `IllegalStateException` si el ítem no aplica. `Step5SummaryController`
  ya la captura y la muestra con `DialogUtils.warning` — no se tocó.

## 4. Contenido ampliado

| Catálogo | Antes | Ahora |
|---|---|---|
| Razas | 4 | 7 (+ Mediano, Draconiano, Gnomo) |
| Clases | 3 | 6 (+ Clérigo, Arquero, Druida) |
| Habilidades | 4 | 12 |
| Vestuario | 5 | 10 |

**Exclusivas por raza:** Furia Sangrienta (Orco), Aliento de Fuego y
Yelmo Dracónico (Draconiano), Resistencia Pétrea (Enano), Truco Menor
(Gnomo), Capa Élfica (Elfo), Sigilo (Elfo/Humano/Mediano).

**Exclusivas por clase:** Bola de Fuego y Paso Sombrío (Mago/Pícaro),
Curación Menor y Enraizar (Clérigo/Druida), Vista de Águila (Arquero),
Golpe de Escudo (Guerrero), Armadura de Placas y Escudo de Roble
(Guerrero/Clérigo), Túnica de Mago y Manto Rúnico (Mago/Druida),
Capucha de Sombras (Pícaro).

Todas las restricciones se editan en los JSON, sin recompilar nada.

---

## Decisiones que conviene revisar en el PR

1. **Se acepta id o nombre en las restricciones.** El handoff proponía
   comparar contra `Race.getName()`. Con el catálogo data-driven eso es
   frágil: renombrar una raza en `races.json` rompería las restricciones
   en silencio. La comparación acepta el id (`"elfo"`) o el nombre
   (`"Elfo"`), sin distinguir mayúsculas.

2. **`allowedRaces == null` cuenta como "sin restricción".** Los
   personajes guardados antes de este cambio no tienen el campo en
   `characters.json` y Gson lo deja en `null`. Comprobar solo `isEmpty()`
   habría tirado `NullPointerException` al abrir un personaje viejo.

3. **Skills y vestuario también pasaron a JSON**, cosa que el handoff de
   exclusividad marcaba como fuera de alcance. Con 12 habilidades y 10
   prendas, mantenerlas hardcodeadas era peor.

4. **Los ítems restringidos se ocultan, no se muestran en gris.** Es lo
   que pedía el handoff. Si prefieren mostrarlos deshabilitados con el
   motivo al lado, `Skill.getRequirementLabel()` ya devuelve el texto
   ("Solo Elfo", "Solo Mago, Druida") y solo habría que cambiar el
   `cellFactory`.

## Verificación hecha

- 30 archivos `.java` parsean sin errores de sintaxis.
- Los JSON son válidos y sus campos coinciden 1:1 con los DTO.
- Todos los ids referenciados en restricciones existen en el catálogo.
- Los 10 `spriteRef` tienen archivo real.
- Ninguna de las 42 combinaciones raza×clase queda sin nada que elegir
  (el mínimo es 3 habilidades y 3 prendas).
- Las 3 plantillas NPC del Prototype pasan la validación del Builder.

**No se pudo compilar ni ejecutar** (el entorno donde se hicieron estos
cambios no tiene JDK ni acceso a Maven Central). Correr
`mvn clean javafx:run` antes de mergear.

## Detalle menor preexistente

La clase Mago suma 9 puntos de bonus mientras las otras cinco suman 11.
Viene del código original, no lo toqué por si es intencional.
