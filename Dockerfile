# ---- Stage 1: Build the application ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom and src
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ---- Stage 2: Run the application ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built jar from the previous stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Use dynamic port for Railway
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT}"]
