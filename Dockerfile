FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/*.jar app.jar

RUN mkdir -p /app/news /app/data /app/logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
