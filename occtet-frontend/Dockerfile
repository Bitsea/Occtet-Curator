FROM eclipse-temurin:21-jre-jammy

#COPY build/libs/occtet-boc-frontend-0.3.10-SNAPSHOT.jar app.jar
COPY build/libs/*.jar app.jar
COPY entrypoint.sh /entrypoint.sh

# Ensure the script has permission to run
RUN chmod +x /entrypoint.sh

EXPOSE 8090

# Run the script instead of running Java directly
ENTRYPOINT ["/entrypoint.sh"]