# syntax=docker/dockerfile:1

# Etapa 1: compilar con Maven y reunir las dependencias de ejecucion
# (el proyecto compila para Java 17 con <release>17</release>; el JDK 21 lo soporta sin problema)
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline
COPY src ./src
# Las pruebas se ejecutan en CI, no al construir la imagen.
# El servidor no usa JavaFX, asi que se excluye de las librerias copiadas.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -DskipTests \
 && mvn -B -ntp -q dependency:copy-dependencies \
      -DoutputDirectory=/libs -DincludeScope=runtime -DexcludeGroupIds=org.openjfx \
 && cp target/rpg-character-creator-*.jar /libs/

# Etapa 2: imagen final solo con el JRE, el .jar y sus librerias
FROM eclipse-temurin:21.0.12.1_1-jre-noble
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /libs /app/libs
USER app

# Puerto por defecto del servidor WebSocket (se puede cambiar con SERVER_PORT en el compose)
ENV SERVER_PORT=8887
EXPOSE 8887

# El servidor no tiene endpoint HTTP: se comprueba que el puerto acepte conexiones TCP
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT}' || exit 1

# La JVM usa como maximo el 75 % de la memoria que el contenedor tenga asignada.
# "/app/libs/*" incluye el .jar del proyecto y sus dependencias (gson, Java-WebSocket).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-cp", "/app/libs/*", "com.proyecto.rpg.adapters.server.ServerMain"]
# Argumento por defecto: el puerto
CMD ["8887"]
