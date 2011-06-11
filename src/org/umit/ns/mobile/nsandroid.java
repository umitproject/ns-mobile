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

import org.umit.ns.mobile.api.networkInfo;
import org.umit.ns.mobile.core.hostDiscovery;
import org.umit.ns.mobile.view.UIController;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author angadsg
 * 
 * The main controller. Initializes the UI.
 * Initializes the API for various functions.
 * Manages tasks and other threads.
 *
 */

public class nsandroid extends Activity {
    
    public static int hosts;
    public int mode;
    
    networkInfo ni;
    public TextView results;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Initialize UI
        UIController ui = new UIController(this);
        
        //Initialize API
        WifiManager w = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ni = new networkInfo(w);
        
        results = (TextView)findViewById(R.id.results);
        
        //Attach event handlers
        //modeSelect
        Button mode = (Button)findViewById(R.id.modeSelect);
        mode.setOnClickListener(ui.modeSelect);
        
        //Discover
        Button discover = (Button)findViewById(R.id.discover);
        discover.setOnClickListener(ui.startDiscovery);
        
        //Initialize API
    }
    
    //Set Discovery mode
    public void setMode(int mode)
    {
        this.mode = mode;
    }
    
    public int getMode()
    {
        return mode;
    }

    public void discoverHosts() {
        //Use networkInfo to get IP address details
        //Use SubnetUtils to convert IP addresses
        
        
        
        String[] range = ni.getRange();
        String[] mode = {Integer.toString(getMode())};
        
        AsyncTask<Object[], Integer, Void> hd = new hostDiscovery();
        hd.execute((Object[])range, (Object[])mode);
    }
    
}
