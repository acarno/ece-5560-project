#!/usr/bin/env python2

import argparse
import time

from Crypto.Cipher import AES
import paho.mqtt.client as mqtt

start_time = -1

def get_args():
	''' Parse command line arguments '''
	parser = argparse.ArgumentParser()
	parser.add_argument('--decrypt',
				action='store_true')
	parser.add_argument('--tls',
				action='store_true')
	args = parser.parse_args()
	return args

# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))

    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    client.subscribe("hello/world")

def on_message(client, userdata, message):
	global start_time

	obj, args = userdata
	if start_time == -1:
		start_time = time.time()

	# If manual encryption was enabled (and thus manual decryption is needed)
	if args.decrypt:
		msg = str(obj.decrypt(message.payload))
	else:
		msg = str(message.payload)
	#print("Received message '" + str(msg) + "' on topic '"
        #      + message.topic + "' with QoS " + str(message.qos))

	# Check if end of message reached -- this is hardcoded for testing
	if msg.endswith('00000'):
		end_time = time.time()
		print('Done!')
		print(end_time - start_time)
		start_time = -1

	with open('SecureLog.txt', 'a') as f:
		f.write(msg)

def main(args):
	# Create new AES object in case manual encryption is needed
	obj = AES.new('key123456789abcd', AES.MODE_CBC, 'Initialization V')

	client = mqtt.Client(userdata=(obj, args))
	client.on_connect = on_connect
	client.on_message = on_message

	# Set up TLS if enabled
	if args.tls:
		client.tls_set("./ca.crt")
		client.connect('10.1.1.181', port=8883)
	else:
		client.connect('10.1.1.181', port=1883)

	client.loop_forever()


if __name__ == '__main__':
	args = get_args()
	main(args)
 
