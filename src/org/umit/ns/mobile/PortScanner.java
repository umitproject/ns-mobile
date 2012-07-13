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
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.umit.ns.mobile.core.Scanning;
import org.umit.ns.mobile.model.FileManager;
import org.umit.ns.mobile.model.PortScanDBAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class PortScanner extends Activity{

    static int open = 0;
    static int scannerMode = 0;
    static boolean started = false;
    static int p = 0;
    static AsyncTask<Object[], String, Void> scan;
    static int portsToScan = 0;

    static ProgressBar progress;
    static TextView from;
    static TextView to;
    static TextView target;
    TextView h;
    ArrayAdapter<CharSequence> adapter;
    Builder select;

    static String portsOpen = "";
    String host = null;
    int all = 0;
    
    ListView lv;
    static SimpleAdapter sa;
    static List<HashMap<String, String>> fillMaps;
    TextView list_host;
    
    PortScanDBAdapter portscandb;
    
    public PortScanner()
    {
        portscandb = new PortScanDBAdapter(nsandroid.defaultInstance);
        portscandb.open();
    }
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.portscanner);
        
        lv = (ListView) findViewById(R.id.listView1);
        String[] f = new String[] {"port"};
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
        
        progress = (ProgressBar)findViewById(R.id.progress);
        from = (TextView)findViewById(R.id.from);
        to = (TextView)findViewById(R.id.to);
        target = (TextView)findViewById(R.id.target);
        from.setText("1");
        to.setText("1024");
        target.setText(host);
        
        portsToScan = 1024;
                
        //Discover
        Button start = (Button)findViewById(R.id.scan);
        start.setOnClickListener(startScan);
        
        //Stop
        Button stop = (Button)findViewById(R.id.stop);
        stop.setOnClickListener(stopScan);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (portscandb != null) {
            portscandb.close();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.appmenu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.info:
            nsandroid.makeToast("Target: " + host);
            return true;
        case R.id.mode:
            showMode();
            return true;
        case R.id.nmap:
            nmapActivity();
            return true;
        case R.id.portscan:
            scanActivity();
            return true;
        case R.id.traceroute:
            tracerouteActivity();
            return true;
        case R.id.save:
            saveScan();
            return true;
        case R.id.reset:
            reset();
        case R.id.logs:
            loadLogs();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadLogs() {        
        Intent n = new Intent(PortScanner.this, LogsViewer.class);
        startActivityForResult(n, 0);
    }
    
    private void saveScan()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Save Port Scanning Results");
        alert.setMessage("Enter name");

        // Set an EditText view to get user input 
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          String name = input.getText().toString();
          String type = Integer.toString(scannerMode);
          String target = host;
          String range = from + "-" + to;
          String args = "";
          
          String open = portsOpen;
          
          String closed = "";
          String filtered = "";
          String protocol = "";
          String d_id = "";
          
          portscandb.save(name, type, target, range, args, open, closed, filtered, protocol, d_id);          
          }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            //  Canceled.
          }
        });

        alert.show();
    }
    
    private void tracerouteActivity() {
        Intent n = new Intent(this, Traceroute.class);
        startActivityForResult(n, 0);        
    }

    private void scanActivity() {
        Intent n = new Intent(this, PortScanner.class);
        n.putExtra("host", "0.0.0.0");
        startActivityForResult(n, 0);
    }

    private void nmapActivity() {
//        Intent n = new Intent(this, nmap.class);
//        startActivityForResult(n, 0);
    }

    /**
     * Event handler for Mode Select Button
     * Shows a popup for selecting the mode
     * modifies discovery_mode
     */
    public void showMode() {
        select.setTitle(R.string.discovery_prompt)
        .setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                setMode(which);
                h.setText(host + " Current Scan mode: " + getScanMode());                    
                dialog.dismiss();
            }
        }).create().show();
    }
    
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
        
        host = target.getText().toString();
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
        if(scannerMode == 2 || scannerMode == 3) {
            killProcess("/data/local/scanner");
            started = false;
            return;
        }
        
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
        started = false;
        resetProgressBar();
        resetList();
    }
    
    public int getPortsToScan() {
        int f = Integer.parseInt(from.getText().toString());
        int t = Integer.parseInt(to.getText().toString());
        return t-f+1;
    }
    
    public static void setStarted(boolean s)
    {
        started = s;
    }
    
    public int getMode() {
        return scannerMode;
    }
    
    public void setMode(int mode) {
        scannerMode = mode;
    }
    
    public int getOpenCount() {
        return open;
    }

    public static void addPort(String host, String port) {
        PortScanner.resultPublish(port + " found!");
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
    public static void resultPublish(String string) {
        Log.v("Scanner", string);
        FileManager.write("scanner", string);
    }
    
    /**
     * @param str
     * Add IP to list
     */
    public static void addToList(String str) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("port", str);
        fillMaps.add(map);
        sa.notifyDataSetChanged();
        portsOpen = portsOpen + "-" + str;
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
       p=0;
       progress.setProgress(0);
   }
   
   /**
    * completely fill progress bar
    */
   public void fillProgressBar() {
       resetProgressBar();
       updateProgressBar(100);
   }
   
   private static boolean killProcess(String path)
   {
       resultPublish("Killing " + path);
       Process p;
       StringBuffer output = new StringBuffer();
       
       //A very dirty method of killing the process
       try{
           p = Runtime.getRuntime().exec("su");
           
           DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
           try {
               pOut.writeBytes("ps | grep " + path + "\nexit\n");
               pOut.flush();
           } 
           catch (IOException e1) {
               e1.printStackTrace();
           }
           
           try {
               p.waitFor();
           } catch (InterruptedException e1) {
               e1.printStackTrace();
           }
           
           int read;
           BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
           char[] buffer = new char[1024];
           try{
               while ((read = reader.read(buffer)) > 0) {
                   output.append(buffer, 0, read);
                   resultPublish(output.toString());
                   break;
               }
           }
           catch(IOException e) {
               e.printStackTrace();
           }
       }
       catch(IOException e) {
           e.printStackTrace();
       }
       
       String pid = "";
       for(int i = 0; i<output.length(); i++)
       {
           //look for the process id
           if(output.charAt(i) > 47 && output.charAt(i) < 58)
           {
               pid = output.substring(i, i + output.substring(i).indexOf(' '));
               break;
           }
       }
       
       try{
           p = Runtime.getRuntime().exec("su");
//           p = Runtime.getRuntime().exec("ps | grep " + path);

           DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
           try {
               pOut.writeBytes("kill -9 " + pid + "\nexit\n");
               pOut.flush();
           } 
           catch (IOException e1) {
               e1.printStackTrace();
           }
           
           try {
               p.waitFor();
           } catch (InterruptedException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
           }
           
           int read;
           BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
           char[] buffer = new char[1024];
           try{
               while ((read = reader.read(buffer)) > 0) {
                   output.append(buffer, 0, read);
                   resultPublish(output.toString());
                   break;
                   //output = new StringBuffer();
               }
           }
           catch(IOException e) {
               e.printStackTrace();
           }
       }
       catch(IOException e) {
           e.printStackTrace();
       }
       return false;
   }
}
