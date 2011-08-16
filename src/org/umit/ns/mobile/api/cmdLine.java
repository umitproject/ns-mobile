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
 */

package org.umit.ns.mobile.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.umit.ns.mobile.Traceroute;
import org.umit.ns.mobile.nmap;

import android.os.AsyncTask;

public class cmdLine extends AsyncTask<String, String, String> {

    String app;
    
    @Override
    protected String doInBackground(String... params) {
        String cmd = params[0];
        app = params[1];
        cmdRun(cmd);
        return " ";
    }
    
    protected void onPostExecute(String param) {
    }
    
    protected void onProgressUpdate(String... params) {
        if(app == "traceroute")
        {
            Traceroute.resultPublish(params[0]);
        }

        if(app == "nmap")
        {
            nmap.resultPublish(params[0]);
        }
    }   
    
    private boolean cmdRun(String cmd) {
        
        //String cmd = "/data/local/" + c + " -e /data/local/download/error_log.txt";
        publishProgress("Executing " + cmd);
        Process p;
        
        try{
            p = Runtime.getRuntime().exec("su");
            //p = Runtime.getRuntime().exec(cmd);

            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
            try {
                pOut.writeBytes(cmd + "\n");
                pOut.flush();
            } 
            catch (IOException e1) {
                e1.printStackTrace();
            }

            int read;
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            char[] buffer = new char[1024];
            StringBuffer output = new StringBuffer();
            try{
                while ((read = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                    publishProgress(output.toString());
                    output = new StringBuffer();
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
}
