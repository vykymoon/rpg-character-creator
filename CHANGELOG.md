# Changelog

Todos los cambios relevantes del proyecto. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [versionado semántico](https://semver.org/lang/es/). Cada versión es un tag `vX.Y.Z` en `main`. `release.yml` publica estas notas en GitHub Releases junto con el cliente de escritorio.

## [Unreleased]

## [1.0.1] - 2026-11-18

Corrige el modo multijugador del cliente descargado de GitHub Releases.

### Corregido
- El multijugador no encontraba los personajes guardados cuando se ejecutaba el `.jar`: el creador guarda en `~/.rpg-character-creator/characters.json` y la partida los buscaba en la carpeta de trabajo, así que pedía elegir el archivo a mano. Ahora lee el mismo archivo que usa el creador (`CharacterDAOProvider.storageFile()`).
- El botón **Multijugador LAN** del menú cerraba todo el programa si se cancelaba una ventana o no había conexión, y el aviso de error fallaba con `Not on FX application thread`. Ahora vuelve al menú, y al cerrar la partida el menú reaparece.
- La partida independiente (`--jugar`) propone por defecto el servidor en línea (`wss://rpg-servidor-qz1m.onrender.com/ws`) en vez de `ws://localhost:8887`, y por Internet espera hasta 90 s a que el servidor despierte (antes 3 s, menos de lo que tarda Render gratuito en arrancar).

### Documentación
- Evidencia de la reversión probada en producción (`docs/devops.md` §5).

## [1.0.0] - 2026-11-15

Corte 3: entrega continua y segura. Primera versión desplegada automáticamente.

### Agregado
- Servidor de partida con HTTP y WebSocket en un solo puerto (Javalin): `/health`, `/metrics` y WebSocket en `/` y `/ws` ([ADR-006](docs/adr/ADR-006-adaptador-http-websocket.md)).
- Configuración por variables de entorno (`PORT`, `JOIN_CODE`, `MAX_PLAYERS`, `MAX_MESSAGES_PER_SECOND`, `MAX_MESSAGE_BYTES`, `APP_VERSION`) y `.env.example`.
- Código de sala para unirse a la partida y controles del modelo STRIDE: identidad atada a la conexión, límite de mensajes, tamaño máximo y cupo de jugadores ([docs/seguridad.md](docs/seguridad.md)).
- Dockerfile multi-etapa sin root, `docker-compose.yml` con healthcheck y perfil de observabilidad.
- Pipeline de GitHub Actions:
  - **CI**: construcción, pruebas unitarias con cobertura ≥ 80 %, ArchUnit, integración, SAST, SCA, secretos, imagen;
  - **CD**: GHCR, Render, prueba de humo, prueba de carga con el SLO y ZAP;
  - **Rollback**, **Release**, carga larga y Kubernetes.
- Reglas de arquitectura con ArchUnit sacadas de los ADR 001, 003 y 004.
- Pruebas del catálogo JSON, del `CharacterBuilder` y de ida y vuelta del DAO.
- Métricas de Prometheus y tablero de Grafana con p95, tasa de error y throughput ligados al SLO.
- Manifiestos de Kubernetes con probes, límites, ConfigMap, Secret, NetworkPolicy y ServiceAccount sin permisos.
- Cliente de escritorio empaquetado por sistema operativo en GitHub Releases (`Launcher`; `--jugar` abre la partida).
- Documentación:
  - [docs/devops.md](docs/devops.md), [docs/seguridad.md](docs/seguridad.md), [docs/evolucion.md](docs/evolucion.md), [docs/implementacion.md](docs/implementacion.md);
  - [vista de despliegue](docs/diagramas/despliegue.md);
  - ADR 004, 005 y 006.

### Cambiado
- `GameSessionService.move()` exige una casilla vecina y un jugador que haya hecho `join` (pendientes del Corte 2). Las dos pruebas `@Disabled` ahora corren.
- La interfaz gráfica obtiene el DAO de `CharacterDAOProvider` en vez de instanciar `CharacterDAOJson` (ADR-003).
- `GameClient` y `ClientMain` se movieron a `adapters/client/`, donde su paquete dice que están.
- El fondo del mapa multijugador se carga del classpath, no de una ruta fija de un integrante.

### Seguridad
- El servidor ya no confía en el `playerId` de cada mensaje (suplantación entre jugadores) ni imprime los mensajes inválidos completos.
- Cabeceras de seguridad HTTP, y Jetty no anuncia su versión.

## [0.3.0] - 2026-10-07

Corte 2: arquitectura y pruebas.

### Agregado
- Multijugador LAN con servidor autoritativo por WebSocket ([ADR-001](docs/adr/ADR-001-multijugador-lan.md)).
- Pruebas unitarias del dominio, prueba de integración con servidor y clientes reales, y pruebas de carga con k6.
- Mapa navegable, movimiento, inventario y combate.
- Mejoras de interfaz: tema retro ampliado y fondo del mapa multijugador.

## [0.2.0] - 2026-09

### Agregado
- Catálogo data-driven en JSON: razas, clases, habilidades y vestuario ([ADR-002](docs/adr/ADR-002-catalogo-en-json.md)).
- Sprites de vestuario y exclusividad de habilidades y prendas por raza y por clase.
- Ventana de detalle del personaje, estética retro y sonido.

## [0.1.0] - 2026-08

Corte 1: diseño.

### Agregado
- Wizard de creación de personajes en cinco pasos, galería, clonado de plantillas NPC (Prototype) y validación de nombres duplicados.
- Persistencia en JSON detrás de `CharacterDAO` ([ADR-003](docs/adr/ADR-003-persistencia-en-archivo.md)).
- Patrones Builder, Factory, Singleton, Prototype, DAO y Command.
