# README #
This repository holds the API gateway service verticle and its supporting classes.

### What is this repository for? ###

* API gateway Service provides a single entry point for all customer services.
* 1.0-SNAPSHOT

### Pre-requisite 
Make sure you have build the service-parent and service-common project first following microservices are already running.
* use-db-service
* Notification service
* registration service

### How to Run 

* mvn clean package to package the fat jar
* java -jar target/api-gateway-fat.jar -conf src/main/conf/conf.json -cluster

### Hot Deployment - 
Thanks to https://github.com/burrsutter/vertx-achievement-service/blob/master/redeploy.sh

* ./redeploy.sh