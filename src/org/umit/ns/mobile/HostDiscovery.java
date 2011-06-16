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
 * This is the Controller class for Host Discovery. 
 * 
 */

package org.umit.ns.mobile;

import org.umit.ns.mobile.api.networkInfo;
import org.umit.ns.mobile.core.Discovery;

import android.os.AsyncTask;
import android.widget.Toast;

public class HostDiscovery {
    static int hosts = 0;
    static String[] discoveredHosts = null;
    static int countPossibleHosts = 0;
    static int totalHosts = 0;
    
    String[] possibleHosts= null;
    int discoveryMode = 0;
    static int progress = 0;
    String low = null;
    String high = null;
    AsyncTask<Object[], Integer, Void> hd = null;
    static boolean started = false;
    networkInfo ni;

    /**
     * 
     * @param w
     * 
     * @param ni
     */
    public HostDiscovery(networkInfo ni) {
        this.ni = ni;
        init();
    }
    
    public void init()
    {
        reset();
        if(ni==null) {
            String result = "Error in getting Network Information\n Make sure you are connected to atleast one network interface.";
            nsandroid.resultPublish(result);
            Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG);
        }
        else {
            countPossibleHosts = ni.getNodes();
            setTotalHosts(getMode());
            nsandroid.resultPublish(Integer.toString(totalHosts));
            possibleHosts = ni.getRange();
            discoveredHosts = new String[countPossibleHosts];
            low = possibleHosts[0];
            high = possibleHosts[countPossibleHosts-1];
        }
    }
    
    /**
     * @param mode
     * Sets the discovery_mode
     */
    public void setMode(int mode) {
        this.discoveryMode = mode;
        setTotalHosts(mode);
    }
    
    /**
     * @return discovery_mode
     */
    public int getMode() {
        return discoveryMode;
    }
    
    public String getLow()
    {
        return low;
    }
    
    public String getHigh()
    {
        return high;
    }

    public void start()
    {
        init();
        if(started == true)
        {
            String result = "Please wait for the current scan to finish or press stop.";
            Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG).show();
            return;
        }
        
        if(ni.isValid(low) && ni.isValid(high)) {
            possibleHosts = ni.getRange(low, high);
            countPossibleHosts = possibleHosts.length;
            
            if(possibleHosts != null) {
                String[] mode = {Integer.toString(getMode())};
                hd = new Discovery();
                started = true;
                hd.execute((Object[])possibleHosts, (Object[])mode);
            }
            else {
                String result = "Error in getting Network Information\n Make sure you are connected to atleast one network interface.";
                nsandroid.resultPublish(result);
                Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG);
            }
        }
        else{
            String result = "Invalid IP. Please re-enter";
            Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG).show();
            return;
        }

    }
    
    public void stop()
    {
        if(started == false) {
            String result = "Discovery not running";
            Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG).show();
            return;
        }
        
        hd.cancel(true);
        started = false;
        String result = "Host Discovery interrupted\nDiscovered " + hosts + " hosts.";
        nsandroid.resultPublish(result);
        Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG).show();    
    }
    
    public boolean isStarted(){
        return started;
    }

    public void reset() {
        hosts = 0;
        possibleHosts = null;
        countPossibleHosts = 0;
        discoveredHosts = null;
        low = null;
        high = null;
        hd = null;
        started = false;
        progress = 0;
    }
    
    /**
     * @param which
     * sets the number of possible nodes based on the discovery methods
     * modified possibleNodes
     */
    private void setTotalHosts(int which) {
        switch(which){
        case 0: totalHosts = countPossibleHosts; break;
        case 1: totalHosts = countPossibleHosts *2; break;
        case 2: totalHosts = countPossibleHosts *5; break;
        }
    }

    /**
     * @param ipaddress
     * If a host is discovered, this method is called
     * modifies discovered[], hosts
     */
    public static void addHosts(String ipaddress) {
        int flag = 0;
        for(int i=0; i<hosts; i++) {
            if(ipaddress.equals(discoveredHosts[i]))
                flag = 1;
        }
        
        if(flag == 0) {
            discoveredHosts[hosts] = ipaddress;
            nsandroid.resultPublish(ipaddress);
            nsandroid.addToList(ipaddress);
            updateProgress();
            hosts++;
        }
    }

    public static void updateProgress() {
        progress++;
        nsandroid.updateProgressBar((int)(progress*100.0/(float)totalHosts));
                
        if(progress==totalHosts) {
            String result = "Done Host Discovery\nFound " + hosts + " hosts.";
            started = false;
            nsandroid.resultPublish(result);
            nsandroid.resetProgressBar();
            Toast.makeText(nsandroid.defaultInstance, result, Toast.LENGTH_LONG).show();
        }
    }
    
    public static void publishHost(String ip){
        nsandroid.resultPublish(ip);
    }
}
