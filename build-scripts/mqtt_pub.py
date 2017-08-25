import paho.mqtt.client as mqtt
import time
import datetime
import sys

#host = "192.168.0.154"
host = "localhost"
port = 1883
counter=1

def on_connect(client, userdata, flags, rc):
    print("Connected succesfully to localhost:1883 with result code "+str(rc))

def on_publish(client, userdata, mid):
    print("message succesfully sent to topic" + " with ID " +str(mid))

if (len(sys.argv) < 2 ):
	print("please provide topic to publish")
	exit()

client = mqtt.Client()

client.on_connect = on_connect

client.on_publish = on_publish

client.connect(host, port)

client.loop_start()
 
while (counter<=20000):
	message = "mid" + str(counter)
	print("publishing message to topic : " +sys.argv[1] + " -> "+ message)
	(rc, mid) = client.publish(sys.argv[1], message, qos=2)
	time.sleep(0.001)
	counter+=1

print("done")	