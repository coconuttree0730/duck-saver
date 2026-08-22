FROM eclipse-temurin:17-jre

ADD ./target/ai-service.jar /app/

CMD ["java", "-Xmx200m", "-jar", "/app/ai-service.jar"]

EXPOSE 9000
