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

import org.umit.ns.mobile.api.XmlParser;
import org.umit.ns.mobile.api.cmdLine;
import org.umit.ns.mobile.api.shellUtils;
import org.umit.ns.mobile.model.FileManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class nmap extends Activity{
    
    TextView cmd;
    static TextView results;
    static boolean started = false;
    static AsyncTask<String, String, String> nmap;
    static Button start;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nmap);
        
        start = (Button)findViewById(R.id.startNmap);
        start.setOnClickListener(nmapLoad);

        cmd = (TextView)findViewById(R.id.nmapcmd);
        
        results = (TextView)findViewById(R.id.nmapOutput);
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

    public OnClickListener nmapLoad = new OnClickListener() {
        public void onClick(View v) {
            nmap = new cmdLine();
            nmap.execute("./" + cmd.getText().toString() + " -oX nmap.xml", "nmap");
            started = true;
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
}
