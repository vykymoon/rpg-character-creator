# ADR-003: Los personajes se guardan en un archivo JSON detrás de una interfaz

Estado: aceptado e implementado.

Fecha: agosto de 2026.

## Contexto

El creador de personajes tiene que guardar lo que el jugador construye, listarlo en una galería, abrirlo de nuevo y borrarlo. Es un proyecto de escritorio de un solo usuario, sin cuentas ni acceso concurrente.

## Opciones consideradas

### A. Base de datos embebida (SQLite o H2)

A favor: consultas, integridad referencial, y un camino claro si algún día hay muchos personajes.

En contra: una dependencia más, un esquema que mantener y migrar, y nada de eso lo necesita un archivo con unas decenas de personajes de un solo usuario.

### B. Un archivo JSON

A favor: sin dependencias nuevas más allá de Gson, que ya se usa para el catálogo. El archivo se puede abrir y leer a mano, lo que ayuda al depurar.

En contra: hay que leer y escribir el archivo completo en cada operación, y no hay transacciones: una escritura interrumpida deja el archivo a medias.

## Decisión

Se elige la opción B, pero detrás de la interfaz `CharacterDAO`.

La interfaz es la parte importante de la decisión, más que el formato. Declara las cinco operaciones que el sistema necesita y nada más:

```java
public interface CharacterDAO {
    void save(Character character);
    Optional<Character> findById(String id);
    List<Character> findAll();
    void delete(String id);
    boolean existsByName(String name);
}
```

Los controladores declaran el tipo abstracto, no el concreto:

```java
private final CharacterDAO characterDAO = new CharacterDAOJson();
```

`CharacterDAOJson` solo aparece a la derecha del `new`. Ningún controlador pregunta cómo se guarda.

## Consecuencias

La decisión es reversible al costo de una clase. Cambiar a SQLite es escribir `CharacterDAOSqlite implements CharacterDAO` y cambiar el sitio donde se instancia; el paquete `ui/` no se recompila porque nunca nombró la implementación.

Eso es inversión de dependencias: la interfaz pertenece al lado que la usa, y la implementación es un detalle.

El costo es una indirección más. Para entender cómo se guarda un personaje hay que seguir la interfaz hasta su implementación, en vez de leer el código directo.

No hay transacciones. Si el proceso muere a mitad de una escritura, `characters.json` queda incompleto y la galería no abre. Escribir a un archivo temporal y renombrarlo al final lo arreglaría, y está pendiente.

Instanciar `new CharacterDAOJson()` dentro de cada controlador cumple el principio pero deja la elección repetida en tres sitios (`GalleryController`, `Step5SummaryController`, `TemplateGalleryController`). Un único punto de composición que la inyecte sería más limpio.
