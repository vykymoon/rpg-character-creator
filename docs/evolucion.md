# Evolución del sistema en los tres cortes

## Resumen

| | Corte 1: diseño | Corte 2: arquitectura y pruebas | Corte 3: entrega continua y segura |
|---|---|---|---|
| Problema | Crear un personaje sin errores de reglas | Varios jugadores en el mismo mapa por red local | Que cada cambio llegue a producción probado, seguro y sin pasos manuales |
| Qué se construyó | Wizard de 5 pasos, galería, catálogo y persistencia | Servidor autoritativo WebSocket, mapa y movimiento | Contenedor, pipeline con 14 etapas, despliegue en Render, cliente en Releases |
| Estilo | Capas + patrones GoF (Builder, Factory, Singleton, Prototype, DAO) | Cliente-servidor autoritativo con puertos y adaptadores (ADR-001) | El mismo, verificado en cada cambio con ArchUnit |
| Calidad | Validación en el Builder | 22 unitarias, 4 de integración, carga con k6 | 45 unitarias, 11 de integración, 5 reglas de arquitectura, SLO en el pipeline, 4 controles de seguridad + DAST |
| Dónde corre | PC del jugador | PC del anfitrión (servidor) y de los jugadores | Servidor en Render; cliente descargado de Releases |
| Cómo se verifica | A mano | A mano, en el PC de un integrante | GitHub Actions en cada push y PR |

## Corte 1: diseño

El reto era la barrera de entrada de crear un personaje: demasiadas opciones y reglas que una hoja de papel no valida. Se diseñó un wizard en capas: la UI en JavaFX, el modelo y los patrones.

- `CharacterBuilder` valida la combinación.
- Las factories crean los objetos del catálogo.
- `CatalogManager` (Singleton) lo cachea.
- `CharacterPrototype` clona plantillas.
- `CharacterDAO` aísla la persistencia.

Decisiones que se mantuvieron hasta el final:

- **Catálogo en JSON** ([ADR-002](adr/ADR-002-catalogo-en-json.md)). En el Corte 3 se volvió verificable: `CatalogoJsonTest` falla si alguien deja un JSON roto o un id repetido.
- **Persistencia detrás de una interfaz** ([ADR-003](adr/ADR-003-persistencia-en-archivo.md)). En el Corte 3 se descubrió que la interfaz existía pero la UI la saltaba (sección "Pendientes"), y ahora una regla de ArchUnit la protege.

## Corte 2: arquitectura y pruebas

El reto nuevo era compartir el mapa entre procesos. Se eligió cliente-servidor con servidor autoritativo en lugar de punto a punto ([ADR-001](adr/ADR-001-multijugador-lan.md)). El argumento decisivo fue la testabilidad: con el estado en un solo sitio, las reglas se prueban sin red.

Se midió el SLO (p95 < 150 ms, error < 1 %). El servidor lo cumple hasta 32 jugadores y colapsa entre 32 y 48 por el volumen de la difusión, no por la CPU.

Se dejaron documentados, con pruebas desactivadas, dos defectos del servidor y varios huecos de la suite.

## Corte 3: entrega continua y segura

Las verificaciones del Corte 2 pasaron a ser automáticas y obligatorias para llegar a `main` (ver [docs/devops.md](devops.md)).

- **Contenedor del servidor.** Dockerfile multi-etapa, sin root, con healthcheck. Compose levanta el sistema con un comando.
- **Pipeline.**
  - CI: construcción, unitarias con umbral de cobertura, arquitectura, integración, SAST, SCA, secretos, imagen.
  - CD: GHCR, Render, humo, carga con el SLO y DAST.
  - Reversión, versiones y cliente en Releases.
- **Arquitectura verificada.** Cinco reglas de ArchUnit que salen de los ADR. El SLO es el *threshold* de k6 contra el entorno desplegado.
- **DevSecOps.** Modelo STRIDE con 10 amenazas del sistema, controles como *quality gates* y un PR bloqueado como evidencia ([docs/seguridad.md](seguridad.md)).
- **Bonificaciones.** Observabilidad (Prometheus y Grafana con el SLO), Kubernetes en kind y DAST con ZAP.

### Decisiones que cambiaron y por qué

| Decisión | Antes | Ahora | Por qué |
|---|---|---|---|
| Adaptador del servidor | Java-WebSocket (`WebSocketServer`) | Javalin / Jetty 12, HTTP y WebSocket en un puerto ([ADR-006](adr/ADR-006-adaptador-http-websocket.md)) | El despliegue necesita `/health` y `/metrics` en el único puerto que expone Render. Solo cambió `adapters/server`: es la prueba de que la frontera del ADR-001 funcionaba |
| Quién corre el servidor | El jugador anfitrión, en su PC | Render, en un contenedor ([ADR-004](adr/ADR-004-estrategia-de-despliegue.md)) | El anfitrión era el punto único de falla visible: si cerraba la aplicación, la partida terminaba. Ahora el servidor no depende de ningún jugador |
| Identidad del jugador | El `playerId` que venía en cada mensaje | El `playerId` atado a la conexión y un código de sala | En LAN entre conocidos daba igual. Con una URL pública, cualquiera podía mover a otro jugador o entrar a la sala (amenazas A-01 y A-02) |
| Dónde se guardan los personajes | `src/main/resources/data/characters.json`, relativo al directorio de trabajo | `CharacterDAOProvider`: `RPG_DATA_FILE`, o la carpeta del repo en desarrollo, o `~/.rpg-character-creator/` en el cliente descargado | El cliente de Releases no se ejecuta desde el repositorio |
| Cómo se verifica todo | A mano | En cada push y PR, y como condición para fusionar | Objetivo del corte |

