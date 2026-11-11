# Step 1: Use an official Maven image with OpenJDK 17 to build the application
FROM maven:3.8.5-openjdk-17 AS build
COPY . .

# CRITICAL FIX: Grant Linux execution permissions to the Maven wrapper script
RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

# Step 2: Use the secure, maintained Eclipse Temurin image for runtime
FROM eclipse-temurin:17-jdk
COPY --from=build /target/clinic-0.0.1-SNAPSHOT.jar clinic.jar

# Expose the internal port that Render will assign dynamically
EXPOSE 8080

# Command to execute the Spring Boot application container runtime
ENTRYPOINT ["java", "-jar", "clinic.jar"]