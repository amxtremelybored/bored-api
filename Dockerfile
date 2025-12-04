FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

# Firebase creds will be mounted at runtime
ENV GOOGLE_APPLICATION_CREDENTIALS=/app/secret/firebase-service-account.json

EXPOSE 7082

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]