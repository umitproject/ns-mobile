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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.umit.ns.mobile.api.networkInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
/**
 * @author angadsg
 * 
 * The main controller. Initializes the UI.
 * Initializes the API for various functions.
 *
 */

public class nsandroid extends Activity {
    
    //UI
    static ProgressBar progress;
    static TextView results;
    static TextView from;
    static TextView to;
    ArrayAdapter<CharSequence> adapter;
    Builder select;
    ListView lv;
    static SimpleAdapter sa;
    static List<HashMap<String, String>> fillMaps;
    TextView list_host;
    
    //API Objects
    networkInfo ni;
    HostDiscovery hd;
    PortScanner ps;
    
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
        //setting up list View
        lv = (ListView) findViewById(R.id.listView1);
        String[] f = new String[] {"host"};
        int[] t = new int[] { R.id.host };
        fillMaps = new ArrayList<HashMap<String, String>>();
        sa = new SimpleAdapter(this, fillMaps, R.layout.list_item, f, t);
        lv.setAdapter(sa);
        lv.setOnItemClickListener(startPortScan);
        
        //Database
        
        //setting up mode selection popup
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
        
        setupNative();
        
        //Attach event handlers
        //Discover
        Button discover = (Button)findViewById(R.id.discover);
        discover.setOnClickListener(discoverHosts);
        
        //Stop
        Button stop = (Button)findViewById(R.id.stop);
        stop.setOnClickListener(stopDiscovery);
    }
    
    
    public void setupNative()
    {
        //Setting up libraries and native binaries
        try {
            Runtime.getRuntime().exec("su -c 'chmod 777 /data/local'");
        } catch (IOException e1) {
            nsandroid.resultPublish(e1.getMessage());
            e1.printStackTrace();
        }
        
        nsandroid.resultPublish("Copying Busybox to /data/local/...");
        CopyNative("/data/local/busybox", R.raw.busybox);
        
        nsandroid.resultPublish("Copying PortScanner native to /data/local/...");
        CopyNative("/data/local/scanner", R.raw.scanner);
        
        nsandroid.resultPublish("Copying nmap to /data/local/...");
        CopyNative("/data/local/nmap", R.raw.nmap);
        
        nsandroid.resultPublish("Trying to mount /system/lib/ as read-write partition");
        try {
            Runtime.getRuntime().exec("su -c 'mount -o rw,remount -t yaffs2 /dev/block/mtdblock3 | chmod 777 /system/lib/'");
        } catch (IOException e) {
            nsandroid.resultPublish(e.getMessage());
            e.printStackTrace();
        }
        
        nsandroid.resultPublish("Copying libcrypto, libgif, libltdl, libnet, libpcre, libssh, libssl libraries to /system/lib...");
        CopyNative("/system/lib/libcrypto.so", R.raw.libcrypto);
        CopyNative("/system/lib/libgif.so", R.raw.libgif);
        CopyNative("/system/lib/libltdl.so", R.raw.libltdl);
        CopyNative("/system/lib/libnet.so", R.raw.libnet);
        CopyNative("/system/lib/libpcre.so", R.raw.libpcre);
        CopyNative("/system/lib/libssh.so", R.raw.libssh);
        CopyNative("/system/lib/libssl.so", R.raw.libssl);
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
            showInfo();
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
            saveDiscovery();
            return true;
        case R.id.reset:
            resetApp();
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void saveDiscovery()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Save Host Discovery Results");
        alert.setMessage("Enter name");

        // Set an EditText view to get user input 
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          String name = input.getText().toString();
          hd.saveDiscovery(name);
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
        Intent n = new Intent(nsandroid.this, Traceroute.class);
        startActivityForResult(n, 0);        
    }

    private void scanActivity() {
        Intent n = new Intent(nsandroid.this, PortScanner.class);
        n.putExtra("host", "0.0.0.0");
        startActivityForResult(n, 0);
    }

    private void nmapActivity() {
        Intent n = new Intent(nsandroid.this, nmap.class);
        startActivityForResult(n, 0);        
    }

    /**
     * Copies the native binary from resource to path
     * 
     * @param path
     * path to where the native binary needs to be copied
     * 
     * @param resource
     * integer value of the resource (e.g. R.raw.synscanner)
     * 
     */
    protected void CopyNative(String path, int resource) {
        InputStream setdbStream = getResources().openRawResource(resource);
        try {
            byte[] bytes = new byte[setdbStream.available()];
            DataInputStream dis = new DataInputStream(setdbStream);
            dis.readFully(bytes);   
            FileOutputStream setdbOutStream = new FileOutputStream(path);
            setdbOutStream.write(bytes);
            setdbOutStream.close();

            //Set executable permissions
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("chmod 755 " + path + "\n");
            os.writeBytes("exit\n");
            os.flush();
        }
        
        catch (Exception e) {
            nsandroid.resultPublish("Unable to Copy native binary " + e.getMessage());
          return;
        }
      }
        
    /**
     * Extracts from a zip file. Unused method.
     * 
     * @param path
     * @param resource
     * 
     * 
     */
    protected void extractFromZip(String path, int resource) {
        try {
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = new ZipInputStream(getResources().openRawResource(resource));
            ZipEntry zipentry;

            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) { 
                String entryName = zipentry.getName();
                nsandroid.resultPublish("Extracting " + entryName);
                
                int n;
                FileOutputStream fileoutputstream;
                File newFile = new File(entryName);
                String directory = newFile.getParent();
                
                if(directory == null) {
                    if(newFile.isDirectory())
                        break;
                }
                
                fileoutputstream = new FileOutputStream(path + entryName);
                while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
                    fileoutputstream.write(buf, 0, n);

                fileoutputstream.close(); 
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();
            }
            zipinputstream.close();
        }
        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Resets the application
     * modifies ni, possibleNodes
     */
    public void resetApp() {
        ni = new networkInfo((WifiManager) getSystemService(Context.WIFI_SERVICE));
        hd = new HostDiscovery(ni);
        from.setText(hd.getLow());
        to.setText(hd.getHigh());
        resetProgressBar();
    }
    
    
    /**
     * Event handler for Mode Select Button
     * Shows a popup for selecting the mode
     * modifies discovery_mode
     */

    public void showMode() {
        select.setTitle(R.string.discovery_prompt)
            .setAdapter(adapter, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    hd.setMode(which);
                    dialog.dismiss();
                }
            }).create().show();
    }

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
    public void showInfo() {
            String info;
            if(ni == null) {
                info = "Cannot get information";
            }
            else {
                resetApp();
                String networkInterface = ni.getInterface();
                String ip = ni.getIp();
                String subnet = ni.getSubnet();
                info = "Interface: " + networkInterface + "\nIP Address: " + ip + "\nSubnet: " + subnet;    
            }
            makeToast(info);
    }
    

    /**
     * Event Listener of listItem click
     * Starts port scan
     */
    public OnItemClickListener startPortScan = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
            String host = ((TextView)arg1).getText().toString();
            Intent ps = new Intent(arg0.getContext(), PortScanner.class);
            ps.putExtra("host", host);
            startActivityForResult(ps, 0);
        }
    };

    /**
     * Static UI methods
     */
    private static int line_count = 0;
    private static boolean isFull = false;
    public static void resultPublish(String string) {
        Log.v("nsandroid", string);
        if(line_count == 5 || isFull) {
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
    
    /**
     * set the From IP address in Text box
     */
    public static void setFrom(String f) {
        from.setText(f);
    }
    
    /**
     * @param t
     * Set the To IP address
     */
    public static void setTo(String t) {
        to.setText(t);
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
}
