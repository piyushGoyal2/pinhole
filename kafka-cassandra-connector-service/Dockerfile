FROM java:8u77-jre-alpine

# Verticle Name
ENV VERTICLE_FILE target/kafka-cassandra-connector-service-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /opt/verticles

# Expose Microservice Port on Host Machine , Not needed for Notification Service
EXPOSE 1883

# Copy the verticle to verticles location in image
COPY $VERTICLE_FILE $VERTICLE_HOME/


WORKDIR $VERTICLE_HOME

ENTRYPOINT ["sh", "-c"]

CMD ["java -jar kafka-cassandra-connector-service-fat.jar -cluster"]
