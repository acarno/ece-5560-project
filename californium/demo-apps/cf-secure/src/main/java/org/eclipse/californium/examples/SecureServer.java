/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package org.eclipse.californium.examples;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.logging.Level;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite.KeyExchangeAlgorithm;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;


public class SecureServer {

	// allows configuration via Californium.properties
	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	public static final int DTLS_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT);

	private static final String TRUST_STORE_PASSWORD = "rootPass";
	private final static String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
	private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";

	public static void main(String[] args) {	

		/* Check for a simple flag to turn on DTLS.
		   This allows us to use the same script for testing with and without
		   encryption. Doing so ensures that we eliminate other sources of 
		   variance in the timing measurements.
		*/
		boolean encrypt = false;
		if ((args.length > 0) && args[0].equals("--encrypt")) {
			encrypt = true;	
		}	
		

		CoapServer server = new CoapServer();
		/* Add resource to CoAP server.
		   This represents the addition of a new resource to the server. This
		   particular resource is designed such that upon instantiation, it
		   reads the contents of the book "Flatland" into memory. This is what
		   is sent upon a "GET" request from a CoAP client. Note that segmenting
		   the book text into multiple messages happens within the Californium
		   library.
		*/
		server.add(new MyCoapResource("secure") {
			@Override
			public void handleGET(CoapExchange exchange) {
				long start = System.nanoTime();
				exchange.respond(ResponseCode.CONTENT, bookText);
				long end = System.nanoTime();
				System.out.println((float)(end - start)/1000000000);
			}
		});

		try {
			// Pre-shared secrets
			/* These are used first, before the public key infrastructure or 
			   certificates are attempted. */
			InMemoryPskStore pskStore = new InMemoryPskStore();
			pskStore.setKey("password", "sesame".getBytes()); // from ETSI Plugtest test spec

			// load the trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			InputStream inTrust = SecureServer.class.getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION);
			trustStore.load(inTrust, TRUST_STORE_PASSWORD.toCharArray());

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate("root");

			// load the key store
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = SecureServer.class.getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());

			DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(new InetSocketAddress(DTLS_PORT));
			config.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8,
					CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
			config.setPskStore(pskStore);
			config.setIdentity((PrivateKey)keyStore.getKey("server", KEY_STORE_PASSWORD.toCharArray()),
					keyStore.getCertificateChain("server"), true);
			config.setTrustStore(trustedCertificates);

			DTLSConnector connector = new DTLSConnector(config.build());
			if (encrypt) {
				server.addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));
			} else {
				server.addEndpoint(new CoapEndpoint(new InetSocketAddress("10.1.1.181", COAP_PORT), NetworkConfig.getStandard()));
			}

			server.start();

		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Could not load the keystore");
			e.printStackTrace();
		}

	}

}
