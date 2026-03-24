# Start mit einem schlanken Java-Image
FROM eclipse-temurin:21-jre-jammy

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere das JAR-File ins Image
COPY target/*.jar app.jar

# Starte die Spring Boot Anwendung
ENTRYPOINT ["java","-Dspring.profiles.active=live", "-jar", "app.jar"]
