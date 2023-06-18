FROM adoptopenjdk:11-jdk-hotspot

WORKDIR /app

COPY build/libs/awsstorage-0.0.1-SNAPSHOT.jar /app

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/awsstorage-0.0.1-SNAPSHOT.jar"]
