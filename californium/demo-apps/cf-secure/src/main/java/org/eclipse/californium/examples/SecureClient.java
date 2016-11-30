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
 *	  Anthony Carno - modified for research purposes
 ******************************************************************************/
package org.eclipse.californium.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.logging.Level;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite.KeyExchangeAlgorithm;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.californium.scandium.ScandiumLogger;

public class SecureClient {
	
	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);


	private static final String TRUST_STORE_PASSWORD = "rootPass";
	private static final String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
	private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";

	//Secure URI
	private static final String SERVER_URI = "coaps://10.1.1.181/secure";
	//Unsecure URI
	private static final String USERVER_URI = "coap://10.1.1.181/secure";

	private DTLSConnector dtlsConnector;

	public SecureClient() {
		try {
			// load key store
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = getClass().getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());
			in.close();

			// load trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			in = getClass().getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION);
			trustStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
			in.close();

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate("root");

			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));

			/* Set simple pre-shared key.
			   Note that this is the simplest means of using DTLS. More complex
			   methods involve the use of public key infrastructure or
			   certificates. We would like to explore these as future work.
			*/
			builder.setPskStore(new StaticPskStore("password", "sesame".getBytes()));

			builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
			builder.setIdentity((PrivateKey)keyStore.getKey("client", KEY_STORE_PASSWORD.toCharArray()), keyStore.getCertificateChain("client"), true);
			builder.setTrustStore(trustedCertificates);
			dtlsConnector = new DTLSConnector(builder.build());

		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Could not load the keystore");
			e.printStackTrace();
		}
	}

	public void test(boolean encrypt) {

		CoapResponse response = null;
		try {
			URI uri = null;
			if (encrypt) {
				uri = new URI(SERVER_URI);
			} else {
				uri = new URI(USERVER_URI);
			}

			CoapClient client = new CoapClient(uri);
			if (encrypt) {
				client.setEndpoint(new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard()));
			} 

			client.setTimeout(0);
			System.out.println("Sending GET request.");			
			long start = System.nanoTime();
			response = client.get();
			long end = System.nanoTime();
			System.out.println((float)(end-start)/1000000000);


		} catch (URISyntaxException e) {
			System.err.println("Invalid URI: " + e.getMessage());
			System.exit(-1);
		}

		if (response != null) {
			try{
			    PrintWriter writer = new PrintWriter("secure-book.txt", "ASCII");
			    writer.print(response.getResponseText());
			    writer.close();
			} catch (Exception e) {
			   // do something
			}

		} else {
			System.out.println("No response received.");
		}
	}

	public static void main(String[] args) throws InterruptedException {

		/* Check for a simple flag to turn on DTLS.
		   This allows us to use the same script for testing with and without
		   encryption. Doing so ensures that we eliminate other sources of 
		   variance in the timing measurements.
		*/
		boolean encrypt = false;
		if ((args.length > 0) && args[0].equals("--encrypt")) {
			encrypt = true;
		}

		SecureClient client = new SecureClient();
		client.test(encrypt);
		
		synchronized (SecureClient.class) {
			SecureClient.class.wait();
		}
	}
}
