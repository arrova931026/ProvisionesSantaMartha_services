# ── Stage 1: Build con Maven ─────────────────────────────────────────────────
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# Descarga dependencias primero (capa cacheada)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ligero ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Carpeta de fotos de perfil persistida vía volumen
RUN mkdir -p /profile_pictures

COPY --from=build /app/target/funeraria-api-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
