# Seguridad: modelo de amenazas, controles y hallazgos

Este documento cubre la sección 4 del Corte 3 (DevSecOps). Las decisiones sobre herramientas y umbrales están en [ADR-005](adr/ADR-005-seguridad-del-pipeline.md).

## 1. Qué se protege

El sistema desplegado es el **servidor de partida** (contenedor en Render, ver la [vista de despliegue](diagramas/despliegue.md)). Recibe por WebSocket los mensajes `join` y `move` de los clientes JavaFX, y difunde `map`, `state` y `error`. Expone por HTTP `/health` y `/metrics`. Mantiene el estado de la partida en memoria.

Los personajes del creador viven en el `characters.json` de cada jugador y no viajan al servidor, salvo el nombre, la raza, la clase y los objetos que se muestran en la partida.

Los flujos del [diagrama de contenedores](diagramas/c4.md) sobre los que se hace el análisis:

| Flujo | Origen → destino | Protocolo |
|---|---|---|
| F1 | Cliente JavaFX → servidor `/ws` | WebSocket sobre TLS (`wss://`) en la nube; `ws://` en LAN |
| F2 | Servidor → todos los clientes | WebSocket: difusión de `state` |
| F3 | Render, el pipeline o Prometheus → `/health` y `/metrics` | HTTPS |
| F4 | GitHub Actions → GHCR → Render | Publicación de la imagen y *deploy hook* |
| F5 | Desarrollador → GitHub (PR → main) | Git y pipeline |

## 2. Modelo de amenazas (STRIDE)

El sistema no tiene cuentas de usuario, pero sí **jugadores con identidad** (`playerId`) y una **sala** a la que no debería entrar cualquiera. La autenticación es el **código de sala** (`JOIN_CODE`). La autorización es "cada conexión solo controla al jugador que registró en su `join`".

| ID | Amenaza | STRIDE | Componente o flujo | Mitigación | Cómo se verifica |
|---|---|---|---|---|---|
| A-01 | Un jugador envía un `move` con el `playerId` de otro y lo mueve por el mapa. En el Corte 2 el servidor tomaba el `playerId` **del mensaje**, no de la conexión. | Suplantación | F1, `GameServer.onMessage` | El `playerId` queda atado a la conexión en el `join`. Un `move` con otro `playerId` se rechaza con `player_id_mismatch` y se registra en el log. | `MultijugadorIT` *A-01: un jugador no puede mover a otro* (etapa **Pruebas de integración**) |
| A-02 | Cualquiera que conozca la URL pública entra a la partida, porque el servidor de la nube no tiene un anfitrión que decida quién juega. | Suplantación (autenticación) | F1, `join` | Código de sala `JOIN_CODE` exigido en el `join`, comparado en tiempo constante (`MessageDigest.isEqual`). Si falla: `invalid_code` y cierre con 4401. El valor vive en GitHub Secrets y en Render, nunca en el repositorio. | `MultijugadorIT` *A-02*; la prueba de humo hace `join` con el código real (etapa **Prueba de humo**) |
| A-03 | Una segunda conexión hace `join` con el `playerId` de un jugador conectado y se apropia de su personaje. | Elevación de privilegios (autorización) | F1, `GameServer.handleJoin` | Un `playerId` pertenece a una sola conexión viva. Un segundo `join` con el mismo id se rechaza con `player_id_in_use`. Una conexión no puede cambiar de jugador (`already_joined`). | `MultijugadorIT` *A-03* |
| A-04 | Un cliente modificado se teletransporta a cualquier casilla, o mueve un jugador que nunca hizo `join` (pendientes del Corte 2). | Manipulación | `GameSessionService.move`, `MovementRules` | `isValidStep`: el destino debe ser caminable y vecino. `move` rechaza a quien no está unido. | `GameSessionServiceTest` (antes `@Disabled`, ahora activas), `MovementRulesTest`, `MultijugadorIT` *A-04* (etapas **Pruebas unitarias** e **integración**) |
| A-05 | Un cliente inunda el servidor con mensajes o envía mensajes enormes, y la difusión del estado degrada a todos los jugadores. El Corte 2 midió colapso desde 48 jugadores. | Denegación de servicio | F1 y F2 | Límite de mensajes por segundo por conexión (`MAX_MESSAGES_PER_SECOND`, cierre con 1008 si persiste), tamaño máximo de mensaje (`MAX_MESSAGE_BYTES`), cupo de jugadores (`MAX_PLAYERS=32`, último punto sano medido), `maxRequestSize` HTTP y límites de CPU y memoria en Kubernetes. | `MultijugadorIT` *A-05*; prueba de carga con el SLO (etapa **Prueba de carga**); métrica `rpg_messages_dropped_total` en Grafana |
| A-06 | Los errores revelan detalles internos: el servidor del Corte 2 imprimía el mensaje recibido completo (inyección en los logs) y Jetty anuncia su versión en la cabecera `Server`. | Divulgación de información | F1, F3 | Errores con un código corto (`{"type":"error","code":"..."}`), sin trazas ni el mensaje original. `setSendServerVersion(false)`. Cabeceras de seguridad en toda respuesta HTTP (CSP, `nosniff`, `X-Frame-Options`, HSTS, `no-store`). TLS de Render en `wss://`. | `MultijugadorIT` *El healthcheck responde UP* (revisa las cabeceras); ZAP baseline (etapa **DAST**); CodeQL (`java/log-injection`) |
| A-07 | Un jugador niega haber hecho trampa o se desconoce quién saturó el servidor. | Repudio | Servidor | Log estructurado por evento (`event=join\|move_rejected\|join_rejected\|rate_limit_close`) con jugador, IP (respetando `X-Forwarded-For` de Render) y motivo. Las métricas guardan el conteo por resultado. | Revisión de logs en Render (`docs/implementacion.md` §7) y paneles de rechazos en Grafana |
| A-08 | Si alguien ejecuta código en el contenedor, usa el token de la cuenta de servicio, escala a root o abre conexiones de salida para exfiltrar o descargar herramientas. | Elevación de privilegios | Contenedor, Kubernetes | Usuario sin privilegios (UID 10001), `readOnlyRootFilesystem`, `capabilities: drop ALL`, `no-new-privileges`, `automountServiceAccountToken: false` y ServiceAccount sin roles, NetworkPolicy que deniega toda salida, namespace con Pod Security `restricted`. | Job *Imagen Docker y escaneo* (comprueba UID ≠ 0); `k8s.yml` (comprueba que la salida está bloqueada) |
| A-09 | Una dependencia con un CVE conocido, una llave en el código o una imagen sin parches llega a producción. | Manipulación (cadena de suministro) | F4, F5 | Los cuatro controles de la sección 3 como *quality gates*. `main` protegida: PR obligatorio, aprobación y checks en verde. Imagen inmutable por SHA, con SBOM y procedencia. | Pipeline (sección 3) y PR bloqueado (sección 5) |
| A-10 | Alguien con acceso al repositorio despliega una imagen arbitraria llamando al *deploy hook*. | Elevación de privilegios | F4 | El hook es un secreto del Environment `production`: solo lo leen los workflows que corren en `main` o en un tag. Nunca está en el repositorio ni en los logs (GitHub lo enmascara). Si se filtra, se regenera en Render. | Gitleaks (cero secretos); revisión del ruleset |

