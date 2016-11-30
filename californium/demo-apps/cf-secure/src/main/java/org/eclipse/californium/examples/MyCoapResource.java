package org.eclipse.californium.examples;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.californium.core.CoapResource;

public class MyCoapResource extends CoapResource {
	
	String bookText = "";

	public MyCoapResource(String name) {
		super(name);

		/* Read the contents of the book "Flatland" into memory. */		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("flatland.txt"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (br != null) {
			String everything = "";
			try {
			    StringBuilder sb = new StringBuilder();
			    String line = br.readLine();
	
			    while (line != null) {
			        sb.append(line);
			        sb.append(System.lineSeparator());
			        line = br.readLine();
			    }
			    everything = sb.toString();
			    
			    int rem = everything.length() % 16;
			    if (rem != 0) {
			    	everything = everything + (16-rem) * '0';
			    }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bookText = everything;
			}
		}
		
	}

}
