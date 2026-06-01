FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY auth-service/pom.xml ./auth-service/pom.xml
RUN mvn dependency:go-offline -B -f ./auth-service/pom.xml

COPY auth-service/src ./auth-service/src
RUN mvn clean package -Dmaven.test.skip=true -f ./auth-service/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]