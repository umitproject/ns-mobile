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
 * Implements port scanning
 * 
 * 3 methods
 * 1. TCP Connect - Java
 * 2. UDP Connect - Java
 * 3. TCP SYN - Native code
 * 
 * 2 Modes
 * 1. Connect mode - TCP and UDP connect
 * 2. Stealth mode - TCP SYN
 * 
 */

package org.umit.ns.mobile.core;

import org.umit.ns.mobile.Constants;
import org.umit.ns.mobile.PortScanner;
import org.umit.ns.mobile.api.scanner.FINScan;
import org.umit.ns.mobile.api.scanner.SYNScan;
import org.umit.ns.mobile.api.scanner.ScanTCP;
import org.umit.ns.mobile.api.scanner.ScanUDP;

import android.os.AsyncTask;

public class Scanning extends AsyncTask<Object[], String, Void>{

    AsyncTask<String, String, String> tcp;
    AsyncTask<String, String, String> udp;
    AsyncTask<String, String, String> syn;
    AsyncTask<String, String, String> fin;
    int method;
    int total;
    
    @Override
    protected Void doInBackground(Object[]... params) {
        
        method = (Integer)params[0][0];
        String host = (String)params[0][1];
        String from = (String)params[0][2];
        String to = (String)params[0][3];

        switch(method) {
            case 0: connectTCPScan(host, from, to); break;
            case 1: connectUDPScan(host, from, to); break;
            case 2: synScan(host, from, to); break;
            case 3: finScan(host, from, to); break;
        }
        
        return null;
    }

    private void synScan(String host, String from, String to) {
        scanSYN(host, from, to);
    }
    
    private void finScan(String host, String from, String to) {
        scanFIN(host, from, to);
    }

    protected void onProgressUpdate(String... params){
        PortScanner.resultPublish(params[0]);
    }
    
    private void connectTCPScan(String host, String from, String to) {
        int f = Integer.parseInt(from);
        int t = Integer.parseInt(to);
        
        for(int i=f; i<=t; i++)
        {
            scanTCP(host, Integer.toString(i));
            sleep(Constants.rateControl);
            if(isCancelled()) {
                udp.cancel(true);
                return;
            }
        }
    }
    
    private void connectUDPScan(String host, String from, String to) {
        int f = Integer.parseInt(from);
        int t = Integer.parseInt(to);
        
        for(int i=f; i<=t; i++)
        {
            scanUDP(host, Integer.toString(i));
            sleep(Constants.rateControl);
            if(isCancelled()) {
                udp.cancel(true);
                return;
            }
        }
    }
    
    private void sleep(int time)
    {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void scanTCP(String host, String port) {
        tcp = new ScanTCP();
        tcp.execute(host, port);
    }
    
    private void scanUDP(String host, String port) {
        udp = new ScanUDP();
        udp.execute(host, port);
    }    

    private void scanSYN(String host, String from, String to) {
        syn = new SYNScan();
        syn.execute(host, from, to);
    }
    
    private void scanFIN(String host, String from, String to) {
        syn = new FINScan();
        syn.execute(host, from, to);
    }

}
