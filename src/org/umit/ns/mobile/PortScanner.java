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

public class PortScanner {
    public static int open = 0;
    int all = 0;
    int scannerMode = 0;
    int high = 0;
    int low = 0;
    int[] portsOpen = null;
    int[] allPorts = null;
    
    public PortScanner() {
        init();
    }

    public void init() {
        
    }
    
    public void start() {
        
    }
    
    public void stop() {
        
    }
    
    public void addPorts() {
        
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
        // TODO Auto-generated method stub
        
    }

    public static void updateProgress() {
        // TODO Auto-generated method stub
        
    }
}
