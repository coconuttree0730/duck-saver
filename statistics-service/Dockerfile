FROM eclipse-temurin:17-jre

ADD ./target/statistics-service.jar /app/

CMD ["java", "-Xmx200m", "-jar", "/app/statistics-service.jar"]

EXPOSE 7000
