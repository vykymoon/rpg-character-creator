# Guía de implementación y operación

Esta guía es para el **equipo técnico de la organización** que va a poner en marcha RPG Character Creator en su propia infraestructura y operarlo después. Supone que el equipo conoce Docker y Git, pero no el proyecto. Cada paso trae el comando exacto y el resultado esperado.

Convenciones:

- Los valores que cada organización cambia van entre `<>`: `<usuario-github>`, `<servicio>`, `<codigo-de-sala>`.
- Los comandos son para una terminal Bash (Linux, macOS, o Git Bash / WSL en Windows). Cuando cambian en PowerShell se indica aparte.
- Los nombres de variables, servicios y workflows son los mismos del repositorio: `.env.example`, `docker-compose.yml` y `.github/workflows/`.

---

## 1. Qué se va a implementar

RPG Character Creator es una aplicación de escritorio para crear personajes de rol con validación de reglas (razas, clases, habilidades y vestuario), más un modo de juego en el que varios jugadores comparten un mapa en tiempo real. La parte compartida la controla un **servidor autoritativo**: cada movimiento se valida en el servidor y el estado se difunde a todos.

Se instalan dos componentes:

| Componente | Qué es | Dónde se instala |
|---|---|---|
| **Servidor de partida** | Contenedor Docker `rpg-servidor` (Java 21, Javalin). Expone en un solo puerto el WebSocket del juego (`/ws`), `/health` y `/metrics` | En la nube (Render, como en el proyecto) o en un servidor propio con Docker |
| **Cliente de escritorio** | Un `.jar` por sistema operativo (Java 17 + JavaFX). Es el creador de personajes y la ventana de juego | En el computador de cada jugador, descargado de GitHub Releases |
| *(Opcional)* Observabilidad | Prometheus + Grafana con el tablero del SLO | Junto al servidor, con `docker compose --profile observabilidad` |

La vista de despliegue, con dónde corre cada contenedor y cómo se comunican, está en [docs/diagramas/despliegue.md](diagramas/despliegue.md).

El pipeline (`.github/workflows/cd.yml`) automatiza todo lo demás. Cada cambio en `main` construye la imagen, la escanea, la publica en GitHub Container Registry (GHCR), la despliega en Render y verifica que responda.

---

## 2. Requisitos previos

### Cuentas y servicios

