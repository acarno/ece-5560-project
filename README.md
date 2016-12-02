# ece-5560-project
ECE 5560 Fall 2016 Project (Group 1)

## CoAP Code

After examining the Californium codebase, we decided it would be easiest to 
simply modify one of the existing demo applications. To that end, we modified
the following files:

[SecureClient.java](californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureClient.java)
[SecureServer.java](californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/SecureServer.java)

and created the following file:

[MyCoapResource.java](californium/demo-apps/cf-secure/src/main/java/org/eclipse/californium/examples/MyCoapResource.java)

Each of these files is explained below.

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
