# syntax=docker/dockerfile:1.7
#
# Imagen del servidor de partida (Corte 3). El cliente JavaFX no va en la
# imagen: se publica como .jar en GitHub Releases (release.yml).
#
# Etapa 1: compilar con Maven y reunir las dependencias de ejecucion.
# Las imagenes base tienen version exacta (nunca latest) y Dependabot abre
# un PR cuando sale una nueva (.github/dependabot.yml).
FROM maven:3-eclipse-temurin-26-noble AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline
COPY src ./src
# Las pruebas corren en el pipeline (ci.yml), no al construir la imagen.
# El servidor no usa JavaFX: se excluye de las librerias copiadas, lo que
# ademas reduce la superficie que escanea Trivy.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -DskipTests \
 && mvn -B -ntp -q dependency:copy-dependencies \
      -DoutputDirectory=/libs -DincludeScope=runtime -DexcludeGroupIds=org.openjfx \
 && cp target/rpg-character-creator-*.jar /libs/

# Etapa 2: solo el JRE, el .jar y sus librerias.
FROM eclipse-temurin:21.0.13_9-jre-noble
# Parches de seguridad del sistema operativo al construir: el escaneo de la
# imagen (Trivy) falla si queda una vulnerabilidad critica o alta con
# correccion disponible.
RUN apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /libs /app/libs

# Version desplegada: el pipeline pasa el SHA del commit y /health la expone,
# asi la prueba de humo confirma que respondio la version nueva.
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION} \
    PORT=8887 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dorg.slf4j.simpleLogger.defaultLogLevel=info"

# Proceso sin privilegios de root.
USER 10001:10001
EXPOSE 8887

# Healthcheck HTTP contra /health en el mismo puerto del WebSocket. Usa bash
# y /dev/tcp para no instalar curl en la imagen.
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORT} && printf "GET /health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && head -1 <&3 | grep -q " 200"' || exit 1

ENTRYPOINT ["java", "-cp", "/app/libs/*", "com.proyecto.rpg.adapters.server.ServerMain"]
