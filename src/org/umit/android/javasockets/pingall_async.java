package org.umit.android.javasockets;

import android.os.AsyncTask;

public class pingall_async extends AsyncTask<String, String, String> {

		String ipAddress;
		@Override
		protected String doInBackground(String... params) {	
			ipAddress = params[0];
			boolean success;
			String method;
			
			success = javasockets.checkReachable(ipAddress);
			method = "isReachable";
			if(success)
			{
				publishProgress(method);
				return ipAddress;
			}
			
        	/*
			success = javasockets.ping_echo(ipAddress);
        	method = "UDP Channel";
			if(success)
			{
				publishProgress(method);
				return ipAddress;
			}
			*/
            
			success = javasockets.socket_tcp(ipAddress);
			method = "TCP Socket";
			if(success)
			{
				publishProgress(method);
				return ipAddress;
			}
			
        	success = javasockets.ping_socket(ipAddress);
        	method = "SocketChannel 13";
			if(success)
			{
				publishProgress(method);
				return ipAddress;
			}
			
			return "";
		}
		
		protected void onProgressUpdate(String... params)
		{
			javasockets.showResult("method", params[0]);			
		}
		
		protected void onPostExecute(String successIp)
		{
			if(!successIp.equals("")){
				javasockets.showResult("True ", successIp + "");
				javasockets.hosts_found++;
			}
		}
}