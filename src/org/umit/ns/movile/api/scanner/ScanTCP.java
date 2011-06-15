package org.umit.ns.movile.api.scanner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;

import org.umit.ns.mobile.Constants;

import android.os.AsyncTask;

public class ScanTCP extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        
        String host = params[0];
        String port = params[1];
        
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, Integer.parseInt(port)), Constants.timeout);
            s.close();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        
        //TODO Add different port states
        
        return null;
    }
    
    protected void onPublishProgress(String result)
    {
        
    }

}
