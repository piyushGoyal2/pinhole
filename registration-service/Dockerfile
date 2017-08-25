FROM java:8u77-jre-alpine

# Verticle Name
ENV VERTICLE_FILE target/registration-service-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /opt/verticles

# Expose Microservice Port on Host Machine
EXPOSE 9091

# Copy the verticle to verticles location in image
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Copy microservices  configuration for docker environment
COPY src/main/conf/docker.json $VERTICLE_HOME/


WORKDIR $VERTICLE_HOME

ENTRYPOINT ["sh", "-c"]

CMD ["java -Dvertx.hazelcast.config=classpath:docker-cluster.xml -jar registration-service-fat.jar -cluster -conf docker.json"]
