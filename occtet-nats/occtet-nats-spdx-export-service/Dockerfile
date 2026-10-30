# Start mit einem schlanken Java-Image
FROM eclipse-temurin:21-jre-jammy

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere das JAR-File ins Image
COPY target/*.jar app.jar

# Erstelle einen Nicht-Root-Benutzer und wechsle zu diesem
RUN groupadd -r appuser && useradd -r -g appuser appuser && chown -R appuser:appuser /app
USER appuser

# Starte die Spring Boot Anwendung, automatically using the profile defined in the .env file or fallback
ENTRYPOINT ["java", "-jar", "app.jar"]
