FROM eclipse-temurin:17-jre-alpine
ARG JAR_FILE=target/*.jar
WORKDIR /app

# Copy the fat jar built by Maven
COPY ${JAR_FILE} app.jar

# Limit heap so it fits in Cloud Run defaults
ENTRYPOINT ["java","-Xmx512m","-jar","/app.jar"]
