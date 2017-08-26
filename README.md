# pinhole

A Scalable image sharing service which we tried to develop for anyone who want to share images/albums with contacts in your 
phone. Pinhole has album based context rather than chat based context so it takes middle path between Whatsapp and Instagram.

More details around this can be found here. LINK

How to build.
1. Go to build-scripts module.
2. Run ./buildAll.sh pull build docker-run
* pull gets the latest code from repo
* build builds fat jar microservices
* docker-run build the docker images of microservices and run the container of the images.


Docker compose trigger the following things

1. It get the latest mysql image from public docker image repo and runs a mysql in container.
2. Builds a docker image of Start user-db-service and run in within a container. 
3. Builds a docker image of Start notification-service and run in within a container. 
4. Builds a docker image of Start registration-service and run in within a container. 


### MQTT Kafka connector
For some of the message which are coming to some specific topic we need to persist them but rather than directly 
persisting those messages to DB(Cassandra), we are have added a buffering mechanism for the message using kafka to avoid
any back pressure coming from Cassandra. To hear every message which coming to MQTT server you can subscribe to a 
wildcard topic as explained here http://www.hivemq.com/blog/mqtt-essentials-part-5-mqtt-topics-best-practices. This 
service consumes all the message coming to a wildcard topic in mqtt server and send them to kafka queue.


### Kafka cassandra connector
All the message which are buffered in the kafka are consumed by this microservice and pushed to cassandra 
for final persistence. This route consumes all the messages from a kafka topic and depending upon message content
persist it into specific cassandra tables.


### MQTT Webhooks
To authenticate and authorize subscription to any MQTT topic we have created 

. We are using this to persist every message 
