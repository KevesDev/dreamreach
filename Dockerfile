# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy all project files into the container
COPY . .

# Fix Windows line endings on the gradlew script (just like GitHub Actions)
RUN sed -i 's/\r$//' gradlew

# Compile the Spring Boot application
RUN ./gradlew clean build -x test

# Stage 2: Create the final, lightweight running environment
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the compiled .jar file from Stage 1 into Stage 2
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# Expose the port (Render will override this dynamically, but it's good practice)
EXPOSE 8080

# The command to start the server
ENTRYPOINT ["java", "-jar", "app.jar"]