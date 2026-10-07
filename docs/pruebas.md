# Estrategia y resultados de pruebas

## Niveles

| Nivel | Qué ejecuta de verdad | Qué detecta | Dónde |
|---|---|---|---|
| Unitaria | Una clase del dominio aislada | Errores en las reglas del juego | `src/test/java/.../domain`, `.../application` |
| Integración | Servidor y clientes reales por WebSocket | Que el estado llegue a los demás jugadores | `src/test/java/.../adapters` |
| Carga | Servidor real con N jugadores simultáneos | Límites de latencia y saturación | `perf/` |

La convención de nombres decide quién ejecuta qué. Las clases que terminan en `Test` son unitarias y las corre Surefire en `mvn test`; las que terminan en `IT` abren puertos de red y las corre Failsafe en `mvn verify`. Una prueba que abre un socket no debe llamarse `Test`: volvería lento y frágil el ciclo rápido de desarrollo.

```
mvn test      solo unitarias
mvn verify    unitarias e integración
```

## Pruebas unitarias

Prueban el dominio sin red ni interfaz gráfica.

| Clase | Qué verifica | Pruebas |
|---|---|---|
| `GameMapTest` | Dimensiones, borde de piedra bloqueado, zona de aparición despejada, agua bloqueada, caminos transitables, coordenadas fuera del mapa y que el terreno se genere siempre igual | 7 |
| `MovementRulesTest` | Casilla caminable, camino, fuera del mapa, piedra y agua | 5 |
| `GameSessionServiceTest` | Unirse, unirse con personaje, movimiento válido, movimiento a casilla bloqueada, movimiento fuera del mapa, quitar jugador, unirse dos veces, contenido del mensaje de mapa, y dos casos de deuda desactivados | 10 |

No se usó Mockito. `GameSessionService` no tiene colaboradores externos que valga la pena simular, y el mapa se genera con semilla fija, así que se prueba contra el mapa real. Un doble del mapa solo confirmaría que el servicio llama a `isValidMove`, no que respete el terreno — que es justo lo que interesa.

Las aserciones se hacen sobre el JSON que el servidor emite, no sobre campos internos. Ese JSON es el contrato que ven los clientes; probarlo significa que un cambio en la forma del mensaje rompe una prueba en vez de romper al cliente en una partida.

## Prueba de integración

`MultijugadorIT` levanta el `GameServer` real en un puerto libre y conecta clientes `GameClient` reales. Es caja negra: solo usa los mensajes del protocolo.

| Prueba | Qué cubre que una unitaria no puede |
|---|---|
| Al unirse, el jugador recibe el mapa | Que el servidor responda al `join`, no solo que el servicio lo registre |
| Cuando un jugador se mueve, el otro recibe la posición nueva | Que el estado se difunda de verdad por la red |
| Un movimiento inválido no cambia la posición que ven los demás | Que el rechazo llegue hasta la pantalla del otro jugador |
| Cuando un jugador se desconecta, desaparece del estado | Que el cierre del socket limpie el estado compartido |

La diferencia con las unitarias está en la tercera: una unitaria puede demostrar que `move()` devuelve `false` ante una casilla bloqueada, pero no que el otro jugador siga viendo al primero en su sitio. Eso solo se ve con el servidor y dos clientes de verdad.

## Las dos pruebas desactivadas

`GameSessionServiceTest` tiene dos pruebas con `@Disabled`. No están rotas: documentan comportamiento que un servidor autoritativo debería tener y el código todavía no cumple.

La primera es que `move()` acepta saltar a cualquier casilla caminable del mapa, no solo a una vecina. Un cliente modificado puede cruzar el mapa con un solo mensaje.

La segunda es que `move()` crea al jugador aunque nunca haya hecho `join`.

Se activan quitando la anotación cuando se corrija `GameSessionService.move()`. Dejarlas escritas y desactivadas vale más que una lista de pendientes en un documento: el día que alguien arregle el método, la prueba ya está ahí para confirmarlo.

## Resultados de la ejecución

Salida de `mvn verify` el 7 de octubre de 2026:

| Tipo | Ejecutadas | Pasan | Fallan | Omitidas |
|---|---|---|---|---|
| Unitarias (Surefire) | 22 | 20 | 0 | 2 |
| Integración (Failsafe) | 4 | 4 | 0 | 0 |

Las dos omitidas son las marcadas con `@Disabled`, que documentan deuda conocida y no fallos. La prueba de integración levanta cuatro veces el servidor en puertos distintos y tarda menos de un segundo.

## Comprobar que las pruebas sirven

Una prueba que nunca se ha visto en rojo es una hipótesis sin comprobar. Para verificar que esta suite detecta errores, conviene romper el código a propósito y confirmar que falla:

Cambiar `MovementRules.isValidMove` para que devuelva siempre `true` debe tumbar las pruebas de rechazo de `MovementRulesTest`, las de casilla bloqueada de `GameSessionServiceTest` y la de movimiento inválido de `MultijugadorIT`.

Después se revierte el cambio y se confirma que todo vuelve a verde.

## Pruebas de carga

Los escenarios, el script de k6 y la tabla completa de mediciones están en [`perf/`](../perf/README.md). El resumen:

| Escenario | Carga | p95 | Errores | SLO |
|---|---|---|---|---|
| oficial-baseline | 2 jugadores, 200 ms | 3 ms | 0 % | Cumple |
| oficial-carga | 8 jugadores, 200 ms, 3 min | 43 ms | 0 % | Cumple |
| jug-32 | 32 jugadores, 200 ms | 55 ms | 0 % | Cumple |
| jug-48 | 48 jugadores, 200 ms | 689 ms | 0,15 % | No cumple |
| jug-64 | 64 jugadores, 200 ms | 1534 ms | 8,75 % | No cumple |

El SLO del ADR-001 es p95 ≤ 150 ms y error < 1 %.

Tres cosas salieron de medir, y ninguna era la esperada.

El sistema aguanta hasta 32 jugadores y se cae en 48. Pero el cuello de botella no es la CPU, que en el colapso está al 32 %: lo que se estanca es el throughput de red en un techo de unos 45.500 KB/s. El servidor difunde el estado completo a todos en cada movimiento, así que el tráfico crece con el cuadrado de los jugadores.

El límite real son los paquetes por segundo, no los jugadores. 32 jugadores enviando cada 100 ms llegan al mismo techo que 48 enviando cada 200 ms. Bajar la frecuencia a 500 ms devuelve esos mismos 32 jugadores a zona sana.

Y los 45 ms de latencia que aparecían con 4 jugadores o más no venían del código sino del sistema operativo. Desactivar el algoritmo de Nagle baja el p95 de 45–55 ms a 3–9 ms en todos los escenarios hasta 32 jugadores. El retraso era el sistema operativo agrupando paquetes pequeños antes de enviarlos. Pero no salva del colapso: con 48 y 64 jugadores el resultado es igual con y sin Nagle, lo que confirma que el techo es del modelo de difusión y no de la configuración del socket.

## Lo que no está probado

El wizard de creación de personajes no tiene pruebas. Las reglas que importan ahí son las restricciones de raza y clase (`CatalogRestriction`) y la validación del `CharacterBuilder`, y ambas son lógica pura, fáciles de probar sin JavaFX. Es el hueco más grande que queda.

La persistencia (`CharacterDAOJson`) tampoco. Una prueba de ida y vuelta sobre un archivo temporal cubriría que lo que se guarda es lo que se lee.

La interfaz gráfica no tiene pruebas automáticas y no está previsto que las tenga: el esfuerzo de TestFX no se justifica para el alcance de este proyecto.
