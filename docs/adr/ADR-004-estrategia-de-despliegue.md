# ADR-004: Estrategia de despliegue: imagen en GHCR, Render y cliente en Releases

Estado: aceptado e implementado.

Fecha: noviembre de 2026.

## Contexto

El sistema tiene dos partes con necesidades distintas:

- **El servidor de partida** (`ServerMain`, Javalin). Es el componente de servidor y lo único que se contenedoriza y se despliega. Mantiene el estado en memoria (ADR-001), habla WebSocket y necesita una URL pública con TLS para que jueguen personas que no están en la misma red.
- **El cliente de escritorio** (JavaFX): el creador de personajes y la ventana de juego. No se despliega en un servidor. Se distribuye.

Restricciones del curso: costo cero, despliegue sin pasos manuales en cada cambio, secretos fuera del repositorio y una forma probada de volver a la versión anterior.

## Opciones consideradas para el servidor

| | Render (plan gratuito) | Railway | Fly.io | Azure for Students (Container Apps) | Servidor propio con runner autohospedado |
|---|---|---|---|---|---|
| Costo | 0 USD | Crédito de prueba que se agota | Pide tarjeta | Crédito de 100 USD/año | Un equipo encendido |
| WebSocket y TLS | Sí, TLS automático | Sí | Sí | Sí | Hay que montar el proxy y el certificado |
| Despliega una imagen ya construida | Sí, con `imgURL` en el deploy hook | Sí | Sí | Sí | Sí, con Compose |
| Suspensión por inactividad | Sí, tras 15 min (arranque en frío de unos 30–60 s) | No | Configurable | Escala a cero | No |
| URL accesible para el profesor | Sí, `*.onrender.com` | Sí | Sí | Sí | Solo si se expone a Internet |
| Complejidad | Baja | Baja | Media (CLI, `fly.toml`) | Media-alta (suscripción, registro, roles) | Alta |

## Decisión

**Servidor.**

- El pipeline construye **una sola imagen** por commit y la publica en **GitHub Container Registry** con dos etiquetas: `sha-<sha completo>` siempre, y la versión semántica (`1.2.0` y `1.2`) cuando el commit tiene tag. **Nunca se despliega `latest`**: `metadata-action` lo tiene desactivado y `scripts/render-deploy.sh` lo rechaza.
- Esa imagen exacta se despliega en un **Web Service de Render basado en imagen**. Se llama al *deploy hook* con `imgURL=ghcr.io/...:sha-<sha>`. La imagen que pasó el escaneo de Trivy es la misma que corre en producción: Render no recompila nada.
- El deploy hook y el código de sala son secretos del **GitHub Environment `production`**. La URL pública es la variable `APP_URL` del mismo entorno.
- Después del despliegue, el pipeline espera a que `/health` responda con el SHA nuevo. Eso cubre la suspensión del plan gratuito. Luego corre la prueba de humo, la de carga (SLO) y ZAP.
- **Reversión:** `rollback.yml` vuelve a llamar al deploy hook con la imagen de un tag anterior, que sigue en GHCR, y repite la prueba de humo. No reconstruye nada, así que tarda lo que tarda Render en cambiar de imagen.

**Cliente.** `release.yml` lo compila en Windows, Linux y macOS (JavaFX trae librerías nativas por sistema operativo) y lo adjunta al GitHub Release de cada tag, junto con las notas de `CHANGELOG.md`.

Se descartaron:

- **Railway**, porque el crédito se agota a mitad del semestre.
- **Fly.io**, porque pide tarjeta.
- **Azure**, por complejidad: suscripción, registro propio y asignación de roles, demasiado para un servicio de un contenedor. Queda como la recomendación para producción en [docs/implementacion.md](../implementacion.md).
- **El servidor propio**, porque la URL no quedaría accesible para la calificación.

## Consecuencias

- **Arranque en frío.** El plan gratuito suspende el servicio tras 15 minutos sin tráfico, y la primera conexión tarda unos segundos. Está documentado en la guía como problema conocido. Para el negocio se recomienda el plan pago más pequeño, que no se suspende.
- **Una réplica.** El estado está en memoria, así que hay una réplica y un despliegue implica reiniciar la partida. Es el mismo límite del ADR-001, ahora en la nube: el anfitrión ya no es un jugador que puede cerrar la aplicación, pero sigue habiendo un único proceso.
- **Imagen pública.** El paquete de GHCR debe ser público, o Render debe tener una credencial de registro para descargarlo. Se eligió público porque la imagen no contiene secretos: la configuración llega por variables de entorno.
- **Kubernetes.** Para el entorno local o un clúster propio están los manifiestos de `k8s/`, probados en kind por `k8s.yml`, con las mismas probes sobre `/health`.
