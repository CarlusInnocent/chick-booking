# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY chicke-booking/chicke-booking/.mvn .mvn
COPY chicke-booking/chicke-booking/mvnw chicke-booking/chicke-booking/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY chicke-booking/chicke-booking/src src
RUN ./mvnw package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Cloud Run provides PORT env var (default 8080)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Xmx512m -Dserver.port=${PORT} -Dspring.profiles.active=prod -jar app.jar"]
