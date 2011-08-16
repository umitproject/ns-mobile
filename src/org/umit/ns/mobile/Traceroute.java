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

import org.umit.ns.mobile.api.cmdLine;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Traceroute extends Activity{
    
    TextView cmd;
    static TextView results;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.traceroute);
        
        Button start = (Button)findViewById(R.id.startTraceroute);
        start.setOnClickListener(tracerouteLoad);

        cmd = (TextView)findViewById(R.id.traceroutecmd);
        
        results = (TextView)findViewById(R.id.tracerouteOutput);
    }
    
    public OnClickListener tracerouteLoad = new OnClickListener() {
        public void onClick(View v) {
            //NmapLoader.load(cmd.getText().toString());
            AsyncTask<String, String, String> traceroute;
            traceroute = new cmdLine();
            traceroute.execute("/data/local/busybox " + cmd.getText().toString(), "traceroute");
        }
    };
    
    /**
     * Static UI methods
     */
    private static int line_count = 0;
    private static boolean isFull = false;
    public static void resultPublish(String string) {
        if(string == null) return;
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
}
