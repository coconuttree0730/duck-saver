FROM eclipse-temurin:17-jre

ADD ./target/notification-service.jar /app/

CMD ["java", "-Xmx200m", "-jar", "/app/notification-service.jar"]

EXPOSE 8000
