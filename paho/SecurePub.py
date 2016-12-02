#!/usr/bin/env python2

import argparse
import codecs
import time

from Crypto.Cipher import AES
import paho.mqtt.client as mqtt

# Used global variable here for easy of use
start_time = -1
end_time = -1

def get_args():
	''' Parse command line arguments '''
	parser = argparse.ArgumentParser()
	parser.add_argument('--encrypt',
			    action='store_true')
	parser.add_argument('--tls',
			    action='store_true')
	args = parser.parse_args()
	return args

class MyClient(mqtt.Client):
	''' Subclass of Paho MQTT client '''
	
	def on_publish(self, client, userdata, mid):
		''' This function is called *after* a message has been successfully
			transmitted. We use this as our indication of when to finish timing
			the client's publish command. '''

		global end_time

		end_time = time.time()
		self.disconnect()

def main(args):
	global start_time, end_time

	client = MyClient("", True, None, mqtt.MQTTv31)

	# Set up TLS if enabled
	if args.tls:
		client.tls_set("./ca.crt")
		client.connect('10.1.1.181', port=8883)
	else:
		client.connect('10.1.1.181', port=1883)

	book = None
	with codecs.open('book.txt', 'r', 'ascii') as f:
		book = f.read()

	start_time = time.time()

	# If manual AES encryption is enabled, use here
	if args.encrypt:
		obj = AES.new('key123456789abcd', 
			      AES.MODE_CBC, 
			      'Initialization V')
		blen = len(book)
		rem = blen % 16
		# Pad text if necessary
		if rem != 0:
			book = ''.join([book, '0'*(16-rem)])
		sendmsg = obj.encrypt(book)
		sendmsg = bytearray(sendmsg)
	else:
		sendmsg = bytearray(book, 'utf-8')

	# Publish message
	client.publish("hello/world", sendmsg)
	client.loop_forever()

	print(end_time - start_time)

if __name__ == '__main__':
	args = get_args()
	main(args)
