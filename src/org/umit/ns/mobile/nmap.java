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

import android.app.Activity;
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
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void clearLogs() {
        results.setText("");
    }

    public OnClickListener nmapLoad = new OnClickListener() {
        public void onClick(View v) {
            nmap = new cmdLine();
            nmap.execute("./" + cmd.getText().toString() + " -oX nmap.xml", "nmap");
            started = true;
            start.setText("Stop");
        }
    };
    
    public static void onDone()
    {
        start.setText("Start");
        started = false;
        XmlParser xp = new XmlParser();
        String output = xp.parseXML("/data/local/nmap.xml");
        resultPublish(output);
    }
    
    /**
     * Static UI methods
     */
//    private static int line_count = 0;
//    private static boolean isFull = false;
    public static void resultPublish(String string) {
//        if(string == null) return;
            Log.v("nsandroid", string);
//        if(line_count == 5 || isFull) {
//            String txt = results.getText().toString();
//            txt = txt.substring(txt.indexOf('\n') + 1);
//            results.setText(txt);
//            isFull=true;
//            line_count = 0;
//        }
//        line_count++;
        results.append("\n" + string);
    }
}
