package org.umit.android.javasockets;

import android.os.AsyncTask;

public class scan_async extends AsyncTask<Object[], Integer, Void> {
	String[] all;
	
	@Override
	protected Void doInBackground(Object[]... params)
	{
		all = (String[])params[0];
    	
		for(int i = 0; i < all.length; i++)
		{
			publishProgress((int) ((i * 100.0 / (float) all.length)));
			try 
			{
				Thread.sleep(constants.thread_sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			AsyncTask<String, String, String> sa = new ping_async();
	    	sa.execute(all[i]);
		}
		return null;
	}

	protected void onProgressUpdate(Integer... progress) 
	{
		javasockets.updateProgressBar(progress[0]);
		if(progress[0] == 99)
		{
			javasockets.showResult("Discovered", javasockets.hosts_found + " hosts");
		}
	}
}
