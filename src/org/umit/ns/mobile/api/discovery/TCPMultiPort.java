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
 * Checks host availability by opening TCP sockets on common ports. 
 * Runs a non-blocking asynchronous thread.
 * Updates the UI for Host Discovery.
 * 
 * @param params[0] - String ipAddress.
 * @param params[1] - int timeout
 * 
 * @return - updates the UI
 * 
 * @see isReachable
 */

package org.umit.ns.mobile.api.discovery;
import org.umit.ns.mobile.*;

import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.AsyncTask;

public class TCPMultiPort extends AsyncTask<String, String, String>{

    String ipAddress = "";
    int timeout;
    
    @Override
    protected String doInBackground(String... params) {

        ipAddress = params[0];
        timeout = Integer.parseInt(params[1]);

        if(TCPSocket(ipAddress,timeout)) {
            return ipAddress;
        }
        else return "";
    }
    
    
    private boolean TCPSocket(String ip, int time) {
        boolean connected = false;
        
        for(int i = 0; i < Constants.TCPport.length; i++)
        {
            try {
                Socket s = new Socket();
                s.bind(null);
                s.connect(new InetSocketAddress(ip, Constants.TCPport[i]));
                connected = s.isConnected();
                //s.close();
                if(connected) return connected;
            } 
            catch(Exception e) {
                return false;
            }
        }
        return connected;
    }

    protected void onPostExecute(String successIp) {
        if(!successIp.equals(""))
            nsandroid.addHosts(successIp);
    }
    
    protected void onProgressUpdate(String... params){
        nsandroid.resultPublish(params[0]);
    }
    
}