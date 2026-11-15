# RPG Character Creator

[![CI](https://github.com/vykymoon/rpg-character-creator/actions/workflows/ci.yml/badge.svg)](https://github.com/vykymoon/rpg-character-creator/actions/workflows/ci.yml)
[![CD](https://github.com/vykymoon/rpg-character-creator/actions/workflows/cd.yml/badge.svg?branch=main)](https://github.com/vykymoon/rpg-character-creator/actions/workflows/cd.yml)
[![Kubernetes](https://github.com/vykymoon/rpg-character-creator/actions/workflows/k8s.yml/badge.svg?branch=main)](https://github.com/vykymoon/rpg-character-creator/actions/workflows/k8s.yml)
[![Release](https://img.shields.io/github/v/release/vykymoon/rpg-character-creator?label=versi%C3%B3n)](https://github.com/vykymoon/rpg-character-creator/releases)

Proyecto de Diseño y Arquitectura de Software. Integrantes: Victor Luna, Nicolas Salazar y Tiffany Cardona.

| | |
|---|---|
| **Servidor desplegado** | **https://rpg-servidor.onrender.com** · estado en [`/health`](https://rpg-servidor.onrender.com/health) · WebSocket `wss://rpg-servidor.onrender.com/ws` |
| **Cliente de escritorio** | [Última versión en Releases](https://github.com/vykymoon/rpg-character-creator/releases/latest) (Windows, Linux, macOS) |
| **Imagen del servidor** | [`ghcr.io/vykymoon/rpg-servidor`](https://github.com/vykymoon/rpg-character-creator/pkgs/container/rpg-servidor) |

> El servidor usa el plan gratuito de Render: si nadie lo usó en los últimos 15 minutos, la primera petición tarda unos segundos en despertarlo.

## Presentación

Crear un personaje de rol es una barrera de entrada real. Al jugador nuevo lo paraliza una lista larga de razas, clases, habilidades y equipo sin saber qué combinación tiene sentido. Al experimentado le pasa lo contrario: sabe lo que quiere, pero comete errores de reglas al armarlo a mano.

Esta es una aplicación de escritorio en Java y JavaFX que resuelve eso en dos partes:

- **El creador de personajes.** Un wizard de cinco pasos con el catálogo de razas, clases, habilidades y vestuario definido en JSON, validación de combinaciones y una galería donde se guardan, clonan y borran personajes.
- **El modo de juego.** Lleva ese personaje a un mapa compartido por varios jugadores en tiempo real. La partida la controla un servidor autoritativo que corre en la nube, se despliega solo con cada cambio y se verifica en cada pull request.

## Cómo ejecutar

### El sistema completo con Docker (un solo comando)

Requiere solo Docker con Compose v2.

```bash
cp .env.example .env        # Windows PowerShell: Copy-Item .env.example .env
docker compose up --build
```

El servidor queda en `http://localhost:8887`. `docker compose ps` lo muestra como `healthy`, y [`/health`](http://localhost:8887/health) responde `{"status":"UP",...}`.

Para incluir Prometheus y Grafana con el tablero del SLO (http://localhost:3000):

```bash
docker compose --profile observabilidad up --build
```

### El cliente de escritorio

Con Java 17 o superior, descargar el `.jar` de [Releases](https://github.com/vykymoon/rpg-character-creator/releases/latest):

```bash
java -jar rpg-cliente-<version>-<so>.jar           # creador de personajes
java -jar rpg-cliente-<version>-<so>.jar --jugar   # partida multijugador
```

En la partida, la URL del servidor es `wss://rpg-servidor.onrender.com/ws` (nube) o `ws://<ip-del-anfitrion>:8887` (LAN, con `docker compose up`). Se escribe también el código de sala.

### Desde el código (desarrollo)

Requiere Java 17+ y Maven 3.9+.

| Qué | Comando |
|---|---|
| Creador de personajes | `mvn clean javafx:run` |
| Servidor de la partida | `mvn compile exec:java@server` |
| Cliente gráfico de la partida | `mvn compile javafx:run@game` |
| Pruebas unitarias, cobertura y arquitectura | `mvn test` |
| Todo, incluidas las de integración | `mvn verify` |
| Cliente empaquetado como `.jar` | `mvn -Pcliente package -DskipTests` |

La guía completa para instalar y operar el sistema en otra organización está en **[docs/implementacion.md](docs/implementacion.md)**.

## Cómo se ejecuta el pipeline

Todo cambio entra por **pull request** a `main`, que está protegida con un ruleset: aprobación de otro integrante y todos los checks en verde. Detalle de etapas, umbrales y reversión en **[docs/devops.md](docs/devops.md)**.

| Workflow | Se ejecuta | Etapas |
|---|---|---|
| [`ci.yml`](.github/workflows/ci.yml) | En cada push a una rama y en cada PR | Construcción → pruebas unitarias con cobertura del dominio ≥ 80 % · reglas de arquitectura · pruebas de integración · SAST (CodeQL) · SCA (Trivy) · revisión de dependencias · secretos (Gitleaks) · imagen Docker + Trivy |
| [`cd.yml`](.github/workflows/cd.yml) | En cada merge a `main` y cada tag `vX.Y.Z` | CI completa → imagen en GHCR (`sha-<sha>` y versión) → despliegue en Render → prueba de humo → prueba de carga con el SLO → DAST (ZAP) |
| [`release.yml`](.github/workflows/release.yml) | En cada tag `vX.Y.Z` | Cliente JavaFX para Windows, Linux y macOS → GitHub Release con las notas de [CHANGELOG.md](CHANGELOG.md) |
| [`rollback.yml`](.github/workflows/rollback.yml) | Manual | Vuelve a desplegar la imagen de una versión anterior y corre la prueba de humo |
| [`perf.yml`](.github/workflows/perf.yml) | Manual y los lunes | Prueba de carga larga contra producción |
| [`k8s.yml`](.github/workflows/k8s.yml) | PR y `main` que tocan `k8s/`, el Dockerfile o el código | Despliegue en Kubernetes (kind) con probes, humo y NetworkPolicy |

Para lanzar a mano el despliegue o la reversión: **Actions → CD** (o **Rollback**) **→ Run workflow**.

## Estructura

```
.github/workflows/  ci, cd, release, rollback, perf y k8s
docs/               arquitectura, ADR, pruebas, devops, seguridad, evolucion, implementacion
  adr/              decisiones de los tres cortes (ADR-001 a ADR-006)
  diagramas/        C4 (contexto, contenedores, componentes y despliegue) y UML
src/main/java/      aplicacion: domain, application y adapters (servidor); ui, model y patrones (creador)
src/test/java/      unitarias (*Test), integracion (*IT) y reglas de arquitectura (ArchitectureTest)
perf/               k6: prueba de humo, carga y resultados del Corte 2
k8s/                manifiestos de Kubernetes (bonificacion)
observabilidad/     Prometheus y tablero de Grafana (bonificacion)
scripts/            despliegue y espera del healthcheck, usados por los workflows
Dockerfile · docker-compose.yml · .env.example · CHANGELOG.md
```

## Trazabilidad de retos

La tabla del Corte 2 ampliada con la etapa del pipeline que vuelve a verificar cada reto en cada cambio y el umbral que lo detiene.

| Reto (Corte 1) | Atributo de calidad | Decisión arquitectónica | Prueba que lo evidencia | Etapa del pipeline | Umbral que detiene el pipeline |
|---|---|---|---|---|---|
| Mapa navegable con movimiento del jugador | Integridad del estado del juego | Las reglas de movimiento viven en el dominio y el servidor las aplica antes de aceptar cada movimiento; solo pasos a casillas vecinas ([ADR-001](docs/adr/ADR-001-multijugador-lan.md)) | `MovementRulesTest`, `GameMapTest`, `GameSessionServiceTest` | ci → **Pruebas unitarias y cobertura** | Falla una prueba, o la cobertura de líneas de `domain` + `application` < 80 % |
| Conexión de jugadores (LAN y nube) | Consistencia del estado entre jugadores | Cliente-servidor con servidor autoritativo ([ADR-001](docs/adr/ADR-001-multijugador-lan.md)) | `MultijugadorIT`: servidor Javalin real y dos clientes; uno se mueve y el otro recibe el estado | ci → **Pruebas de integración** | Falla una prueba |
| Conexión de jugadores (LAN y nube) | Rendimiento | WebSocket con difusión del estado y cupo de 32 jugadores (último punto sano medido) | `perf/scripts/lan_ws_k6.js` (k6, 8 jugadores, 1 min) contra el entorno desplegado | cd → despliegue → **Prueba de carga (SLO)** | p95 de `move_latency` ≥ 150 ms, o ≥ 1 % de movimientos sin confirmar |
| Conexión de jugadores (nube) | Disponibilidad | Servidor en un contenedor en Render, desplegado por el pipeline, con healthcheck ([ADR-004](docs/adr/ADR-004-estrategia-de-despliegue.md)) | `scripts/wait-for-health.sh` y `perf/scripts/smoke_k6.js` (join, map, move, state) | cd → **Desplegar** → **Prueba de humo** | `/health` no responde el SHA nuevo en 10 min, o falla un *check* del flujo principal |
| Cambiar el transporte de red sin tocar las reglas | Modificabilidad | Puertos y adaptadores: el dominio y el caso de uso no conocen el transporte (cambio a Javalin, [ADR-006](docs/adr/ADR-006-adaptador-http-websocket.md)) | `ArchitectureTest`: `dominio_no_depende_de_infraestructura`, `caso_de_uso_no_conoce_los_adaptadores` | ci → **Reglas de arquitectura** | Una clase de `domain` depende de algo fuera de `domain` y `java.*`, o `application` importa `adapters`, Javalin, Jetty, Java-WebSocket o JavaFX |
| Desplegar el servidor sin la interfaz gráfica | Portabilidad / desplegabilidad | La imagen del servidor no incluye JavaFX ([ADR-004](docs/adr/ADR-004-estrategia-de-despliegue.md)) | `ArchitectureTest`: `servidor_no_depende_de_la_interfaz_grafica`; job de imagen que la arranca | ci → **Reglas de arquitectura** · **Imagen Docker y escaneo** | `adapters.server` importa `javafx`, `ui`, `game` o el cliente; el contenedor no responde `/health` |
| Catálogo ampliable sin recompilar | Extensibilidad | Catálogo en archivos JSON leídos por las factories ([ADR-002](docs/adr/ADR-002-catalogo-en-json.md)) | `CatalogoJsonTest`: carga los 4 JSON reales, ids únicos, datos y restricciones; `CharacterBuilderTest` | ci → **Pruebas unitarias y cobertura** | Falla una prueba (JSON inválido, id repetido, restricción rota) |
| Cambiar el almacenamiento sin tocar la interfaz | Mantenibilidad | Persistencia detrás de `CharacterDAO`, construida solo en `CharacterDAOProvider` ([ADR-003](docs/adr/ADR-003-persistencia-en-archivo.md)) | `ArchitectureTest`: `ui_no_depende_de_la_persistencia_concreta`; `CharacterDAOJsonTest` (ida y vuelta) | ci → **Reglas de arquitectura** · **Pruebas unitarias** | La UI depende de `CharacterDAOJson`, o falla la ida y vuelta |
| Que nadie suplante a otro jugador ni entre sin permiso | Seguridad | Identidad atada a la conexión, código de sala y límites anti-flood ([docs/seguridad.md](docs/seguridad.md)) | `MultijugadorIT` A-01 a A-05 | ci → **Pruebas de integración** | Falla una prueba |
| Que no lleguen vulnerabilidades conocidas a producción | Seguridad | Cuatro *quality gates* más DAST ([ADR-005](docs/adr/ADR-005-seguridad-del-pipeline.md)) | CodeQL, Trivy fs, Gitleaks, Trivy image, ZAP | ci → **Seguridad - \*** · **Imagen** · cd → **DAST** | SAST ≥ 7.0; CVE alto o crítico con corrección (dependencias e imagen); 1 secreto; alerta ZAP WARN no tratada |
| Que el jugador nuevo arme un personaje válido sin conocer las reglas | Usabilidad | Wizard de cinco pasos que filtra las opciones por raza y clase | **Sin verificación automática:** requiere una evaluación con usuarios. La interfaz JavaFX no tiene pruebas automáticas (TestFX no se justifica en el alcance) | — | — |

## Documentación

| Documento | Qué contiene |
|---|---|
| [docs/implementacion.md](docs/implementacion.md) | **Guía de implementación y operación** para el equipo técnico del negocio |
| [docs/devops.md](docs/devops.md) | Diagrama del pipeline, etapas, umbrales y por qué, secretos y reversión |
| [docs/seguridad.md](docs/seguridad.md) | Modelo de amenazas STRIDE, controles, umbrales, registro de hallazgos y PR bloqueado |
| [docs/evolucion.md](docs/evolucion.md) | Los tres cortes, decisiones que cambiaron, pendientes del Corte 2 resueltos y límites |
| [docs/arquitectura.md](docs/arquitectura.md) | Estilo elegido, alternativas descartadas, capas y límites |
| [docs/diagramas/despliegue.md](docs/diagramas/despliegue.md) | Vista de despliegue C4: Render, GHCR, GitHub Actions y clientes |
| [docs/adr/](docs/adr/) | ADR-001 a ADR-006 (multijugador, catálogo, persistencia, **despliegue**, **seguridad del pipeline**, **adaptador HTTP+WS**) |
| [docs/pruebas.md](docs/pruebas.md) | Estrategia por niveles, reglas de arquitectura y resultados de carga |
| [docs/patrones-y-solid.md](docs/patrones-y-solid.md) | Patrones aplicados y principios SOLID |
| [observabilidad/](observabilidad/README.md) · [k8s/](k8s/README.md) | Bonificaciones: Prometheus + Grafana y Kubernetes |
| [CHANGELOG.md](CHANGELOG.md) | Cambios por versión |

## Roles

| Integrante | Contribución principal |
|---|---|
| Victor Luna | SOLID y patrones de diseño; pipeline de CI/CD y despliegue |
| Nicolas Salazar | Reglas de juego, sprites y restricciones; DevSecOps y modelo de amenazas |
| Tiffany Cardona | Control de errores, estética y manejo de excepciones; observabilidad y guía de implementación |
