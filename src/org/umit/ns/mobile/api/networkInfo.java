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

    WifiManager w;
    DhcpInfo d;
    SubnetUtils su;
    SubnetInfo si;

    public networkInfo(WifiManager w)
    {
        this.w = w;
        this.d = w.getDhcpInfo();
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
        return intToIp(d.dns1);
    }
    
    public String getDNS2()
    {
        return intToIp(d.dns2);
    }
    
    public String getSubnet()
    {
        return intToIp(d.gateway);
    }
    
    public int getLease()
    {
        return d.leaseDuration;
    }
    public String getServerAddres()
    {
        return intToIp(d.serverAddress);
    }
    
    public String getIp()
    {
        return intToIp(d.ipAddress);
    }
    
    public String[] getRange()
    {
        return si.getAllAddresses();
    }
    
}
