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
 * Runs the shell command ping.
 * Parses the output.
 * 
 * @param params[0] - String ipAddress.
 * @param params[1] - int timeout
 * 
 * @return - updates the UI
 */

package org.umit.ns.mobile.api.discovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.umit.ns.mobile.HostDiscovery;
import org.umit.ns.mobile.nsandroid;

import android.os.AsyncTask;

public class ShellPing extends AsyncTask<String, String, String>{

    String ipAddress;
    
    @Override
    protected String doInBackground(String... params) {

        ipAddress = params[0];
        
        if(cmdPing(ipAddress))
            return ipAddress;
        else return "";
    }
    
    protected void onPostExecute(String successIp) {
        if(!successIp.equals(""))
            HostDiscovery.addHosts(successIp);
        else  
            HostDiscovery.updateProgress();
    }
    
    protected void onPublishProgress(String... params) {
        HostDiscovery.publishHost(params[0]);
    }

    private boolean cmdPing(String ip) {
        
        String pingCmd = "ping " + ip;
        Process p;
        
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
                if(parseOutput(inputLine))
                    return true;
            }
            in.close();
            p.destroy();
        }
        catch(IOException e)
        {
            return false;
        }
        return false;
    }
    
    public boolean parseOutput(String output) {
        //Parse output to check if ping successful
        if(output.contains("from"))
            return true;
        else return false;
    }
    
    protected void onProgressUpdate(String... params) {
        nsandroid.resultPublish(params[0]);
    }
    
}
