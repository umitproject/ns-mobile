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

package org.umit.ns.mobile;

import org.umit.ns.mobile.core.Scanning;

import android.os.AsyncTask;

public class PortScanner {
    public static int open = 0;
    int all = 0;
    int scannerMode = 0;
    int high = 0;
    int low = 0;
    int[] portsOpen = null;
    Integer[] allPorts = null;
    String host = null;
    boolean started = false;
    
    AsyncTask<Object[], String, Void> scan;
    
    public PortScanner(String host, Integer[] ports) {
        //verify host
        this.allPorts = ports;
        this.host = host;
    }
    
    public void start() {
        scan = new Scanning();
        Object[] arg = {(Object)scannerMode, (Object)host};
        started = true;
        scan.execute((Object[])allPorts, arg);
    }
    
    public void stop() {
        if(started == false) {
            //already stopped
        }
        
        started = false;
        scan.cancel(true);
    }
    
    public void reset() {
        
    }
    
    public int getMode() {
        return scannerMode;
    }
    
    public void setMode(int mode) {
        this.scannerMode = mode;
    }
    
    public void setLow(int low){
        this.low = low;
    }
    
    public void setHigh(int high) {
        this.high = high;
    }
    
    public int getLow() {
        return low;
    }
    
    public int getHigh() {
        return high;
    }
    
    public int[] getOpenPorts(){
        return portsOpen;
    }
    
    public int getOpenCount() {
        return open;
    }

    public static void addPort(String host, String port) {
        nsandroid.resultPublish(port + "found!");
    }

    public static void updateProgress() {
        
    }
}
