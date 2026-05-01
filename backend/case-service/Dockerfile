FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY case-serivce/pom.xml ./case-serivce/pom.xml
RUN mvn dependency:go-offline -B -f ./case-serivce/pom.xml

COPY case-serivce/src ./case-serivce/src
RUN mvn clean package -DskipTests -f ./case-serivce/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/case-serivce/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]