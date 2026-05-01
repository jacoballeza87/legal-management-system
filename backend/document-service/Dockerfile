FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY document-service/pom.xml ./document-service/pom.xml
RUN mvn dependency:go-offline -B -f ./document-service/pom.xml

COPY document-service/src ./api-gateway/src
RUN mvn clean package -DskipTests -f ./document-service/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/document-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]