# Build the Spring Boot jar with Maven, then run it on a slim JRE.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Pre-fetch dependencies (cached layer) then build.
RUN mvn -q -e -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests package

# Multi-arch JRE (works on both amd64 and Apple Silicon/arm64). The -alpine JRE tag is amd64-only.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
