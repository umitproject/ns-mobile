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
 * Implements various modes of Host Discovery
 * 1. Insane mode
 * 2. Normal mode
 * 3. Speed mode
 * 
 * In all modes, if ARP cache is available - uses that.
 * 
 * Insane - Uses all ping methods
 * Normal - isReachable, TCP Connect
 * Speed - isReachable
 * 
 * Implements Async Task for various modes.
 * 
 */

package org.umit.ns.mobile.core;

import org.umit.ns.mobile.api.discovery.*;
import org.umit.ns.mobile.Constants;

import android.os.AsyncTask;

public class Discovery extends AsyncTask<Object[], Integer, Void>{

    String[] range;
    int method;
    int total;
    
    AsyncTask<String, String, String> ir;
    AsyncTask<Void, String, String> arp;
    AsyncTask<String, String, String> sp;
    AsyncTask<String, String, String> tcp13;
    AsyncTask<String, String, String> udp7;
    AsyncTask<String, String, String> multiport;
    
    @Override
    protected Void doInBackground(Object[]... params) {
    
        range = (String[]) params[0];        
        method = Integer.parseInt(params[1][0].toString());
        
        switch(method) {
        case 0: 
            speed(range); 
            break;
        case 1: 
            normal(range);
            break;
        case 2: 
            insane(range);
            break;
        }
        return null;
    }
    
    /**
     * 
     * @param r
     * Range of the IP addresses to do the scanning.
     * Calls publishProgress.
     *  
     * @return void
     * 
     */
    
    
    /**
     * Insane mode - 
     * isReachable
     * ARP Scan
     * TCP Sockets
     * TCP 13
     * UDP 7
     * Shell 
     * 
     */
    private void insane(String[] r) {
        int i;
        for(i=0; i<r.length; i++)
        {
            isReachable(r[i]);
            sleep(50);
            if(isCancelled()) {
                ir.cancel(true);
                return;
            }
        }
        
        ARPScan();
        if(isCancelled()){
            arp.cancel(true);
            return;
        }
        
        for(i=0; i<r.length; i++)
        {
            tcp13(r[i]);
            if(isCancelled()) {
                tcp13.cancel(true);
                return;
            }
            sleep(50);
            udp7(r[i]);
            if(isCancelled()) {
                udp7.cancel(true);
                return;
            }
            sleep(50);
            multiport(r[i]);
            if(isCancelled()) {
                multiport.cancel(true);
                return;
            }
            sleep(50);
            shellping(r[i]);
            if(isCancelled()) {
                sp.cancel(true);
                return;
            }
            sleep(50);
        }
    }
    
    /**
     * Normal Mode
     * isReachable
     * TCP Multiport
     * 
     */
    private void normal(String[] r) {
        int i;
        for(i=0; i<r.length; i++)
        {
            isReachable(r[i]);
            if(isCancelled()) {
                ir.cancel(true);
                return;
            }
            sleep(50);
        }
        
        ARPScan();
        if(isCancelled()) {
            arp.cancel(true);
            return;
        }

        if(isCancelled()) return;
        for(i=0; i<r.length; i++)
        {
            multiport(r[i]);
            if(isCancelled()) {
                multiport.cancel(true);
                return;
            }
            sleep(50);
        }
    }
    
    /**
     * Speed mode 
     * isReachable
     * 
     */
    private void speed(String[] r) {
        int i;
        for(i=0; i<r.length; i++) {
            isReachable(r[i]);
            sleep(50);
            if(isCancelled()) {
                ir.cancel(true);
                return;
            }
        }
        ARPScan();
        if(isCancelled()) {
            arp.cancel(true);
            return;
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
    
    private void isReachable(String ip)
    {
        ir = new isReachable();
        ir.execute(ip, Integer.toString(Constants.timeout));   
    }
    
    private void ARPScan()
    {
        arp = new ARPScan();
        arp.execute();
    }
    
    private void shellping(String ip)
    {
        sp = new ShellPing();
        sp.execute(ip);
    }
    
    private void tcp13(String ip)
    {
        tcp13 = new TCP13();
        tcp13.execute(ip, Integer.toString(Constants.timeout));
    }
    
    private void udp7(String ip)
    {
        udp7 = new UDP7();
        udp7.execute(ip, Integer.toString(Constants.timeout));
    }
    
    private void multiport(String ip)
    {
        multiport = new TCPMultiPort();
        multiport.execute(ip, Integer.toString(Constants.timeout));        
    }
}
