/*
Android Network Scanner Umit Project
Copyright (C) 2011 Adriano Monteiro Marques

Author: Angad Singh <angad@angad.sg>

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

/**
 * @author angadsg
 * 
 * 
 */

package org.umit.ns.mobile.api.scanner;

import org.umit.ns.mobile.PortScanner;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import android.os.AsyncTask;

public class SYNScan extends AsyncTask<String, String, String>{

    String ipAddress;
    String from;
    String to;
    
    @Override
    protected String doInBackground(String... params) {

        ipAddress = params[0];
        from = params[1];
        to = params[2];
        if(cmdRun(ipAddress))
            return ipAddress;
        else return "";
    }
    
    protected void onPostExecute(String successIp) {
    }
    
    protected void onProgressUpdate(String... params) {
        PortScanner.resultPublish(params[0]);
    }

    private boolean cmdRun(String ip) {
        
        String cmd = "/data/local/synscanner " + ip + " " + from + " " + to; 
        publishProgress(cmd);
        Process p;
        
        try{
            //Gaining root access
            p = Runtime.getRuntime().exec("su");

            //Wait for a second to ensure root access
            //Superuser app may block root access
            try {
                Thread.sleep(100);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
                        
            try {
                pOut.writeBytes(cmd + "\n");
                //pOut.flush();
            } 
            catch (IOException e1) {
                e1.printStackTrace();
            }
                        
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            AsyncTask<BufferedReader, String, String> pt = new PrintingThread();
            pt.execute(in);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }    
}
