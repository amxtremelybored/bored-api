# Use official OpenJDK 21 JDK image (Alpine variant)
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy built jar
COPY build/libs/*.jar app.jar

# ✅ Copy Firebase service account from repo into container
#    Source: src/main/resources/firebase-service-account.json
#    Target: /app/bored/firebase-service-account.json
COPY src/main/resources/firebase-service-account.json /app/bored/firebase-service-account.json

EXPOSE 7082

# Run Spring Boot with prod profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]