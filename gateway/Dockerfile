FROM eclipse-temurin:17-jre

ADD ./target/gateway.jar /app/

CMD ["java", "-Xmx200m", "-jar", "/app/gateway.jar"]

EXPOSE 4000
