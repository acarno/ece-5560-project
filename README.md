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