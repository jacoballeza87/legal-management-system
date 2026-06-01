FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY case-service/pom.xml ./case-service/pom.xml
RUN mvn dependency:go-offline -B -f ./case-service/pom.xml

COPY case-service/src ./case-service/src
RUN mvn clean package -Dmaven.test.skip=true -f ./case-service/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/case-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]