# Use the official Java 8 image as the base image
FROM openjdk:8-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the executable JAR file and the configuration files to the container
COPY target/awsstorage-0.0.1-SNAPSHOT.jar /app
COPY config/application.properties /app/config/

# Expose the port that the Spring Boot application listens on
EXPOSE 8080

# Set the entrypoint command to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/awsstorage-0.0.1-SNAPSHOT.jar"]