import paho.mqtt.client as mqtt
import sys

#host = "192.168.0.154"
host = "localhost"
port = 1883

def on_connect(client, userdata, flags, rc):
    print("Connected to localhost:1883 with result code : "+str(rc))
    print("Subscribing to topic :" , sys.argv[1])
    client.subscribe(sys.argv[1])

def on_message(client, userdata, msg):	
    print("recevied message on : " + msg.topic+" -> "+str(msg.payload))

if (len(sys.argv) < 2 ):
	print("please provide topic to subscribe")
	exit()

client = mqtt.Client()

client.on_connect = on_connect

client.on_message = on_message

client.connect(host, port, 60)

client.loop_forever()