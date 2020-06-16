FROM openjdk:8-jre-alpine

ENV SERVICE_FILE vehiclesimulator-1.0.0-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV SERVICE_HOME /usr/service

EXPOSE 8081

# Copy your fat jar to the container
COPY build/libs/$SERVICE_FILE $SERVICE_HOME/

# Launch the verticle
WORKDIR $SERVICE_HOME

ENTRYPOINT ["sh", "-c"]

CMD ["exec java -XX:+ExitOnOutOfMemoryError -jar $SERVICE_FILE"]
