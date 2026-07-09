# Start mit einem schlanken Java-Image
FROM eclipse-temurin:21-jre-jammy

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere das JAR-File ins Image
COPY target/*.jar app.jar

# Erstelle entrypoint script für certificate import und app start
RUN echo '#!/bin/bash\n\
set -e\n\
# Import certificates if any exist in /tmp/cacerts/\n\
if [ -d "/tmp/cacerts" ] && [ "$(ls -A /tmp/cacerts/*.cert 2>/dev/null)" ]; then\n\
  for cert in /tmp/cacerts/*.cert; do\n\
    if [ -f "$cert" ]; then\n\
      alias=$(basename "$cert" .cert)\n\
      keytool -import -alias "$alias" -file "$cert" \\\n\
        -keystore "$JAVA_HOME/lib/security/cacerts" \\\n\
        -storepass changeit -noprompt || echo "Failed to import $cert"\n\
    fi\n\
  done\n\
fi\n\
# Drop privileges and start application\n\
exec su-exec appuser java -jar /app/app.jar "$@"' > /entrypoint.sh && \
chmod +x /entrypoint.sh

# Install su-exec for privilege dropping
RUN apt-get update && apt-get install -y --no-install-recommends su-exec && rm -rf /var/lib/apt/lists/*

# Erstelle einen Nicht-Root-Benutzer und bereite Verzeichnisse vor
RUN groupadd -r appuser && useradd -r -g appuser appuser && \
    chown -R appuser:appuser /app && \
    mkdir -p /tmp/cacerts && chmod 755 /tmp/cacerts

# Starte die Spring Boot Anwendung über entrypoint script
# Das Script läuft als root für certificate import, droppt dann zu appuser
ENTRYPOINT ["/entrypoint.sh"]