### Decisiones que se mantuvieron

- **Servidor autoritativo con difusión del estado completo.** La medición del Corte 2 sigue vigente: 32 jugadores es el último punto sano. El cupo por defecto (`MAX_PLAYERS=32`) sale de ahí.
- **Una réplica con el estado en memoria.** En Kubernetes se declara `replicas: 1` con estrategia `Recreate` y la razón.
- **Catálogo JSON y DAO JSON.**

### Cómo se resolvieron los pendientes del Corte 2

Pendientes declarados en `docs/arquitectura.md`, `docs/pruebas.md` y el ADR-001 del Corte 2:

| # | Pendiente del Corte 2 | Cómo se resolvió | Evidencia |
|---|---|---|---|
| P1 | El servidor aceptaba como destino cualquier casilla caminable, no solo una vecina (teletransporte) | `MovementRules.isValidStep` exige distancia ≤ 1 en cada eje; `GameSessionService.move` lo usa | Prueba `elServidorRechazaSaltosNoAdyacentes` (antes `@Disabled`) y `MovementRulesTest` |
| P2 | El servidor creaba al jugador si recibía un `move` sin `join` | `move` devuelve `false` si el jugador no está unido; el adaptador responde `not_joined` | `moverSinHaberseUnidoNoCreaAlJugador` (antes `@Disabled`) y `MultijugadorIT` *A-04* |
| P3 | El catálogo JSON no tenía prueba automática ("el hueco más grande de la suite") | `CatalogoJsonTest` carga los cuatro JSON reales y valida ids, datos y restricciones | Etapa *Pruebas unitarias* |
| P4 | Las restricciones de raza y clase y el `CharacterBuilder` no tenían pruebas | `CharacterBuilderTest` | Etapa *Pruebas unitarias* |
| P5 | La persistencia (`CharacterDAOJson`) no tenía prueba | `CharacterDAOJsonTest`: ida y vuelta sobre un archivo temporal | Etapa *Pruebas unitarias* |
| P6 | `GameClient` y `ClientMain` declaraban `adapters.client` pero estaban en `adapters/server/client/` | Se movieron a `adapters/client/` | ArchUnit las ubica bien; la regla *servidor_no_depende_de_la_interfaz_grafica* lo usa |
| P7 | El servidor se arrancaba aparte, a mano, en el PC del anfitrión | Contenedor en Render desplegado por el pipeline; `docker compose up` en local | `cd.yml`, URL pública |
| P8 | Si el anfitrión cerraba la aplicación, la partida terminaba para todos | El servidor ya no corre en el PC de un jugador. Sigue siendo una sola instancia (límite conocido abajo) | Vista de despliegue |
| P9 | La interfaz instanciaba `CharacterDAOJson`: la interfaz `CharacterDAO` no protegía nada | `CharacterDAOProvider` como único punto de construcción | Regla `ui_no_depende_de_la_persistencia_concreta` |
| P10 | `GameApp` cargaba el fondo del mapa desde `C:\Users\...\Downloads` | Recurso del classpath | Hallazgo H-04 de `docs/seguridad.md` |
| P11 | `CatalogManager` cachea el catálogo al arrancar | Riesgo aceptado: el catálogo solo cambia entre versiones y viaja dentro del cliente | — |

## Límites conocidos del sistema final

- **Una sola instancia del servidor.** El estado está en memoria. Un despliegue o un reinicio termina la partida en curso, y no se puede escalar horizontalmente. Para cambiarlo hay que llevar el estado a un almacén compartido (Redis) y repartir jugadores por sala.
- **Techo de unos 32 jugadores por partida**, por la difusión del estado completo en cada movimiento: el tráfico crece con el cuadrado de los jugadores. Para subirlo hay que enviar solo lo que cambió (deltas) o limitar la difusión al área visible.
- **Arranque en frío.** El plan gratuito de Render suspende el servicio tras 15 minutos de inactividad.
- **Autenticación por código de sala compartido.** No identifica a cada persona (riesgo aceptado H-07).
- **El SLO se mide en el pipeline desde los runners de GitHub**, no desde la red de los jugadores. Un jugador lejos de la región de Render verá más latencia.
- **La interfaz gráfica no tiene pruebas automáticas.** La usabilidad del wizard se declara sin verificación automática en la tabla de trazabilidad.

## Qué haríamos distinto

- Habríamos escrito el `Dockerfile` y un pipeline mínimo en el Corte 1. Varios pendientes (la ruta fija a Descargas, el paquete mal ubicado, la UI acoplada al DAO) los habría detectado una construcción limpia o una regla de arquitectura mucho antes.
- Habríamos trabajado por PR desde el principio. En los Cortes 1 y 2 casi todo entró por push directo a `main`.
- Habríamos diseñado el protocolo con la identidad atada a la conexión desde el ADR-001: la suplantación de `playerId` no dependía de la red, sino del diseño del mensaje.
