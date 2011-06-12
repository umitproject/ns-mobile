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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
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
    
    public static int hosts = 0;
    public static String[] discovered;
    public static ProgressBar progress;
    public static TextView results;

    public int mode;
    
    networkInfo ni;
    ArrayAdapter<CharSequence> adapter;
    Builder select;
        
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        discovered = new String[254];
        
        select = new AlertDialog.Builder(this);
        adapter = ArrayAdapter.createFromResource(this, R.array.discovery_array, android.R.layout.simple_spinner_dropdown_item);
        
        progress = (ProgressBar)findViewById(R.id.progress);

        //Initialize API
        WifiManager w = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ni = new networkInfo(w);
        
        results = (TextView)findViewById(R.id.results);
        
        //Attach event handlers
        //modeSelect
        Button mode = (Button)findViewById(R.id.modeSelect);
        mode.setOnClickListener(modeSelect);
        
        //Discover
        Button discover = (Button)findViewById(R.id.discover);
        discover.setOnClickListener(discoverHosts);
        
        //Initialize API
    }
    
    //Set Discovery mode
    public void setMode(int mode) {
        this.mode = mode;
    }
    
    public int getMode() {
        return mode;
    }
    
    public void resetDiscovery()
    {
        hosts = 0;
        discovered = new String[254];
    }
    
    public OnClickListener modeSelect = new OnClickListener() {
        public void onClick(View v) {
            select.setTitle(R.string.discovery_prompt)
            .setAdapter(adapter, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setMode(which);
                    resetDiscovery();
                    dialog.dismiss();
                    nsandroid.resultPublish(Integer.toString(which));
                }
            }).create().show();
        }
    };

    public OnClickListener discoverHosts = new OnClickListener() {
        public void onClick(View v) {
            //Use networkInfo to get IP address details
            //Use SubnetUtils to convert IP addresses
            
            String[] range = ni.getRange();
            String[] mode = {Integer.toString(getMode())};
            
            AsyncTask<Object[], Integer, Void> hd = new hostDiscovery();
            hd.execute((Object[])range, (Object[])mode);
        }
    };
    

    //private static int line_count = 0;
    //private static boolean isFull = false;
    public static void resultPublish(String string) {
        Log.v("nsandroid", string);
        /*
        if(line_count == 10 || isFull) {
            String txt = results.getText().toString();
            txt = txt.substring(txt.indexOf('\n') + 1);
            results.setText(txt);
            isFull=true;
            line_count = 0;
        }
        line_count++;
        results.append(string + "\n");
        */
    }

    public static void updateProgressBar(int l) {
        progress.setProgress(l);
    }
    
    public static void resetProgressBar() {
        progress.setProgress(0);
    }
    
    public static void fillProgressBar() {
        resetProgressBar();
        updateProgressBar(100);
    }
    
    public static void addHosts(String ipaddress) {
        int flag = 0;
        for(int i=0; i<hosts; i++)
        {
            if(ipaddress.equals(discovered[i]))
                flag = 1;
        }
        
        if(flag == 0){
            discovered[hosts] = ipaddress;
            resultPublish(ipaddress);
            hosts++;
        }
    }
}
