# Start mit einem schlanken Java-Image
FROM eclipse-temurin:21-jre-jammy

RUN echo '#!/bin/bash\n\
for cert in /tmp/cacerts/*.cert; do\n\
  if [ -f "$cert" ]; then\n\
    alias=$(basename "$cert" .cert)\n\
    keytool -import -alias "$alias" -file "$cert" \\\n\
      -keystore /opt/java/openjdk/lib/security/cacerts \\\n\
      -storepass changeit -noprompt\n\
  fi\n\
done\n\
java -jar /app/app.jar' > /entrypoint.sh && \
chmod +x /entrypoint.sh

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere das JAR-File ins Image
COPY target/*.jar app.jar

# Starte die Spring Boot Anwendung
ENTRYPOINT ["java", "-Dspring.profiles.active=live","-jar", "app.jar"]
