# ece-5560-project
ECE 5560 Fall 2016 Project (Group 1)

## Overview

This document contains a brief walkthrough of our code, with specific note to
any changes we made.

## CoAP Code

After examining the Californium codebase, we decided it would be easiest to 
simply modify one of the existing demo applications. To that end, we modified
the following files:

[SecureClient.java](californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureClient.java)
and 
[SecureServer.java](californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java)

and created the following file:

[MyCoapResource.java](californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/MyCoapResource.java)

Each of these files is explained below.

To run these demos, you'll need to have [Maven](https://maven.apache.org/)
and a Java JDK installed. Go to the root californium folder and run 

```bash
mvn clean install -DskipTests
```

You'll need to do this twice - once to build the client, and once to build the
server. In between builds, you'll need to modify the [pom.xml](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/pom.xml)
file in the `demo-apps/cf-secure` folder. Specifically, change [line 20](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/pom.xml#L20)
from "SecureServer" to "SecureClient". Note that the corresponding jar file in
`demo-apps/run` will be overwritten, so you'll need to copy it to a new folder
between runs as well.

### SecureClient.java

This file (as the name suggests) contains the CoAP client code. Our primary
modifications to this file included setting the preshared key, adding the timing
code, and adding a flag to enable or disable the use of DTLS encryption at will.

The first modification, setting the preshared key, occurs on [line 84](https://github.com/acarno/ece-5560-project/blob/6793ee521f911e26bef8c977df9c2b3474f0ce04/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureClient.java#L84):

```java
builder.setPskStore(new StaticPskStore("password", "sesame".getBytes()));
```

Obviously, this is a very weak password, but it suffices for our purposes. The
"builder" object referred to here is a `DTLSConnectorConfig.Builder` object; 
this contains the configuration information for the DTLS connection.

The second modification, the timing code, occurs on [lines 115-118](https://github.com/acarno/ece-5560-project/blob/6793ee521f911e26bef8c977df9c2b3474f0ce04/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureClient.java#L115):

```java
long start = System.nanoTime();
response = client.get();
long end = System.nanoTime();
System.out.println((float)(end-start)/1000000000);
```

We simply wrap the GET request with two calls to `System.nanoTime()`, which
returns a high-resolution counter.

The final modification, adding a flag to enable or disable DTLS encryption,
occurs on [lines 147-150](https://github.com/acarno/ece-5560-project/blob/6793ee521f911e26bef8c977df9c2b3474f0ce04/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureClient.java#L147):

```java
boolean encrypt = false;
if ((args.length > 0) && args[0].equals("--encrypt")) {
    encrypt = true;
}
```

If this flag is set on the command-line, we specifically set the CoAP client
to pass through the DTLS connector on [lines 109-111](https://github.com/acarno/ece-5560-project/blob/6793ee521f911e26bef8c977df9c2b3474f0ce04/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureClient.java#L109):

```java
if (encrypt) {
    client.setEndpoint(new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard()));
} 
```

### SecureServer.java

This file contains the CoAP server code. Our primary modifications to this file
included adding a flag to enable or disable the use of DTLS encryption at will,
adding a custom resource to the server, and setting the preshared key.

The first modification, adding the flag to enable or disable the use of DTLS
encryption, occurs on [lines 62-65](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java#L62) and
acts in the same manner as in the client code above.

If this flag is set on the command-line, we specifically set the CoAP server
to pass all connections through a DTLS connector on [lines 109-110](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java#L109).
If it is not set, we simply create an unencrypted endpoint on [line 112](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java#L112).

```java
if (encrypt) {
    server.addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));
} else {
    server.addEndpoint(new CoapEndpoint(new InetSocketAddress("10.1.1.181", COAP_PORT), NetworkConfig.getStandard()));
}
```

The second modification, adding a custom resource to the server, involves
creating a new MyCoapResource object (see below for details). Whenever a client
references the "`secure`" object on the server, this resource is returned. This
modification occurs on [line 68](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java#L68).

```java
CoapServer server = new CoapServer();
```

The final modification, setting the preshared key, occurs on [line 84](https://github.com/acarno/ece-5560-project/blob/da2df41ef6092e11787947d3457a6c9f46921bf5/californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java#L84) and
is also identical to the CoAP client code above.


### MyCoapResource.java

This file contains the test payload for our experiments. On construction, the
`flatland.txt` file is opened and read into memory. Whenever the server then
receives a GET request for this resource, the text is returned. On the server
side, we time this response (though we do not use this data in our report, 
as the server was run on our secondary platform).

## MQTT Code

We utilized the [Eclipse Paho](http://www.eclipse.org/paho/) and 
[Mosquitto](http://mosquitto.org/) projects for our MQTT tests. Both of these
will need to be installed (in addition to Python 2.x) to run our MQTT code. We
also used the [PyCrypto](https://www.dlitz.net/software/pycrypto/) 
library for manual AES encryption.

The MQTT code consists primarily of two scripts that we wrote:

[SecurePub.py](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py)
and
[SecureSub.py](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecureSub.py)

We also manually generated test certificates and configured the broker to use
these as needed (see the additional files in the `Paho` folder).

### SecurePub.py

This file contains the code for the publisher in our setup (note: we based our
data in our report on the publisher running on the test platform).

The first feature to note is the [`MyClient`](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py#L24) 
class. The only change we make here to the default MQTT client class is in
the `onPublish` method, which is called after a message has successfully been
transmitted to the broker. As we wanted to time the difference between sending
a message with and without encryption, this was a logical function to use as the
termination of our timing code. 

```python
class MyClient(mqtt.Client):
    ''' Subclass of Paho MQTT client '''
    
    def on_publish(self, client, userdata, mid):
        ''' This function is called *after* a message has been successfully
            transmitted. We use this as our indication of when to finish timing
            the client's publish command. '''

        global end_time

        end_time = time.time()
        self.disconnect()
```

Next, moving to the `main` function, we see the bulk of the program logic. 
First, we set up TLS  on [line 44](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py#L43)
if the approriate flag is set on the command line:

```python
if args.tls:
    client.tls_set("./ca.crt")
    client.connect('10.1.1.181', port=8883)
else:
    client.connect('10.1.1.181', port=1883)
```

We begin timing on [line 53](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py#L53).
This captures the cost of using no encryption, performing manual encryption,
or using TLS.

If manual encryption is enabled, we pad the message text appropriately on 
[line 64](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py#L64)
and encrypt it using the PyCrypto AES class on [line 65](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py#L65).

```python
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
```

Finally, we actually publish the message on [line 71](https://github.com/acarno/ece-5560-project/blob/905a36aa90f8db9200d2374d2bca3b859c1e6a57/paho/SecurePub.py#L71):

```python
client.publish("hello/world", sendmsg)
```

### SecureSub.py

This file contains the subscriber code. We did not time the subscriber in our 
report. Please see the comments in the file for more information. In general, 
though, the subscriber follows the same flow as the publisher - just in reverse.
