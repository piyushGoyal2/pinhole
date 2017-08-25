# MQTT webhooks which are triggerd on vaious mqtt oprations.

https://vernemq.com/docs/plugindevelopment/publishflow.html
https://vernemq.com/docs/plugindevelopment/sessionlifecycle.html
https://vernemq.com/docs/plugindevelopment/publishflow.html

MQTT commands:

```
docker run -p 1883:1883 -e "DOCKER_VERNEMQ_ALLOW_ANONYMOUS=on" --name vernemq1 -p 1883:1883 -d erlio/docker-vernemq
docker exec -it vernemq1 /bin/bash
vmq-admin plugin enable -n vmq_webhooks
vmq-admin webhooks status
Check your host machine IP which can communicate from vernemq

vmq-admin plugin show
vmq-admin plugin disable -n vmq_passwd
vmq-admin plugin disable -n vmq_acl  

vmq-admin webhooks register hook=on_unsubscribe endpoint="http://microservice.pinhole.tech:8080/unsubscribe"
vmq-admin webhooks register hook=auth_on_subscribe endpoint="http://microservice.pinhole.tech:8080/auth/subscribe"
vmq-admin webhooks register hook=auth_on_publish endpoint="http://microservice.pinhole.tech:8080/auth/publish"
vmq-admin webhooks register hook=auth_on_register endpoint="http://microservice.pinhole.tech:8080/auth/register"

```