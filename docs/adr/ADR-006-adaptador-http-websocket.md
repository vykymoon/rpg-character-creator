# ADR-006: El servidor sirve HTTP y WebSocket en un solo puerto con Javalin

Estado: aceptado e implementado.

Fecha: noviembre de 2026.

## Contexto

En el Corte 2 el adaptador de entrada del servidor era un `WebSocketServer` de la librería Java-WebSocket. Esa librería solo habla WebSocket: una petición HTTP normal recibe un `404 WebSocket Upgrade Failure` y nada más.

El Corte 3 necesita HTTP en el servidor por cuatro razones:

1. **Healthcheck.** Compose (`depends_on: condition: service_healthy`), Kubernetes (probes) y Render (health check path) preguntan por HTTP si el servicio está listo.
2. **Prueba de humo.** Después de cada despliegue el pipeline necesita saber que respondió la versión nueva y no la anterior.
3. **DAST.** OWASP ZAP analiza respuestas HTTP.
4. **Métricas.** Prometheus lee `/metrics` por HTTP.

Además, Render, como casi todos los proveedores con plan gratuito, expone **un solo puerto** por servicio. Un segundo puerto para HTTP no sería accesible desde fuera.

## Opciones consideradas

### A. Mantener Java-WebSocket y abrir un segundo puerto con el `HttpServer` del JDK

A favor: no suma dependencias.

En contra: el segundo puerto no se puede publicar en Render, así que el healthcheck del proveedor, la prueba de humo y ZAP no tendrían a qué apuntar. Solo resolvería Compose y Kubernetes.

### B. Healthcheck por TCP (comprobar que el puerto acepta conexiones)

Es lo que tenía el primer Dockerfile del grupo (`/dev/tcp`).

A favor: cero cambios en el código.

En contra: un puerto abierto no significa que el servidor responda. No dice qué versión está corriendo, y no sirve para DAST ni para métricas.

### C. Cambiar el adaptador a Javalin (Jetty 12): HTTP y WebSocket en el mismo puerto

A favor: un solo puerto con `/health`, `/metrics` y el WebSocket del juego en `/` y en `/ws`. Javalin es una capa delgada sobre Jetty y la API de WebSocket es de eventos (`onConnect`, `onMessage`, `onClose`), parecida a la que ya se usaba.

En contra: suma Jetty y Kotlin stdlib como dependencias transitivas, que el control de SCA tiene que vigilar. El tamaño de la imagen crece unos 6 MB.

## Decisión

Se elige la opción C.

El cambio quedó **solo en `adapters/server`**. `GameSessionService` y el paquete `domain` no se tocaron, salvo para cerrar los pendientes del Corte 2. Es la prueba práctica de que la frontera de puertos y adaptadores del ADR-001 funciona, y la regla `caso_de_uso_no_conoce_los_adaptadores` de `ArchitectureTest` impide que deje de funcionar.

El cliente de escritorio sigue usando Java-WebSocket: es un cliente estándar y habla con Jetty sin cambios. Las URL `ws://host:8887` del Corte 2 siguen funcionando, porque el WebSocket también se atiende en `/`.

## Consecuencias

- `/health` devuelve `{"status":"UP","version":"<sha>","players":n,"maxPlayers":m}`. La prueba de humo y la reversión comprueban que `version` sea el SHA desplegado.
- `/metrics` expone las métricas del SLO en formato Prometheus, escritas a mano en `ServerMetrics` para no sumar más dependencias.
- El adaptador concentra los controles del modelo de amenazas: identidad atada a la conexión, código de sala, límite de mensajes y cabeceras de seguridad. Ver [docs/seguridad.md](../seguridad.md).
- La prueba de integración `MultijugadorIT` se adaptó al nuevo arranque (`new GameServer(config).start()`) y se le sumaron las pruebas de seguridad y de los endpoints HTTP.
- El servidor ya no imprime el mensaje recibido cuando llega uno inválido. Antes lo hacía, y eso era una vía de inyección en los logs.
