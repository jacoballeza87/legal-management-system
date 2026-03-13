FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S legal && adduser -S legal -G legal
USER legal

COPY eureka-server/target/*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://eureka-server:8761/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
