# ADR-001: Estilo arquitectónico para el multijugador LAN

Estado: aceptado e implementado (commit `827ab59`).

Fecha: septiembre de 2026.

## Contexto

En el Corte 1 la aplicación era de un solo jugador: el creador de personajes guardaba en un archivo local y nadie más veía ese estado. El reto del Corte 2 es que varios jugadores, cada uno en su propia instancia de la aplicación, compartan el mismo mapa por red local y se vean moverse.

Eso obliga a sincronizar estado entre procesos independientes, que es un problema nuevo: hasta ahora no había dos copias de nada.

## Opciones consideradas

### A. Punto a punto

Cada cliente mantiene su propia copia del mapa y anuncia su posición a los demás. No hay servidor.

A favor: no hay punto único de falla, y nadie tiene que hacer de anfitrión.

En contra: cada cliente decide su propia posición, así que dos jugadores pueden terminar viendo cosas distintas sin que nada lo detecte. Probarlo exige levantar varios clientes y comparar sus estados entre sí. Y un cliente modificado puede anunciar cualquier posición, incluso dentro del agua.

### B. Cliente-servidor con servidor autoritativo

Un servidor guarda el estado real, valida cada movimiento contra las reglas del dominio y difunde el resultado a todos.

A favor: una sola fuente de verdad, así que la consistencia es una propiedad del diseño y no algo que haya que vigilar. Las reglas se prueban sin levantar red. Un cliente modificado no logra nada: el servidor rechaza el movimiento inválido.

En contra: el servidor es punto único de falla, y alguien tiene que correrlo.

## Decisión

Se elige la opción B.

El argumento decisivo fue la testabilidad, no el rendimiento. Con el estado en un solo sitio, `MovementRules` y `GameSessionService` se prueban con pruebas unitarias normales, y la prueba de integración solo tiene que verificar que ese estado llega a los demás jugadores. Con punto a punto, cada prueba habría necesitado varios clientes y una forma de comparar sus estados.

La comunicación es por WebSocket (Java-WebSocket) con mensajes JSON:

| Dirección | Tipo | Contenido |
|---|---|---|
| Cliente a servidor | `join` | `playerId` y, si lo hay, el personaje elegido |
| Cliente a servidor | `move` | `playerId`, `x`, `y` |
| Servidor a cliente | `map` | dimensiones y filas del terreno |
| Servidor a cliente | `state` | posición y datos de todos los jugadores |

El código queda en tres capas: `adapters/` para la red, `application/GameSessionService` para el estado, `domain/` para las reglas. El dominio no depende de JavaFX ni de WebSocket, que es lo que permite probarlo solo.

## Objetivos de servicio

p95 de latencia de movimiento por debajo de 150 ms y menos del 1 % de movimientos sin confirmar, con la carga esperada de una partida local.

## Consecuencias

Todos los jugadores ven el mismo estado, porque solo el servidor lo modifica.

Si el jugador que hace de anfitrión cierra la aplicación, la partida termina para todos. Es el costo aceptado de la decisión.

El estado completo se difunde a todos en cada movimiento. Las mediciones ([`docs/pruebas.md`](../pruebas.md)) confirman que con 8 jugadores el p95 es de 43 ms, muy por debajo del objetivo, pero que el sistema se cae entre 32 y 48 jugadores por volumen de tráfico. Para una partida local es más que suficiente; para una sala grande habría que enviar solo lo que cambió en vez del estado completo.

El servidor se arranca aparte con `mvn compile exec:java@server`, no desde la aplicación. Es una molestia de uso que quedó pendiente.

## Pendientes

El servidor no comprueba que el destino de un movimiento sea una casilla vecina de la posición actual: acepta cualquier casilla caminable del mapa.

El servidor crea al jugador si recibe un `move` de alguien que nunca hizo `join`.

Las dos cosas tienen una prueba escrita y desactivada en `GameSessionServiceTest`, que se activa quitando la anotación cuando se corrija `GameSessionService.move()`.
