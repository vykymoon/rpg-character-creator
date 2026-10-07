# RPG Character Creator

Proyecto de Arquitectura de Software. Integrantes: Victor Luna, Nicolas Salazar y Tiffany Cardona.

## Presentación

Crear un personaje de rol es una barrera de entrada real. Al jugador nuevo lo paraliza una lista larga de razas, clases, habilidades y equipo sin saber qué combinación tiene sentido. Al experimentado le pasa lo contrario: sabe lo que quiere pero comete errores de reglas al armarlo a mano, con combinaciones que el sistema no permite o estadísticas mal calculadas.

Una hoja en papel no valida nada, y el error se descubre jugando. El software sí puede conocer las reglas, validarlas en el momento de elegir y calcular las estadísticas derivadas.

Esta es una aplicación de escritorio en Java 17 y JavaFX que resuelve eso en dos partes:

El creador de personajes es un wizard de cinco pasos con el catálogo de razas, clases, habilidades y vestuario definido en archivos JSON, validación de combinaciones y una galería donde se guardan, clonan y borran personajes.

El modo de juego lleva ese personaje a un mapa navegable, en un jugador o en partida compartida por red local, donde varios jugadores se ven moverse en tiempo real.

## Cómo ejecutar

Requiere Java 17 o superior y Maven.

| Qué | Comando |
|---|---|
| Aplicación: galería y wizard | `mvn clean javafx:run` |
| Servidor de la partida LAN | `mvn compile exec:java@server` |
| Cliente gráfico LAN | `mvn compile javafx:run@game` |
| Pruebas unitarias | `mvn test` |
| Pruebas unitarias y de integración | `mvn verify` |

Para jugar en red, el anfitrión levanta el servidor y cada jugador abre el cliente e ingresa `ws://IP-DEL-ANFITRION:8887`.

Las pruebas de carga necesitan k6 y están explicadas en [`perf/README.md`](perf/README.md).

## Estructura

```
docs/            arquitectura, decisiones y estrategia de pruebas
  adr/           registros de decisiones arquitectónicas
  diagramas/     C4 del multijugador y diagrama de clases
src/main/java/   código de la aplicación
src/test/java/   pruebas unitarias (*Test) y de integración (*IT)
perf/            script de k6, escenarios y resultados de carga
```

## Trazabilidad de retos

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba que lo evidencia | Resultado |
|---|---|---|---|---|---|
| Mapa navegable con movimiento del jugador | Consistencia del estado del mapa | Las reglas de movimiento viven en el dominio, sin JavaFX ni red; el servidor las aplica antes de aceptar cada movimiento | `domain/GameMap.java`, `domain/MovementRules.java`, `application/GameSessionService.java` | `GameMapTest`, `MovementRulesTest`, `GameSessionServiceTest` | 22 pruebas: 20 pasan y 2 omitidas que documentan deuda conocida |
| Conexión de jugadores por LAN | Consistencia de estado entre jugadores | Cliente-servidor con servidor autoritativo ([ADR-001](docs/adr/ADR-001-multijugador-lan.md)) | `adapters/server/GameServer.java`, `adapters/server/client/GameClient.java` | `MultijugadorIT`: servidor real y dos clientes; uno se mueve y el otro recibe el estado | 4 de 4 pasan |
| Conexión de jugadores por LAN | Rendimiento y escalabilidad | WebSocket con difusión del estado en cada movimiento | `adapters/server/GameServer.java` | Carga con k6: 2 y 8 jugadores, más barrido de 2 a 64 | Cumple el SLO: p95 de 43 ms y 0 % de errores con 8 jugadores |
| Conexión de jugadores por LAN | Disponibilidad | El jugador anfitrión corre el servidor | `adapters/server/ServerMain.java` | No probada | Límite conocido: si el anfitrión cierra, la partida termina para todos |
| Catálogo ampliable sin recompilar | Extensibilidad | Catálogo en archivos JSON leídos por las factories ([ADR-002](docs/adr/ADR-002-catalogo-en-json.md)) | `factory/`, `src/main/resources/data/` | Sin prueba automática | Pendiente: es el hueco más grande de la suite |
| Cambiar el almacenamiento sin tocar la interfaz | Mantenibilidad | Persistencia detrás de la interfaz `CharacterDAO` ([ADR-003](docs/adr/ADR-003-persistencia-en-archivo.md)) | `dao/CharacterDAO.java`, `dao/CharacterDAOJson.java` | Sin prueba automática | Pendiente |

## Documentación

| Documento | Qué contiene |
|---|---|
| [docs/arquitectura.md](docs/arquitectura.md) | Estilo elegido, comparación con las alternativas descartadas, capas y límites conocidos |
| [docs/adr/](docs/adr/) | Las tres decisiones arquitectónicas con su contexto, opciones y consecuencias |
| [docs/pruebas.md](docs/pruebas.md) | Estrategia por niveles, qué cubre cada prueba y resultados de carga |
| [docs/patrones-y-solid.md](docs/patrones-y-solid.md) | Patrones aplicados y principios SOLID, con el antes y el después |
| [docs/diagramas/](docs/diagramas/) | Diagramas C4 del multijugador y diagrama de clases del creador |
| [perf/](perf/README.md) | Cómo correr las pruebas de carga y la tabla completa de mediciones |

También está la [wiki del proyecto](https://github.com/vykymoon/rpg-character-creator/wiki), con la guía de uso y el detalle del protocolo de mensajes.

## Roles

| Integrante | Contribución principal |
|---|---|
| Victor Luna | SOLID y patrones de diseño |
| Nicolas Salazar | Reglas de juego, sprites y restricciones |
| Tiffany Cardona | Control de errores, estética y manejo de excepciones |
