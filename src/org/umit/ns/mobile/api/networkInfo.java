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
 * Gets network information.
 * Uses WifiManager, DHCPInfo
 * Shell commands - ip, netcfg
 * 
 */

package org.umit.ns.mobile.api;

import org.umit.ns.mobile.nsandroid;
import org.umit.ns.mobile.api.SubnetUtils.SubnetInfo;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

/**
 * 
 * @author angadsg
 * Gets the network information 
 * 1. MAC address
 * 2. Subnet mask
 * 3. IP address
 * 4. Network Interface
 *
 */

public class networkInfo {

    WifiManager w = null;
    DhcpInfo d = null;
    SubnetUtils su = null;
    SubnetInfo si = null;
    
    String networkInterface;
    String ipAddress;
    String serverAddress;
    String subnet;
    
    boolean isWifi = false;

    public networkInfo(WifiManager w) {
        this.w = w;
        if(w.isWifiEnabled()) {
            this.d = w.getDhcpInfo();
            isWifi = true;
            
            networkInterface = "eth0";
            ipAddress = intToIp(d.ipAddress);
            serverAddress = intToIp(d.serverAddress);
            subnet = intToIp(d.netmask);
        }
        else {
            String output = shellUtils.runCommand("netcfg");
            for(int i=4; i<output.length(); i+=57)
            {
                //first 4 characters = null
                // next 9 char contain interface name
                // next 6 contain status - DOWN, UP
                // next 17 contain IP address
                // next 16 contain subnet address
                // next 10 contain hex
                
                String line = output.substring(i, i+57);
                if(line.substring(9,15).trim().equals("UP"))
                {
                    networkInterface = line.substring(0,9).trim();
                    ipAddress = line.substring(15,31).trim();
                    subnet = line.substring(31,47).trim();
                    //nsandroid.resultPublish(networkInterface + " " + ipAddress + " " + subnet);
                }
                
                /*
                nsandroid.resultPublish("interface: " + line.substring(0,9));
                nsandroid.resultPublish("status: " + line.substring(9,15));
                nsandroid.resultPublish("ipaddress: " + line.substring(15,31));
                nsandroid.resultPublish("subnet: " + line.substring(31,47));
                nsandroid.resultPublish("hex: " + line.substring(47,57));
                nsandroid.resultPublish(output.substring(i, i+57));
                */
            }
        }
        this.su = new SubnetUtils(getIp(), getSubnet());
        this.si = su.getInfo();
    }
    
    //converts integer to IP
    public String intToIp(int i) 
    {
        String t1 = ((i >> 24 ) & 0xFF ) + "";
        String t2 = ((i >> 16 ) & 0xFF) + ".";
        String t3 = ((i >> 8 ) & 0xFF) + ".";
        String t4 = ( i & 0xFF) + ".";
    
        return t4+t3+t2+t1;
    }
    
    public String getMACAddress()
    {
        return w.getConnectionInfo().getMacAddress();
    }
    
    public String getDNS1()
    {
        if(d != null)
            return intToIp(d.dns1);
        else return null;
    }
    
    public String getDNS2()
    {
        if(d != null)
            return intToIp(d.dns2);
        else return null;
    }
    
    public String getSubnet()
    {
        return subnet;
    }
    
    public int getLease()
    {
        if(d != null)
            return d.leaseDuration;
        else return 0;

    }
    public String getServerAddres()
    {
        return serverAddress;
    }
    
    public String getIp()
    {
        return ipAddress;
    }
    
    public String[] getRange()
    {
        if(si != null)
            return si.getAllAddresses();
        else return null;
    }
}
