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

# LibreOffice para convertir el contrato Mutualista (.docx) a PDF
RUN apk add --no-cache libreoffice-writer fontconfig font-liberation font-noto \
    && fc-cache -f

# URL pública de la API (se usa en el QR del video de consentimiento), p. ej. https://tu-dominio/api
ENV PUBLIC_API_URL=""

# Carpetas de archivos locales; móntalas como volúmenes para conservarlas al recrear el contenedor.
RUN mkdir -p /app/profile_pictures /app/docs
VOLUME ["/app/profile_pictures", "/app/docs"]

COPY --from=build /app/target/funeraria-api-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
