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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
/**
 * @author angadsg
 * 
 * The main controller. Initializes the UI.
 * Initializes the API for various functions.
 * Manages tasks and other threads.
 *
 */

public class nsandroid extends Activity {
    
    //UI
    static ProgressBar progress;
    static TextView results;
    static TextView from;
    static TextView to;

    //API Objects
    HostDiscovery hd;
    
    //Port scanning parameters
    public static int ports = 0;
    public int scanning_mode = 0;
    
        //General parameters
    networkInfo ni;
    ArrayAdapter<CharSequence> adapter;
    Builder select;
    
    //instance required for Toast
    public static nsandroid defaultInstance = null;
    
    //defaultInstance constructor
    public nsandroid()
    {
        defaultInstance = this;
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Initialize UI Elements
        select = new AlertDialog.Builder(this);
        adapter = ArrayAdapter.createFromResource(this, R.array.discovery_array, android.R.layout.simple_spinner_dropdown_item);
        results = (TextView)findViewById(R.id.results);
        progress = (ProgressBar)findViewById(R.id.progress);
        from = (TextView)findViewById(R.id.from);
        to = (TextView)findViewById(R.id.to);

        //Initialize API
        ni = new networkInfo((WifiManager) getSystemService(Context.WIFI_SERVICE));
        hd = new HostDiscovery(ni);
        from.setText(hd.getLow());
        to.setText(hd.getHigh());
        
        //Attach event handlers
        //modeSelect
        Button mode = (Button)findViewById(R.id.modeSelect);
        mode.setOnClickListener(modeSelect);
        
        //Discover
        Button discover = (Button)findViewById(R.id.discover);
        discover.setOnClickListener(discoverHosts);
        
        //Stop
        Button stop = (Button)findViewById(R.id.stop);
        stop.setOnClickListener(stopDiscovery);
        
        //Info
        Button info = (Button)findViewById(R.id.info);
        info.setOnClickListener(networkInfo);
    }
    
    
    /**
     * Resets the application
     * modifies ni, possibleNodes
     */
    public void resetApp() {
        hd.reset();
    }
    
    
    /**
     * Event handler for Mode Select Button
     * Shows a popup for selecting the mode
     * modifies discovery_mode
     */
    public OnClickListener modeSelect = new OnClickListener() {
        public void onClick(View v) {
            select.setTitle(R.string.discovery_prompt)
            .setAdapter(adapter, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    hd.setMode(which);
                    dialog.dismiss();
                }
            }).create().show();
        }
    };

    /**
     * Event Handler for Host Discovery button
     * 
     */
    public OnClickListener discoverHosts = new OnClickListener() {
        public void onClick(View v) {
            hd.start();
        }
    };
    
    /**
     * Event Handler for Stop Discovery button
     * modifies started
     */
    public OnClickListener stopDiscovery = new OnClickListener() {
        public void onClick(View v) {
            hd.stop();
        }
    };
            
    /**
     * Event handler for Network Info button
     */
    public OnClickListener networkInfo = new OnClickListener() {
        public void onClick(View v) {

            String info;
            if(ni==null)
            {
                info = "Cannot get information";
            }
            else {
                String networkInterface = ni.getInterface();
                String ip = ni.getIp();
                String subnet = ni.getSubnet();
                info = "Interface: " + networkInterface + "\nIP Address: " + ip + "\nSubnet: " + subnet;    
            }
            Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
        }
    };
    
    /**
     * Static UI methods
     */
    private static int line_count = 0;
    private static boolean isFull = false;
    public static void resultPublish(String string) {
        Log.v("nsandroid", string);
        if(line_count == 10 || isFull) {
            String txt = results.getText().toString();
            txt = txt.substring(txt.indexOf('\n') + 1);
            results.setText(txt);
            isFull=true;
            line_count = 0;
        }
        line_count++;
        results.append("\n" + string);
    }
    
    public static void updateProgressBar(int l) {
        progress.setProgress(l);
    }
    
    public static void resetProgressBar() {
        progress.setProgress(0);
    }
    
    public void fillProgressBar() {
        resetProgressBar();
        updateProgressBar(100);
    }
    
    public static void setFrom(String f) {
        from.setText(f);
    }
    
    public static void setTo(String t)
    {
        to.setText(t);
    }
}
