FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY notification-service/pom.xml ./notification-service/pom.xml
RUN mvn dependency:go-offline -B -f ./notification-service/pom.xml

COPY notification-service/src ./notification-service/src
RUN mvn clean package -Dmaven.test.skip=true -f ./notification-service/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]