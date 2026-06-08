FROM openjdk:17-slim
WORKDIR /app
COPY target/distributed-kv-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080 9090
CMD ["java", "-jar", "app.jar"]