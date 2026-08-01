# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S bloodbridge && adduser -S bloodbridge -G bloodbridge
COPY --from=builder /app/target/*.jar app.jar
USER bloodbridge
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]