## 3. Controles en el pipeline

Todos corren como *quality gates*: si superan el umbral, el job falla, el check del PR queda en rojo y el ruleset impide fusionar.

| Control | Herramienta | Job (check del PR) | Umbral | Reporte (artefacto) |
|---|---|---|---|---|
| SAST | CodeQL `security-extended` + `sarif_gate.py` | Seguridad - SAST (CodeQL) | Ningún hallazgo con severidad ≥ 7.0 sin tratar | `reporte-sast` y la pestaña Security |
| SCA | Trivy fs + dependency-review-action | Seguridad - dependencias (SCA) / revisión de dependencias del PR | Ninguna vulnerabilidad alta o crítica con corrección | `reporte-sca` |
| Secretos | Gitleaks, historial completo | Seguridad - detección de secretos | Cero | `reporte-secretos` |
| Imagen | Trivy image | Imagen Docker y escaneo | Ninguna vulnerabilidad **alta** o crítica con corrección (más estricto que el enunciado) | `reporte-imagen` |
| DAST (bonificación) | OWASP ZAP baseline | Seguridad - DAST (OWASP ZAP), en `cd.yml` | Ninguna alerta WARN o FAIL sin tratar | `reporte-dast` |

Las excepciones solo se agregan después de registrar el hallazgo en la sección 4:

- `.trivyignore` para SCA e imagen;
- `.github/security/sast-allowlist.txt` para SAST;
- `.zap/rules.tsv` para DAST.

## 4. Registro de hallazgos

Cada hallazgo lleva una de tres decisiones: **corregido** (con el commit), **falso positivo** (con la justificación técnica) o **riesgo aceptado** (con la justificación y lo que haría falta para corregirlo).

