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
 * Checks host availability by opening a TCP socket on port 13. 
 * See RFC-867 Daytime Protocol.  
 * Runs a non-blocking asynchronous thread.
 * Updates the UI for Host Discovery.
 * 
 * @param params[0] - String ipAddress.
 * @param params[1] - int timeout
 * 
 * @return - updates the UI
 */

package org.umit.ns.mobile.api.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import org.umit.ns.mobile.HostDiscovery;

import android.os.AsyncTask;

public class TCP13 extends AsyncTask<String, String, String>{

    String ipAddress;
    int timeout;
    
    @Override
    protected String doInBackground(String... params) {

        ipAddress = params[0];
        timeout = Integer.parseInt(params[1]);
        
        if(tcpSocket(ipAddress, timeout)) {
            return ipAddress;
        }
        else return "";
    }
    
    protected void onPostExecute(String successIp) {
        if(!successIp.equals(""))
            HostDiscovery.addHosts(successIp);
        else  
            HostDiscovery.updateProgress();
    }
    
    protected void onProgressUpdate(String... params) {
        HostDiscovery.publishHost(params[0]);
    }


    private boolean tcpSocket(String ip, int time) {

        InetSocketAddress address;
        SocketChannel channel = null;
        
        try {
            address = new InetSocketAddress(InetAddress.getByName(ip), 13);
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(address);
                return channel.isConnected();
            } catch (IOException e) {
                return false;
            }
        } 
        catch (UnknownHostException e) {
            return false;
        }
    }
}
