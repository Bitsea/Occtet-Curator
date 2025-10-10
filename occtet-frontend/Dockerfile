FROM eclipse-temurin:21-jre-jammy
COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=live", "-jar", "/app.jar"]

EXPOSE 8090
