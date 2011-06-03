package org.umit.android.javasockets;

import android.os.AsyncTask;

public class ping_async extends AsyncTask<String, String, String> {

		String ipAddress;
		@Override
		protected String doInBackground(String... params) {	
			ipAddress = params[0];
			boolean success = javasockets.checkReachable(ipAddress);
			if(success)
				return ipAddress;
			else return "";
		}
		
		protected void onPostExecute(String successIp)
		{
			if(!successIp.equals("")){
				javasockets.showResult("isReachable ", successIp + "");
				javasockets.hosts_found++;
			}
		}
}
