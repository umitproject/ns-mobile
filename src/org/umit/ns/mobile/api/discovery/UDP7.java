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
 * Checks host availability by opening a Datagram channel on Port 7.
 * Runs a non-blocking asynchronous thread.
 * Updates the UI for Host Discovery.
 * 
 * @param params[0] - String ipAddress.
 * @param params[1] - int timeout
 * 
 * @return - updates the UI
 */

package org.umit.ns.mobile.api.discovery;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.umit.ns.mobile.nsandroid;

import android.os.AsyncTask;


public class UDP7 extends AsyncTask<String, String, String>{

    String ipAddress;
    int timeout;
    
    @Override
    protected String doInBackground(String... params) {

        ipAddress = params[0];
        timeout = Integer.parseInt(params[1]);
        
        if(udpEcho(ipAddress,timeout)) {
            return ipAddress;
        }
        else return "";
    }

    protected void onPostExecute(String successIp) {
        if(!successIp.equals(""))
            nsandroid.addHosts(successIp);
    }
    
    protected void onProgressUpdate(String... params) {
        nsandroid.resultPublish(params[0]);
    }
    
    private boolean udpEcho(String ip, int time) {
        boolean r = false;
        try {
            ByteBuffer msg = ByteBuffer.wrap("Hello".getBytes());
            ByteBuffer response = ByteBuffer.allocate("Hello".getBytes().length);
            
            InetSocketAddress sockaddr = new InetSocketAddress(ip, 7);
            DatagramChannel dgc = DatagramChannel.open();
            dgc.configureBlocking(false);
            dgc.connect(sockaddr);
            dgc.send(msg, sockaddr);
            Thread.sleep(time);
            dgc.receive(response);
            
            String received = new String(response.array());
            if(received.contains("Hello"))
                r = true;
            else r = false;
            }
        catch (Exception e){
            return false;
        }
        return r;
    }
}