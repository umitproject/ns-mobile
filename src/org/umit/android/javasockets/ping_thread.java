/*
Various methods of Android Ping
Copyright (C) 2011 Angad Singh
angad@angad.sg

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 */


package org.umit.android.javasockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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