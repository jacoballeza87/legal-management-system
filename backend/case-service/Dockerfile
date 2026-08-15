# Build context: backend/
# docker build -f case-service/Dockerfile -t case-service .

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
COPY case-service/pom.xml ./case-service/pom.xml
RUN mvn dependency:go-offline -B -f ./case-service/pom.xml

COPY case-service/src ./case-service/src
RUN mvn clean package -Dmaven.test.skip=true -f ./case-service/pom.xml

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/case-service/target/*.jar app.jar

# Puerto real: 8083
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
