FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/*.jar /tmp/
RUN rm -f /tmp/*-plain.jar \
  && set -- /tmp/*.jar \
  && [ "$#" -eq 1 ] \
  && mv "$1" /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
