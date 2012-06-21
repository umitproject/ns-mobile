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
 */

package org.umit.ns.mobile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.*;
import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.api.XmlParser;
import org.umit.ns.mobile.api.shellUtils;
import org.umit.ns.mobile.model.FileManager;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class nmap extends Activity implements ScanCommunication {
    
    TextView cmd;
    static TextView results;
    static boolean started = false;
    static Button start;

    //---Binding---
    private Messenger msgService;
    private boolean mBound;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            msgService = new Messenger(service);
            mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            msgService = null;
            mBound = false;
        }
    };

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_SCAN_FINISHED:
                    Log.d("UmitScanner","ScanActivity:NOTIFY_SCAN_FINISHED");
                    Message messageService=Message.obtain(null,RQST_RESULTS);
                    try {
                        msgService.send(messageService);
                    } catch (RemoteException e) {
                        Log.d("UmitScanner",
                                "Caught Remote Exception while sending RQST_RESULTS from ScanActivity to ScanService :"+e.toString());
                    }
                    break;

                case RESP_RESULTS_OK:
                    String scanResults = ((Bundle)msg.obj).getString("ScanResults");
                    Log.v("nmap", scanResults);
                    FileManager.write("nmap", scanResults);
                    results.append("\n" + scanResults);
                    break;

                case RESP_RESULTS_ERR:
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());
    //--\Binding---

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nmap);
        
        start = (Button)findViewById(R.id.startNmap);
        start.setOnClickListener(nmapLoad);

        cmd = (TextView)findViewById(R.id.nmapcmd);
        
        results = (TextView)findViewById(R.id.nmapOutput);

        bindService( new Intent("org.umit.ns.mobile.service.ScanService"),
                serviceConnection, Context.BIND_AUTO_CREATE);

    }

    public OnClickListener nmapLoad = new OnClickListener() {
        public void onClick(View v) {
            if(mBound) {
                Message msg = Message.obtain(null,RQST_START_SCAN);
                msg.replyTo = mMessenger;
                Bundle bundle = new Bundle();
                bundle.putString("ScanArguments","./" + cmd.getText().toString() + " -oX nmap.xml");
                msg.obj = bundle;
                try {
                    msgService.send(msg);
                } catch (RemoteException e) {
                    Log.d("UmitScanner",
                            "Caught Remote Exception while sending from ScanActivity to ScanService :"+e.toString());
                }
                started = true;
            } else {
                Log.d("UmitScanner","nmap.nmapLoad():Service is not bound");
            }
            //nmap = new cmdLine();
            //nmap.execute("./" + cmd.getText().toString() + " -oX nmap.xml", "nmap");

        }
    };

    public static void onDone()
    {
        started = false;
        shellUtils.killProcess("./nmap");
        XmlParser xp = new XmlParser();
        String output = xp.parseXML("/data/local/nmap/share/nmap.xml");
        resultPublish(output);
    }

    /**
     * Static UI methods
     */
    public static void resultPublish(String string) {
        Log.v("nmap", string);
        FileManager.write("nmap", string);
        results.append("\n" + string);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { 
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.cmdmenu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.clear:
            clearLogs();
            return true;
        case R.id.logs:
            loadLogs();
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void loadLogs() {        
        Intent n = new Intent(nmap.this, LogsViewer.class);
        startActivityForResult(n, 0);
    }

    public void clearLogs() {
        results.setText("");
    }

}