| Cuenta | Para qué | Plan |
|---|---|---|
| GitHub (organización o usuario) | Repositorio, GitHub Actions, GHCR, Releases | Gratuito: Actions y GHCR no tienen costo en repositorios públicos |
| Render ([render.com](https://render.com)) | Ejecutar el servidor con TLS y una URL pública | Free para pruebas; Starter para producción (sección 3) |
| *(Alternativa a Render)* Un servidor Linux con IP pública | Si se prefiere infraestructura propia | — |

### Software (en el equipo de quien instala)

| Software | Versión mínima | Para qué | Cómo comprobarlo |
|---|---|---|---|
| Git | 2.40 | Clonar y configurar el repositorio | `git --version` |
| Docker Engine o Docker Desktop | 24.0, con Compose v2 | Levantar el servidor localmente o en un servidor propio | `docker --version` y `docker compose version` |
| GitHub CLI *(opcional)* | 2.40 | Registrar secretos desde la terminal | `gh --version` |
| Java (solo jugadores) | 17 (Temurin recomendado) | Ejecutar el cliente de escritorio | `java -version` |
| Maven *(solo para desarrollar)* | 3.9 | Compilar sin Docker | `mvn -v` |

### Recursos mínimos del servidor

| Recurso | Mínimo | Recomendado (32 jugadores) |
|---|---|---|
| CPU | 0.1 vCPU (Render Free) | 0.5 vCPU |
| Memoria | 512 MB (la JVM usa hasta el 75 %) | 512 MB |
| Disco | 400 MB para la imagen | 1 GB |
| Red | Unos 40 MB/s de salida con 32 jugadores moviéndose cada 200 ms (medido en el Corte 2) | — |

### Puertos

| Puerto | Protocolo | Uso |
|---|---|---|
| `8887` (o el valor de `PORT`) | HTTP + WebSocket | Juego, `/health`, `/metrics`. En Render, la plataforma asigna `PORT` y publica 443 con TLS |
| `9090` | HTTP | Prometheus (solo perfil de observabilidad, uso interno) |
| `3000` | HTTP | Grafana (solo perfil de observabilidad) |

### Dominio y HTTPS

Render entrega `https://<servicio>.onrender.com` con un certificado automático. No hace falta comprar dominio. Para un dominio propio, ver la sección 9.

---

## 3. Costos estimados

Precios de lista del proveedor en noviembre de 2026, en USD. Hay que verificarlos en [render.com/pricing](https://render.com/pricing) antes de presupuestar.

| Componente | Plan usado en el proyecto | Costo mensual | Plan recomendado para el negocio | Costo mensual |
|---|---|---|---|---|
| Servidor de partida (Render Web Service) | Free: 512 MB, 0.1 CPU, se suspende tras 15 min sin tráfico | 0 | **Starter**: 512 MB, 0.5 CPU, siempre encendido | 7 |
| Espacio de trabajo de Render | Hobby | 0 | Hobby (Pro, 25 USD, solo si se necesitan varios administradores) | 0 |
| Registro de imágenes (GHCR) | Paquete público | 0 | Público (la imagen no contiene secretos) | 0 |
| CI/CD (GitHub Actions) | Repositorio público | 0 | Público. En uno privado, unos 25 minutos por despliegue sobre los 2.000 minutos gratuitos | 0 |
| Observabilidad (Prometheus + Grafana) | Local con Compose | 0 | En el mismo servidor propio, o Grafana Cloud Free | 0 |
| Dominio propio *(opcional)* | No se usó | 0 | `.com` | ~1 (unos 12 al año) |
| **Total** | | **0** | | **≈ 7–8** |

**Si la partida necesita más de 32 jugadores simultáneos**, el límite no es de hardware, sino del diseño (difusión del estado completo; ver `docs/evolucion.md`). Comprar un plan más grande, como Standard (2 GB, 1 CPU, 25 USD), no lo resuelve.

---

## 4. Configuración

Todas las variables del servidor, con sus valores de ejemplo, están en `.env.example`. Ningún valor sensible vive en el código ni en la imagen.

### Variables del servidor

| Variable | Para qué sirve | Valor de ejemplo | Obligatoria | Dónde se configura |
|---|---|---|---|---|
| `PORT` | Puerto HTTP + WebSocket | `8887` | No (por defecto 8887). En Render la inyecta la plataforma | `.env` (local); Render la define sola |
| `JOIN_CODE` | Código de sala que deben escribir los jugadores. Vacío = sala abierta | `3f9c1a7be2d40c55a1e8` | **Sí en producción** | `.env` (local); Render → *Environment*; GitHub → Environment `production` (secreto) |
| `MAX_PLAYERS` | Jugadores simultáneos. Con la sala llena se rechazan los nuevos (`server_full`) | `32` | No (32) | `.env`; Render → *Environment* |
| `MAX_MESSAGES_PER_SECOND` | Mensajes por segundo y conexión antes de descartar (anti-flood) | `20` | No (20) | `.env`; Render → *Environment* |
| `MAX_MESSAGE_BYTES` | Tamaño máximo de un mensaje WebSocket | `4096` | No (4096) | `.env`; Render → *Environment* |
| `APP_VERSION` | Versión que reporta `/health` | `local` | No. El pipeline la fija con el SHA del commit dentro de la imagen | `.env` (local) |
| `GRAFANA_ADMIN_USER` | Usuario de Grafana | `admin` | Solo con el perfil de observabilidad | `.env` |
| `GRAFANA_ADMIN_PASSWORD` | Contraseña de Grafana | `cambie-esta-clave` | Solo con el perfil de observabilidad (Compose no arranca Grafana sin ella) | `.env` |

### Secretos y variables del pipeline

Se configuran en GitHub → *Settings → Environments → `production`*.

| Nombre | Tipo | Para qué | Valor de ejemplo |
|---|---|---|---|
| `RENDER_DEPLOY_HOOK_URL` | Secret | URL secreta con la que el pipeline pide a Render desplegar una imagen | `https://api.render.com/deploy/srv-xxxxxxxx?key=yyyyyyyy` |
| `JOIN_CODE` | Secret | El mismo código de sala de Render; lo usan la prueba de humo y la de carga | `3f9c1a7be2d40c55a1e8` |
| `APP_URL` | Variable (no secreta) | URL pública del servidor, sin barra final | `https://<servicio>.onrender.com` |

### Cómo generar los valores sensibles

```bash
# Código de sala: 20 caracteres hexadecimales aleatorios
openssl rand -hex 10
```

En PowerShell, sin OpenSSL:

```powershell
-join ((1..20) | ForEach-Object { '{0:x}' -f (Get-Random -Max 16) })
```

El deploy hook no se genera: lo crea Render (paso 5.6). Si se filtra, se regenera en Render (sección 7.4).

---

## 5. Instalación paso a paso

Desde cero: una cuenta de GitHub y una de Render vacías hasta el sistema funcionando. Las rutas de la consola web son las de noviembre de 2026.

### 5.1 Copiar el repositorio a la organización

1. En GitHub, abrir `https://github.com/vykymoon/rpg-character-creator` y pulsar **Fork**, o crear un repositorio vacío e importar este. Como propietario elegir la organización: en el resto de la guía es `<usuario-github>`.
   - **Resultado esperado:** existe `https://github.com/<usuario-github>/rpg-character-creator`.
2. Clonarlo:

   ```bash
   git clone https://github.com/<usuario-github>/rpg-character-creator.git
   cd rpg-character-creator
   ```

   - **Resultado esperado:** la carpeta tiene `Dockerfile`, `docker-compose.yml`, `.env.example` y `.github/workflows/`.
3. En **Settings → Actions → General**, comprobar que *Allow all actions and reusable workflows* esté seleccionado, y en *Workflow permissions*, que esté *Read and write permissions*.
   - **Resultado esperado:** la pestaña Actions muestra los workflows `CI`, `CD`, `Release`, `Rollback`, `Carga larga` y `Kubernetes`.

### 5.2 Probar localmente con Docker (opcional, recomendado)

```bash
cp .env.example .env            # PowerShell: Copy-Item .env.example .env
# Editar .env y poner un JOIN_CODE propio
docker compose up --build -d
docker compose ps
```

**Resultado esperado:** `docker compose ps` muestra `rpg-servidor-1` con el estado `Up ... (healthy)` después de unos 20 segundos. Luego:

```bash
curl http://localhost:8887/health
```

**Resultado esperado:** `{"status":"UP","version":"local","players":0,"maxPlayers":32}`.

Para apagarlo: `docker compose down`.

### 5.3 Proteger la rama main

En **Settings → Rules → Rulesets → New ruleset → New branch ruleset**:

1. *Ruleset name*: `main`. *Enforcement status*: **Active**.
2. *Target branches → Add target → Include default branch*.
3. Marcar las reglas:
   - **Restrict deletions**;
   - **Require linear history**;
   - **Block force pushes**;
   - **Require a pull request before merging**: *Required approvals* = `1` y *Dismiss stale pull request approvals when new commits are pushed*;
   - **Require status checks to pass**: con *Add checks*, agregar `Construccion`, `Pruebas unitarias y cobertura`, `Reglas de arquitectura`, `Pruebas de integracion`, `Seguridad - SAST (CodeQL)`, `Seguridad - dependencias (SCA)`, `Seguridad - deteccion de secretos` e `Imagen Docker y escaneo`. Aparecen después de la primera ejecución de `CI`.
4. **Create**.

**Resultado esperado:** un `git push origin main` directo se rechaza con `GH013: Repository rule violations found`.

### 5.4 Publicar la primera imagen

La imagen la construye y publica el pipeline, no un integrante. La primera vez:

1. En **Actions → CD → Run workflow**, elegir la rama `main` y pulsar **Run workflow**.
2. Esperar a que terminen `CI` y **Publicar imagen en GHCR** (unos 8 minutos).
   - **Resultado esperado:** `CI` y `Publicar imagen` en verde. **Desplegar en Render falla**, porque todavía no hay servicio ni secretos. Es lo esperado.
3. Abrir el resumen del job **Publicar imagen en GHCR** y copiar la línea `Imagen publicada: ghcr.io/<usuario-github>/rpg-servidor:sha-<sha>`.
4. En GitHub, ir a **tu perfil u organización → Packages → rpg-servidor → Package settings → Change visibility → Public**.
   - **Resultado esperado:** `docker pull ghcr.io/<usuario-github>/rpg-servidor:sha-<sha>` funciona sin iniciar sesión.

### 5.5 Crear el servicio en Render

1. En [dashboard.render.com](https://dashboard.render.com), pulsar **New → Web Service → Existing Image**.
2. *Image URL*: la imagen del paso 5.4 (`ghcr.io/<usuario-github>/rpg-servidor:sha-<sha>`). Pulsar **Connect**.
3. Completar el formulario:
   - *Name*: `<servicio>` (por ejemplo `rpg-servidor`);
   - *Region*: **Virginia (US East)**, la más cercana a los runners de GitHub, que miden el SLO;
   - *Instance Type*: **Free** para pruebas o **Starter** para producción.
4. En **Environment Variables**, agregar:
   - `JOIN_CODE` = `<codigo-de-sala>` (generado en la sección 4);
   - `MAX_PLAYERS` = `32`.

   No defina `PORT`: Render lo asigna.
5. En **Advanced → Health Check Path**, escribir `/health`.
6. Pulsar **Deploy Web Service**.
   - **Resultado esperado:** en *Events* aparece `Deploy live` en uno o dos minutos, y `https://<servicio>.onrender.com/health` responde `{"status":"UP",...}`.

### 5.6 Registrar los secretos del pipeline

1. En Render, abrir el servicio → **Settings → Deploy Hook** y copiar la URL.
2. En GitHub, ir a **Settings → Environments → New environment**, con el nombre `production` → **Configure environment**.
   - *(Opcional)* Marcar *Required reviewers* si se quiere aprobar cada despliegue a mano.
   - En **Environment secrets → Add environment secret**, crear:
     - `RENDER_DEPLOY_HOOK_URL` = la URL del deploy hook;
     - `JOIN_CODE` = el mismo `<codigo-de-sala>` de Render.
   - En **Environment variables → Add environment variable**, crear `APP_URL` = `https://<servicio>.onrender.com`, sin barra al final.

Lo mismo con GitHub CLI:

```bash
gh secret set RENDER_DEPLOY_HOOK_URL --env production --body '<deploy-hook-url>'
gh secret set JOIN_CODE --env production --body '<codigo-de-sala>'
gh variable set APP_URL --env production --body 'https://<servicio>.onrender.com'
```

**Resultado esperado:** `gh secret list --env production` muestra los dos secretos, y `gh variable list --env production` muestra `APP_URL`.

### 5.7 Primer despliegue automático

1. Volver a la ejecución del paso 5.4 y pulsar **Re-run failed jobs**, o lanzar **Actions → CD → Run workflow** de nuevo.
2. **Resultado esperado:** todos los jobs en verde:
   - **Desplegar en Render** termina con `Servicio listo: status=UP version=<sha>`;
   - **Prueba de humo** termina con `checks.....: 100.00%`;
   - **Prueba de carga (SLO)** termina con los *thresholds* `move_latency p(95)<150` y `move_failed rate<0.01` en verde;
   - **DAST** termina sin alertas WARN.

Desde aquí, cada PR que se fusiona a `main` se despliega solo.

### 5.8 Datos iniciales

No hay base de datos que poblar:

- El **catálogo** (7 razas, 6 clases, 12 habilidades y 10 prendas) viaja dentro del cliente, en `src/main/resources/data/*.json`. Para cambiarlo, se edita el JSON, se fusiona y se publica una versión nueva del cliente (sección 7.1).
- Los **personajes** de cada jugador se crean en su cliente y se guardan en su equipo, en `~/.rpg-character-creator/characters.json` (en Windows, `C:\Users\<usuario>\.rpg-character-creator\characters.json`). El cliente trae tres plantillas NPC que se pueden clonar desde la galería.
- El **estado de la partida** vive en memoria del servidor y empieza vacío.

### 5.9 Primer administrador

El sistema no tiene cuentas de usuario ni rol de administrador dentro de la aplicación. La administración es de infraestructura:

| Rol | Cómo se otorga | Qué puede hacer |
|---|---|---|
| Administrador del servicio | Invitarlo a la cuenta de Render: *Workspace Settings → Members → Invite* | Ver logs, cambiar variables (`JOIN_CODE`), reiniciar, regenerar el deploy hook |
| Administrador del pipeline | *Settings → Collaborators and teams* del repositorio, con el rol **Admin** o **Maintain** | Fusionar PR, lanzar `Rollback`, cambiar secretos del Environment |
| Organizador de la partida | Quien conoce el `JOIN_CODE` | Repartir el código a los jugadores |

**Resultado esperado:** la persona invitada inicia sesión en Render y ve el servicio, y en GitHub ve la pestaña *Settings* del repositorio.

### 5.10 Instalar el cliente en el equipo de un jugador

1. Instalar Java 17. En Windows: `winget install EclipseAdoptium.Temurin.17.JRE`. En macOS: `brew install --cask temurin@17`. En Ubuntu: `sudo apt install openjdk-17-jre`.
   - **Resultado esperado:** `java -version` muestra `17` o una versión mayor.
2. Descargar de `https://github.com/<usuario-github>/rpg-character-creator/releases/latest` el archivo de su sistema: `rpg-cliente-X.Y.Z-windows.jar`, `-linux.jar` o `-macos.jar`.
3. Abrir el creador de personajes:

   ```bash
   java -jar rpg-cliente-X.Y.Z-windows.jar
   ```

   - **Resultado esperado:** se abre la galería de personajes. Crear un personaje con el wizard.
4. Abrir la partida:

   ```bash
   java -jar rpg-cliente-X.Y.Z-windows.jar --jugar
   ```

   1. Elegir el personaje.
   2. En *URL del servidor*, escribir `wss://<servicio>.onrender.com/ws`.
   3. En *Código de sala*, escribir `<codigo-de-sala>`.

   - **Resultado esperado:** se ve el mapa con el personaje en la zona de aparición, y las flechas o WASD lo mueven.

---

## 6. Verificación

### 6.1 Healthcheck

```bash
curl -s https://<servicio>.onrender.com/health
```

**Resultado esperado:** `{"status":"UP","version":"<sha-del-ultimo-commit-de-main>","players":0,"maxPlayers":32}`. La `version` debe coincidir con el SHA de la última ejecución verde de `CD`.

> Con el plan Free, la primera petición después de 15 minutos sin tráfico tarda entre 30 y 60 segundos.

### 6.2 Prueba de humo

Automática en cada despliegue (job **Prueba de humo**). Para correrla a mano, con [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) instalado:

```bash
k6 run --env HTTP_URL=https://<servicio>.onrender.com \
       --env WS_URL=wss://<servicio>.onrender.com/ws \
       --env JOIN_CODE=<codigo-de-sala> perf/scripts/smoke_k6.js
```

**Resultado esperado:** todos los *checks* con ✓ y `checks...: 100.00%`. La prueba verifica `/health`, el `join`, la recepción del mapa y un movimiento reflejado en el estado.

### 6.3 Recorrido por el flujo principal

1. El jugador A abre el cliente con `--jugar` y se une con el código.
   - **Esperado:** ve el mapa y su personaje.
2. El jugador B hace lo mismo desde otro equipo.
   - **Esperado:** ambos se ven en la zona de aparición.
3. A se mueve con las flechas.
   - **Esperado:** B lo ve moverse en menos de un cuarto de segundo.
4. A intenta entrar al agua.
   - **Esperado:** no se mueve, porque el servidor rechaza la casilla.
5. Un tercer jugador escribe un código incorrecto.
   - **Esperado:** aviso *"código de sala incorrecto"* y no entra.
6. B cierra la ventana.
   - **Esperado:** desaparece del mapa de A.
7. `curl -s https://<servicio>.onrender.com/metrics | grep rpg_moves_total`.
   - **Esperado:** el contador `result="accepted"` aumentó.

---

## 7. Operación

### 7.1 Publicar una nueva versión

1. Crear una rama, hacer el cambio y abrir un PR hacia `main`:

   ```bash
   git switch -c feature/<cambio>
   # ...editar...
   git commit -am "feat: <descripcion>"
   git push -u origin feature/<cambio>
   gh pr create --fill
   ```

2. Esperar a que los checks de `CI` estén en verde y a que otro integrante apruebe. Fusionar con **Squash and merge**.
   - **Resultado esperado:** `CD` corre solo y despliega. `/health` muestra el SHA nuevo.
3. Para marcar una versión con nombre, agregar la entrada en `CHANGELOG.md` (en un PR) y luego:

   ```bash
   git switch main && git pull
   git tag -a vX.Y.Z -m "vX.Y.Z"
   git push origin vX.Y.Z
   ```

   - **Resultado esperado:**
     - `Release` crea el GitHub Release con las notas y los tres `.jar` del cliente;
     - `CD` publica la imagen `ghcr.io/<usuario-github>/rpg-servidor:X.Y.Z` y la despliega.

### 7.2 Volver a la versión anterior

1. **Actions → Rollback → Run workflow**.
   - *version*: la etiqueta de la imagen buena, por ejemplo `1.0.0` o `sha-<sha completo>`. Las disponibles están en *Packages → rpg-servidor*.
   - *reason*: el motivo.
2. **Resultado esperado:** el job termina en verde con `Servicio listo: status=UP version=<sha-de-esa-version>` y la prueba de humo pasa.

Sin Actions, con el deploy hook a mano:

```bash
RENDER_DEPLOY_HOOK_URL='<deploy-hook-url>' bash scripts/render-deploy.sh ghcr.io/<usuario-github>/rpg-servidor:1.0.0
bash scripts/wait-for-health.sh https://<servicio>.onrender.com
```

Render también permite **Rollback** desde *Events* sobre un despliegue anterior. Después, `/health` debe mostrar el SHA esperado.

### 7.3 Copia de seguridad y restauración de los datos

| Dato | Dónde vive | Copia | Restauración |
|---|---|---|---|
| Personajes de un jugador | `~/.rpg-character-creator/characters.json` en su equipo | `cp ~/.rpg-character-creator/characters.json ~/respaldo-personajes-$(date +%F).json` (PowerShell: `Copy-Item "$HOME\.rpg-character-creator\characters.json" "$HOME\respaldo-personajes-$(Get-Date -f yyyy-MM-dd).json"`) | Cerrar el cliente, copiar el respaldo sobre `characters.json` y abrir el cliente. **Esperado:** la galería muestra los personajes del respaldo |
| Configuración del servidor | Variables de entorno en Render | Render → *Environment* → copiar los valores a un gestor de contraseñas | Volver a crearlas en *Environment* y pulsar *Save, rebuild and deploy* |
| Imágenes de cada versión | GHCR | Se conservan solas: no se borran | Reversión (7.2) |
| Estado de la partida | Memoria del servidor | **No se respalda.** Es efímero por diseño (ADR-001): un reinicio empieza una partida nueva | — |

### 7.4 Cambiar un secreto

**Código de sala:**

1. Generar uno nuevo (sección 4).
2. En Render → *Environment*, cambiar `JOIN_CODE` → **Save, rebuild and deploy**.
3. En GitHub, actualizar el secreto:

   ```bash
   gh secret set JOIN_CODE --env production --body '<nuevo-codigo>'
   ```

4. Avisar el nuevo código a los jugadores.

**Resultado esperado:** con el código viejo, el cliente muestra *"código de sala incorrecto"*. La siguiente ejecución de `CD` pasa la prueba de humo con el nuevo.

**Deploy hook** (si se filtró):

1. En Render → *Settings → Deploy Hook → Regenerate*.
2. Actualizar el secreto en GitHub:

   ```bash
   gh secret set RENDER_DEPLOY_HOOK_URL --env production --body '<nuevo-hook>'
   ```

**Resultado esperado:** el hook viejo responde 404 y el siguiente despliegue funciona.

### 7.5 Consultar los logs

- **Render:** servicio → **Logs**. Se puede filtrar por texto, por ejemplo `event=join_rejected` o `event=move_rejected`. Cada evento trae el jugador, la IP y el motivo.
- **Local:**

  ```bash
  docker compose logs -f servidor
  ```

- **Pipeline:** Actions → la ejecución → el job, o los artefactos `reporte-*`.

**Resultado esperado:** líneas como `[server] event=join player=Aerin#123 ip=190.x.x.x`.

### 7.6 Reiniciar el servicio

- **Render:** servicio → **Manual Deploy → Restart service**, o *Deploy latest reference*.
- **Local:**

  ```bash
  docker compose restart servidor
  ```

**Resultado esperado:** `/health` vuelve a responder UP en menos de un minuto. La partida en curso termina, porque el estado está en memoria, y los jugadores deben volver a unirse.

### 7.7 Tablero de observabilidad (opcional)

```bash
docker compose --profile observabilidad up --build -d
```

Abrir `http://localhost:3000`, entrar con `GRAFANA_ADMIN_USER` y `GRAFANA_ADMIN_PASSWORD`, e ir a *Dashboards → RPG → RPG - SLO del servidor de partida*.

**Resultado esperado:** los paneles de p95, tasa de error y throughput, con la línea del SLO.

Para cargarlo con datos, en otra terminal:

```bash
k6 run --env PLAYERS=8 --env DURATION=2m --env JOIN_CODE=<codigo-de-sala> perf/scripts/lan_ws_k6.js
```

---

## 8. Solución de problemas

| # | Síntoma | Causa | Cómo resolverlo |
|---|---|---|---|
| 1 | La primera conexión del día tarda 30–60 s o el cliente dice *"No se pudo conectar"* | Render Free suspende el servicio tras 15 min sin tráfico | Abrir `https://<servicio>.onrender.com/health` en el navegador, esperar el `UP` y volver a conectar. Definitivo: plan Starter (sección 3) |
| 2 | El cliente no conecta con la URL de la nube; en LAN sí | Se escribió `ws://` o se olvidó `/ws`. Render solo acepta TLS | Usar `wss://<servicio>.onrender.com/ws` |
| 3 | El cliente muestra *"código de sala incorrecto"* | `JOIN_CODE` distinto entre Render y lo que escribió el jugador, o con espacios | Revisar el valor en Render → *Environment*. El cliente quita los espacios de los extremos. Si se cambió, avisar el nuevo |
| 4 | El cliente muestra *"ya hay un jugador con ese nombre"* | Otra conexión viva usa ese `playerId`, por ejemplo una ventana anterior sin cerrar | Cerrar la otra ventana, o esperar a que el servidor detecte la desconexión (hasta 10 min por inactividad) |
| 5 | `docker compose up` deja el servidor `unhealthy` o sale con `La variable MAX_PLAYERS debe ser un numero` | Un valor no numérico en `.env`, o `.env` no existe | `cp .env.example .env`, corregir el valor y `docker compose up --build`. Ver `docker compose logs servidor` |
| 6 | `docker compose up` falla con `Bind for 0.0.0.0:8887 failed: port is already allocated` | Otro proceso, por ejemplo un servidor del Corte 2 o IntelliJ, usa el 8887 | Detener el otro proceso, o cambiar `PORT=8890` en `.env`. En Windows, para ver quién lo usa: `netstat -ano \| findstr :8887` |
| 7 | En Render el despliegue queda en *Failed* con `failed to pull image` | El paquete de GHCR es privado | *Packages → rpg-servidor → Package settings → Change visibility → Public*, o agregar en Render una *Registry Credential* con un token de GitHub con `read:packages` |
| 8 | El job **Desplegar en Render** falla con `falta el secreto RENDER_DEPLOY_HOOK_URL`, o `El servicio no respondio la version esperada` | El Environment `production` no tiene el secreto, o `APP_URL` está mal (barra final, `http`) | Revisar la sección 5.6. `APP_URL` sin barra final y con `https://` |
| 9 | El job **Prueba de carga (SLO)** falla con `move_latency p(95)<150` en rojo | Latencia de red del runner a la región, el arranque en frío o la instancia Free (0.1 CPU) saturada | Comprobar que la región sea Virginia. Relanzar con **Re-run failed jobs**. Si se repite, usar Starter y revisar el tablero de Grafana. **No subir el umbral** sin un ADR |
| 10 | El cliente no abre: `Error: JavaFX runtime components are missing` o `UnsupportedClassVersionError` | Se ejecutó una clase distinta del `.jar` del Release, o Java es anterior a 17 | Usar `java -jar rpg-cliente-...jar` (el `Launcher` resuelve JavaFX). Instalar Java 17+ y revisar `java -version` |
| 11 | El cliente de macOS dice que el archivo es de un desarrollador no identificado | El `.jar` no está firmado | Ejecutarlo con `java -jar` desde la Terminal, o *Configuración del Sistema → Privacidad y seguridad → Abrir igualmente* |

---

## 9. Recomendaciones para producción

Lo que el negocio debe cambiar respecto al entorno del proyecto:

1. **Credenciales de ejemplo.** No usar `cambie-este-codigo` ni `cambie-esta-clave` de `.env.example`. Generar el `JOIN_CODE` con `openssl rand -hex 10` y rotarlo cada temporada o cuando salga un jugador del grupo (7.4).
2. **Plan Starter de Render** (7 USD al mes) o superior. Así el servicio no se suspende, tiene 0.5 CPU en lugar de 0.1, y el SLO se cumple con margen.
3. **HTTPS.** Ya viene con Render. Con un dominio propio: Render → *Settings → Custom Domains*, agregar `juego.<dominio>` y crear el `CNAME` que indica. El certificado se emite solo. En un servidor propio, poner delante un proxy con TLS (Caddy o Nginx con Let's Encrypt) y no exponer el 8887 directamente.
4. **Copias de seguridad programadas.** Pedir a cada jugador que respalde `characters.json` (7.3). Si los personajes pasan a ser del servidor, llevarlos a una base de datos gestionada con respaldo diario (Render Postgres con *point-in-time recovery*) implementando `CharacterDAO` (ADR-003).
5. **Aprobación antes de producción.** En el Environment `production`, activar *Required reviewers* para que cada despliegue espere a una persona.
6. **Monitoreo.** Alojar Prometheus y Grafana (o Grafana Cloud) y apuntar el *scrape* a `https://<servicio>.onrender.com/metrics`. Activar las alertas de `observabilidad/prometheus/alertas.yml` (p95 > 150 ms, errores > 1 %).
7. **Tamaño de los recursos.** 512 MB y 0.5 CPU sostienen la partida de 32 jugadores medida en el Corte 2. Más jugadores requieren cambiar el diseño (ADR-001), no la máquina.
8. **Riesgos aceptados que siguen abiertos** ([docs/seguridad.md §4](seguridad.md#4-registro-de-hallazgos)):
   - **H-06:** `/metrics` es público. Restringirlo con un token o publicarlo solo en la red interna.
   - **H-07:** el código de sala es compartido y no identifica personas. Si importa saber quién es quién, agregar inicio de sesión (OAuth) y un token por jugador.
   - **H-08:** `ws://` sin TLS en modo LAN. Usar `wss://` también en la red local, o limitar el modo LAN a redes de confianza.
