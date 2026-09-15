# Build stage
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:26-jre AS runtime
WORKDIR /app

RUN useradd --system --create-home --uid 1000 appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p uploads/profile-images uploads/post-images uploads/message-images logs \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
