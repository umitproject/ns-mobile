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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.umit.ns.mobile.core.Scanning;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class PortScanner extends Activity{

    public static int open = 0;
    int all = 0;
    int scannerMode = 0;
    int[] portsOpen = null;
    static int portsToScan = 0;
    String host = null;
    static boolean started = false;
    static int p = 0;
    static AsyncTask<Object[], String, Void> scan;
    
    static ProgressBar progress;
    static TextView results;
    static TextView from;
    static TextView to;
    TextView h;
    ArrayAdapter<CharSequence> adapter;
    Builder select;
    
    ListView lv;
    static SimpleAdapter sa;
    static List<HashMap<String, String>> fillMaps;
    TextView list_host;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.portscanner);
        
        lv = (ListView) findViewById(R.id.listView1);
        String[] f = new String[] {"host"};
        int[] t = new int[] { R.id.host };
        fillMaps = new ArrayList<HashMap<String, String>>();
        sa = new SimpleAdapter(this, fillMaps, R.layout.list_item, f, t);
        lv.setAdapter(sa);
        
        Bundle extras = getIntent().getExtras();
        host = extras.getString("host");
        h = (TextView)findViewById(R.id.hostd);
        h.setText(host);
        h.append(" Current Scan mode: " + getScanMode());
        
        select = new AlertDialog.Builder(this);
        adapter = ArrayAdapter.createFromResource(this, R.array.portscan_array, android.R.layout.simple_spinner_dropdown_item);
        
        results = (TextView)findViewById(R.id.results);
        progress = (ProgressBar)findViewById(R.id.progress);
        from = (TextView)findViewById(R.id.from);
        to = (TextView)findViewById(R.id.to);
        
        from.setText("1");
        to.setText("1024");
        
        portsToScan = 1024;
        
        Button mode = (Button)findViewById(R.id.modeSelect);
        mode.setOnClickListener(modeSelect);
        
        //Discover
        Button start = (Button)findViewById(R.id.scan);
        start.setOnClickListener(startScan);
        
        //Stop
        Button stop = (Button)findViewById(R.id.stop);
        stop.setOnClickListener(stopScan);
        
        //Info
        //Button info = (Button)findViewById(R.id.info);
        //info.setOnClickListener(networkInfo);
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
                    setMode(which);
                    h.setText(host + " Current Scan mode: " + getScanMode());                    
                    dialog.dismiss();
                }
            }).create().show();
        }
    };
    
    public OnClickListener stopScan = new OnClickListener() {
        public void onClick(View v) {
           stop();
        }
    };
    
    public OnClickListener startScan = new OnClickListener() {
        public void onClick(View v) {
            start();
        }
    };
    
    public String getScanMode()
    {
        String str = "";
        switch(scannerMode) {
        case 0: str = "TCP Connect()"; break;
        case 1: str = "UDP Scan"; break;
        case 2: str = "SYN Scan"; break;
        case 3: str = "FIN Scan"; break;
        }
        
        return str;
    }
    
    public void start() {
        if(started == true) {
            makeToast("Scan already running. Please wait.");
            return;
        }
        reset();
        started = true;
        portsToScan = getPortsToScan();
        Object[] arg = {(Object)scannerMode, (Object)host, (Object)from.getText().toString(), (Object)to.getText().toString()};
        try {
            scan = new Scanning();
            scan.execute(arg);
        }
        catch(IllegalStateException e)
        {
            scan.cancel(true);
            scan.execute(arg);
        }
    }
    
    public static void stop() {
        if(started == false) {
            makeToast("Scan not running.");
        }
        else {
            started = false;
            scan.cancel(true);
            makeToast("Discovered " + open + " ports.");
        }
    }
    
    public void reset() {
        portsToScan = 0;
        open = 0;
        
        resetProgressBar();
        resetList();
    }
    
    public int getPortsToScan() {
        int f = Integer.parseInt(from.getText().toString());
        int t = Integer.parseInt(to.getText().toString());
        return t-f+1;
    }
    
    public int getMode() {
        return scannerMode;
    }
    
    public void setMode(int mode) {
        this.scannerMode = mode;
    }
    
    public int[] getOpenPorts(){
        return portsOpen;
    }
    
    public int getOpenCount() {
        return open;
    }

    public static void addPort(String host, String port) {
        PortScanner.resultPublish(port + "found!");
    }

    public static void updateProgress() {
        p++;
        PortScanner.updateProgressBar((int)(p*100.0/(float)portsToScan));
                
        if(p==portsToScan) {
            String result = "Done Port Scanning\nFound " + open + " ports.";
            started = false;
            nsandroid.resultPublish(result);
            nsandroid.resetProgressBar();
            makeToast(result);
            stop();
        }
    }
    
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
    
    /**
     * @param str
     * Add IP to list
     */
    public static void addToList(String str) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("host", str);
        fillMaps.add(map);
        sa.notifyDataSetChanged();
        open++;
    }
    
    /**
     * Reset the list
     */
    public static void resetList() {
        fillMaps.clear();
        sa.notifyDataSetChanged();
    }
    
    /**
     * @param str
     * Shows a Toast
     */
    public static void makeToast(String str) {
        Toast.makeText(nsandroid.defaultInstance, str, Toast.LENGTH_LONG).show();
    }
    
    /**
    * @param l
    * Sets Progress
    */
   public static void updateProgressBar(int l) {
       progress.setProgress(l);
   }
   
   /**
    * Resets progress bar
    */
   public static void resetProgressBar() {
       progress.setProgress(0);
   }
   
   /**
    * completely fill progress bar
    */
   public void fillProgressBar() {
       resetProgressBar();
       updateProgressBar(100);
   }
}
