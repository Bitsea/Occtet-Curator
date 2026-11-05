# Start mit einem schlanken Java-Image
FROM eclipse-temurin:21-jre-jammy

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere das JAR-File ins Image
COPY target/*.jar app.jar


# Starte die Spring Boot Anwendung, automatically using the profile defined in the .env file or fallback
# -XX:+UseContainerSupport  → JVM respects Kubernetes resource limits automatically
# -XX:MaxRAMPercentage      → Use at most 75% of the container's RAM limit for the heap;
#                             leaves room for Metaspace, thread stacks, and OS buffers.
# -XX:+ExitOnOutOfMemoryError → Crash-fast on OOM instead of silently hanging / stopping work.
#                               Kubernetes will then restart the pod automatically.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
