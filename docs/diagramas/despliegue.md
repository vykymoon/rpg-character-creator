# Vista de despliegue (C4: despliegue)

Muestra dónde corre cada contenedor del sistema y cómo se comunican. Corresponde a lo que despliegan `cd.yml` (producción en Render), `docker-compose.yml` (local) y `k8s.yml` (Kubernetes en kind). Las decisiones están en [ADR-004](../adr/ADR-004-estrategia-de-despliegue.md).

## Producción

```mermaid
flowchart TB
    subgraph PCJ["Computador de cada jugador (Windows / Linux / macOS)"]
        CLI["Cliente de escritorio<br/>rpg-cliente-X.Y.Z-&lt;so&gt;.jar<br/>Java 17 + JavaFX<br/>creador (MainApp) y partida (GameApp)"]
        FILE[("~/.rpg-character-creator/<br/>characters.json")]
        CLI --> FILE
    end

    subgraph GH["GitHub"]
        REPO["Repositorio<br/>main protegida"]
        ACT["GitHub Actions<br/>ci.yml · cd.yml · release.yml<br/>rollback.yml · perf.yml · k8s.yml"]
        GHCR[("GitHub Container Registry<br/>ghcr.io/vykymoon/rpg-servidor<br/>:sha-&lt;sha&gt; · :X.Y.Z")]
        REL[("GitHub Releases<br/>cliente por sistema operativo")]
        ENV["Environment production<br/>RENDER_DEPLOY_HOOK_URL · JOIN_CODE · APP_URL"]
    end

    subgraph RENDER["Render (region Virginia, plan Free)"]
        LB["Proxy de Render<br/>TLS, *.onrender.com:443"]
        subgraph SVC["Web Service rpg-servidor (1 instancia, 512 MB)"]
            CONT["Contenedor rpg-servidor<br/>eclipse-temurin 21 JRE, UID 10001<br/>Javalin / Jetty 12 en $PORT<br/>/ws · /health · /metrics"]
        end
        LB --> CONT
    end

    CLI <-->|"wss:// (WebSocket sobre TLS)<br/>join · move → map · state · error"| LB
    REPO -->|push / PR| ACT
    ACT -->|"docker push (GITHUB_TOKEN)"| GHCR
    ACT -->|"POST deploy hook ?imgURL=…:sha-&lt;sha&gt;"| RENDER
    GHCR -->|"docker pull de la imagen exacta"| CONT
    ACT -->|"HTTPS /health · k6 wss · ZAP"| LB
    ACT -->|tag vX.Y.Z| REL
    REL -.->|descarga| CLI
    ENV -.-> ACT
```

| Elemento | Tecnología | Dónde corre | Se comunica con |
|---|---|---|---|
| Cliente de escritorio | Java 17, JavaFX 21, Java-WebSocket | PC de cada jugador; se descarga de Releases | Servidor por `wss://<servicio>.onrender.com/ws` (o `ws://IP:8887` en LAN) |
| Servidor de partida | Contenedor `rpg-servidor`: JRE 21, Javalin 7 / Jetty 12 | Render, Web Service basado en imagen, una instancia | Clientes (WebSocket) y el pipeline (HTTP `/health`, `/metrics`) |
| Proxy de Render | Gestionado por Render | Render | Termina TLS y reenvía HTTP y WebSocket al `$PORT` del contenedor |
| Registro de imágenes | GHCR | GitHub | Recibe la imagen de `cd.yml`; Render la descarga |
| Pipeline | GitHub Actions (runners `ubuntu-latest`) | GitHub | Publica, despliega, prueba (k6, ZAP) y revierte |

## Local (docker compose)

```mermaid
flowchart LR
    subgraph HOST["Maquina con Docker"]
        S["servidor<br/>rpg-servidor:local<br/>:8887"]
        P["prometheus<br/>prom/prometheus<br/>:9090<br/>(perfil observabilidad)"]
        G["grafana<br/>grafana/grafana<br/>:3000<br/>(perfil observabilidad)"]
        P -->|"scrape /metrics cada 5 s"| S
        G -->|PromQL| P
    end
    C["Clientes JavaFX en la LAN"] <-->|"ws://IP:8887"| S
```

`servidor` tiene un healthcheck sobre `/health`, y `prometheus` espera a que esté sano (`depends_on: condition: service_healthy`).

## Kubernetes (kind, bonificación)

```mermaid
flowchart LR
    subgraph NS["namespace rpg (Pod Security: restricted)"]
        DEP["Deployment rpg-servidor<br/>1 replica · probes /health<br/>100m-500m CPU · 256-512 Mi"]
        SVCK["Service rpg-servidor<br/>ClusterIP :8887"]
        CM["ConfigMap<br/>rpg-servidor-config"]
        SEC["Secret<br/>rpg-servidor-secret (JOIN_CODE)"]
        SA["ServiceAccount sin roles<br/>sin token montado"]
        NP["NetworkPolicy<br/>denegar todo + entrada solo :8887<br/>sin salida"]
        SVCK --> DEP
        CM --> DEP
        SEC --> DEP
        SA --> DEP
        NP -.-> DEP
    end
```
