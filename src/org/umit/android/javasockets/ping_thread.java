package org.umit.android.javasockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class ping_thread implements Runnable{

	String address;
	Process p;
	
	public ping_thread(String address)
	{
		this.address = address;
	}
	
	@Override
	public void run() 
	{
		String pingCmd = "ping " + address;
    	//String pingResult = "";
    	
    	try{
    		Runtime r = Runtime.getRuntime();    		
    		p = r.exec(pingCmd);
    		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		String inputLine;
    		int i=0;
    		while ((inputLine = in.readLine()) != null) 
    		{
    			i++;
    			if(i==5) break;
    			javasockets.showResult("shell ping", inputLine);
    			//pingResult += inputLine;
    		}
    		in.close();
    		p.destroy();
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
	}
}