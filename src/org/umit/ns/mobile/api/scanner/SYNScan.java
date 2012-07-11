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
 * SYN Scan implementation by executing the scanner binary.
 * scanner binary is written in C and cross-compiled for Android
 * Find the source in /jni/main.c
 * 
 */

package org.umit.ns.mobile.api.scanner;

import org.umit.ns.mobile.PortScanner;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import android.os.AsyncTask;

public class SYNScan extends AsyncTask<String, String, String>{

    String ipAddress;
    String from;
    String to;
    
    @Override
    protected String doInBackground(String... params) {

        PortScanner.setStarted(true);
        ipAddress = params[0];
        from = params[1];
        to = params[2];
        if(cmdRun(ipAddress))
            return ipAddress;
        else return "";
    }
    
    protected void onPostExecute(String param) {
        PortScanner.setStarted(false);
    }
    
    protected void onProgressUpdate(String... params) {
        String str = params[0];
        PortScanner.resultPublish(str);
        
        if(str.charAt(0) < 48 && str.charAt(0) > 57 ) return;
        
        StringTokenizer st = new StringTokenizer(str, " ");
        int hostIsUp = 0;
        
        try{
            hostIsUp = Integer.parseInt(st.nextToken());
        } 
        catch(NumberFormatException e) {
            //PortScanner.resultPublish("Unable to parse String");
            return;
        }
        
        if(hostIsUp == 0) {
            PortScanner.resultPublish("Host is down. Aborting port scan.");
            PortScanner.setStarted(false);
            return;
        }
        
        String source_ip = st.nextToken();
        String source_port = st.nextToken();
        String packetRate = st.nextToken();
        String next = st.nextToken();
        String ports_open = null;
        String total_scanned = null;
        String total_found = null;
        
        if(next.contains(":"))
        {
            ports_open = next;
            total_scanned = st.nextToken();
            total_found = st.nextToken();
        }
        else 
        {
            total_scanned = next;
            total_found = st.nextToken();
        }

        while(st.hasMoreTokens()) {
            PortScanner.resultPublish(st.nextToken() + "\n");
        }
        
        PortScanner.resultPublish("\nSource IP: " + source_ip + "\nSource Port: "
                + source_port + "\nPacket Rate: " + packetRate + "\nPorts Open: "
                + ports_open + "\nTotal Scanned: " + total_scanned + "\nTotal Found: " + total_found);
        
        if(ports_open == null) {
            PortScanner.resultPublish("No open ports found!");
        }
        
        else {
            StringTokenizer ports = new StringTokenizer(ports_open, ":");
            while(ports.hasMoreTokens()) {
                String port = ports.nextToken();
                PortScanner.addPort(ipAddress, port);
                PortScanner.addToList(port);
            }
        }
        
        if(str.length() < 20) {
            PortScanner.resultPublish("Error in parsing output.");
            return;
        }
    }

    private boolean cmdRun(String ip) {
        
        //-N flag for parsable output
        //-s flag for SYN scan
        String cmd = "/data/local/scanner -N -s -h " + ip + " " + from + " " + to;  
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
            int read;
            char[] buffer = new char[1024];
            StringBuffer output = new StringBuffer();
            try{
                while ((read = in.read(buffer)) > 0) {
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
