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

import org.umit.ns.mobile.nsandroid;
import org.umit.ns.movile.api.scanner.ScanTCP;
import org.umit.ns.movile.api.scanner.ScanUDP;

import android.os.AsyncTask;

public class Scanning extends AsyncTask<Object[], String, Void>{

    AsyncTask<String, String, String> tcp;
    AsyncTask<String, String, String> udp;
    int method;
    int total;
    
    
    @Override
    protected Void doInBackground(Object[]... params) {
        
        Integer[] ports = (Integer[])params[0];
        method = (Integer)params[1][0];
        String host = (String)params[1][1];

        publishProgress("Scanning " + host);
        switch(method) {
            case 0: speed(host, ports); break;
            case 1: insane(host, ports); break;
        }
        
        return null;
    }

    private void insane(String host, Integer[] ports) {
        //TODO
    }
    
    protected void onPublishProgress(String... params){
        nsandroid.resultPublish(params[0]);
    }

    
    private void speed(String host, Integer[] ports) {
        for(int i=0; i<ports.length; i++)
        {
            publishProgress("TCP" + ports[i]);
            scanTCP(host, Integer.toString(ports[i]));
            sleep(50);
            if(isCancelled()) {
                tcp.cancel(true);
                return;
            }
            scanUDP(host, Integer.toString(ports[i]));
            sleep(50);
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

}
