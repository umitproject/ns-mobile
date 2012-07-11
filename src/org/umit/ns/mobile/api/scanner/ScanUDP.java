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

package org.umit.ns.mobile.api.scanner;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;

import org.umit.ns.mobile.PortScanner;
import android.os.AsyncTask;

public class ScanUDP extends AsyncTask<String, String, String> {
    @Override
    protected String doInBackground(String... params) {
        
        String host = params[0];
        String port = params[1];
        
        //publishProgress(host + ":" + port);
        if(portscan(host, port)) {
            return host + ":" + port;
        }
        else return "";
    }
    
    protected void onProgressUpdate(String... params) {
        PortScanner.resultPublish(params[0] + "...");
    }
    
    protected void onPostExecute(String hostport) {
        if(!hostport.equals("")) {
            String host = hostport.substring(0, hostport.indexOf(':'));
            String port = hostport.substring(hostport.indexOf(':')+1);
            PortScanner.addPort(host, port);
            PortScanner.addToList(port);
        }
        else 
            PortScanner.updateProgress();
    }
    
    private boolean portscan(String host, String port){
        //Adapted from JMap 
        //https://github.com/angad/jmap/blob/master/ScanUDP.java
        
        DatagramSocket ds;
        DatagramPacket dp;
        DatagramChannel dChannel;
        InetAddress IP;
        try {
            IP = InetAddress.getByName(host);
        } catch (UnknownHostException e1) {
            return false;
        }
        
        try{
            byte [] bytes = new byte[128];
            ds = new DatagramSocket();
            dp = new DatagramPacket(bytes, bytes.length, IP, Integer.parseInt(port));
            dChannel = DatagramChannel.open();
            dChannel.connect(new InetSocketAddress(IP, Integer.parseInt(port)));
            dChannel.configureBlocking(true);
            ds = dChannel.socket();
            ds.setSoTimeout(1000);
            ds.send(dp);
            dp = new DatagramPacket(bytes, bytes.length);
            Thread.sleep(1000);
            ds.receive(dp);

            //check datagram channel still connected
            if (!dChannel.isConnected() || !dChannel.isOpen()){
                ds.close();
                return false;
            }

            ds.disconnect();
            dChannel.disconnect();
            dChannel.close();
            ds.close();
        }
        catch(PortUnreachableException e){
            //publishProgress("port unreachable");
            return false;
        }
        catch(InterruptedIOException e){
            //publishProgress("Interrupted io");
            return false;
        }
        catch(IOException e){
            //publishProgress("IO exception");
            return false;
        }
        catch(Exception e){
            //publishProgress("exception");
            //e.printStackTrace();
            return false;
        }
        return true;
    }
}