| # | Fecha | Control | Hallazgo | Severidad | Decisión | Justificación o evidencia |
|---|---|---|---|---|---|---|
| H-01 | 2026-11-15 | Revisión manual (STRIDE) | `GameServer` tomaba el `playerId` del mensaje: suplantación entre jugadores (A-01) | Alta | Corregido | Identidad atada a la conexión; prueba `A-01` en `MultijugadorIT` |
| H-02 | 2026-11-15 | Revisión manual (STRIDE) | `move()` aceptaba saltos no adyacentes y jugadores sin `join` (A-04) | Media | Corregido | `MovementRules.isValidStep`; pruebas antes `@Disabled`, ahora activas |
| H-03 | 2026-11-15 | Revisión manual (CodeQL `java/log-injection`) | El servidor del Corte 2 imprimía el mensaje recibido completo cuando era inválido | Media | Corregido | El adaptador Javalin registra solo el código de error (ADR-006) |
| H-04 | 2026-11-15 | Revisión manual | `GameApp` cargaba el fondo del mapa desde una ruta fija (`C:\Users\...\Downloads`) | Baja | Corregido | Se lee del classpath |
| H-05 | 2026-11-15 | ZAP (esperado) | Cabeceras de seguridad ausentes y versión de Jetty en `Server` | Baja | Corregido | `securityHeaders` y `setSendServerVersion(false)`; la prueba de integración verifica las cabeceras |
| H-06 | 2026-11-15 | Riesgo de diseño | `/metrics` es público en la URL de Render | Baja | Riesgo aceptado | Solo expone conteos agregados, sin nombres ni IP. Para corregirlo: exigir un token *bearer* (`METRICS_TOKEN`) o publicar `/metrics` solo en la red interna (en Kubernetes ya es solo interno) |
| H-07 | 2026-11-15 | Riesgo de diseño | El código de sala es compartido por todos los jugadores: no identifica a cada persona | Media | Riesgo aceptado | Suficiente para una partida entre conocidos. Para corregirlo: cuentas con OAuth (GitHub o Google) y un token firmado por jugador |
| H-08 | 2026-11-15 | Riesgo de diseño | `ws://` sin TLS en el modo LAN | Baja | Riesgo aceptado | Solo dentro de la red local del anfitrión; en la nube el cliente usa `wss://` |
| H-09 | 2026-11-16 | Configuración del repositorio | `dependency-review-action` fallaba con "Dependency review is not supported": el *Dependency graph* estaba desactivado, así que el control no estaba revisando nada | Media | Corregido | Se activó en *Settings → Advanced Security → Dependency graph*. El check volvió a correr en el PR #2 |
| H-10 | 2026-11-16 | DAST (ZAP 10015) | *Re-examine Cache-control Directives*: `Cache-Control: no-store` no incluía `no-cache, must-revalidate` | Baja | Corregido | `securityHeaders` envía `no-store, no-cache, must-revalidate` y `Pragma: no-cache` |
| H-11 | 2026-11-16 | DAST (ZAP 90005) | *Sec-Fetch-Dest Header is Missing* en 12 peticiones | Informativa | Falso positivo | La regla revisa las cabeceras `Sec-Fetch-*` de la **petición**, que agrega un navegador y no el escáner. No depende del servidor. Se marca `IGNORE` en `.zap/rules.tsv` |

Los hallazgos que reporten las herramientas en las ejecuciones del pipeline se agregan aquí con su enlace a la ejecución de Actions.

## 5. Evidencia de que los controles bloquean

Se abrió un pull request que introduce a propósito tres problemas:

1. la dependencia `org.apache.logging.log4j:log4j-core:2.14.1` (Log4Shell, CVE-2021-44228, crítica, con corrección);
2. una llave de AWS falsa en `src/main/resources/config-ejemplo.properties`;
3. una clase de `domain` que importa `io.javalin`, para romper la regla de arquitectura.

El pipeline lo bloquea en los checks **Seguridad - dependencias (SCA)**, **Seguridad - detección de secretos**, **Reglas de arquitectura** e **Imagen Docker y escaneo**. El PR se cerró **sin fusionar** y la rama se borró, de modo que la llave falsa nunca llegó al historial de `main`.

- **PR bloqueado:** [#3 — [DEMO - NO FUSIONAR] PR que los controles de seguridad deben bloquear](https://github.com/vykymoon/rpg-character-creator/pull/3). Está cerrado sin fusionar y su rama fue borrada.
- **Ejecución que lo bloqueó:** [Actions run 43743868255](https://github.com/vykymoon/rpg-character-creator/actions/runs/43743868255).

| Check | Resultado en el PR #3 | Qué detectó |
|---|---|---|
| Seguridad - dependencias (SCA) | ❌ | Trivy: `log4j-core 2.14.1`, CVE-2021-44228 (crítica, corregida en 2.15.0+) |
| Seguridad - revisión de dependencias del PR | ❌ (ver nota) | En el PR #3 falló porque el *Dependency graph* del repositorio estaba desactivado, no por el CVE. Se activó el 16-11-2026 (hallazgo H-09) y desde entonces el check revisa cada PR |
| Seguridad - detección de secretos | ❌ | Gitleaks, regla `aws-access-token` en `config-ejemplo.properties` |
| Reglas de arquitectura | ❌ | ArchUnit `dominio_no_depende_de_infraestructura`: `domain.AtajoDeRed` depende de `io.javalin.Javalin` |
| Imagen Docker y escaneo | ❌ | Trivy image: el `log4j-core` vulnerable también quedó dentro de la imagen |
| Construcción, unitarias, integración y SAST | ✅ | El código compila y las pruebas pasan: lo que bloquea son las reglas, no un error de compilación |

## 6. Qué queda abierto

Los riesgos aceptados H-06, H-07 y H-08 siguen abiertos. Las recomendaciones para producción están en [docs/implementacion.md §9](implementacion.md#9-recomendaciones-para-producción).
