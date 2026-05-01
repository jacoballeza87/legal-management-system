FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN mvn dependency:go-offline -B -f ./api-gateway/pom.xml

COPY api-gateway/src ./api-gateway/src
RUN mvn clean package -Dmaven.test.skip=true -f ./api-gateway/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]