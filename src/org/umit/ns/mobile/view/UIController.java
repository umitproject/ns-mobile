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
 * The UI Controller. Updates various UI Elements. 
 * Initializes the Event handlers and event listeners.
 * Returns Output strings to central controller.
 * 
 * Elements - 
 * progressBar
 * ipAddress
 * TTL
 * Buttons
 * debug window
 * list
 * 
 */

package org.umit.ns.mobile.view;

import org.umit.ns.mobile.R;
import org.umit.ns.mobile.nsandroid;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;

public class UIController {
    
    ArrayAdapter<CharSequence> adapter;
    Builder select;
    nsandroid obj;

    public UIController(nsandroid obj)
    {
        this.obj = obj;
        select = new AlertDialog.Builder(obj);
        adapter = ArrayAdapter.createFromResource(obj, R.array.discovery_array, android.R.layout.simple_spinner_dropdown_item);
    }
    
    
    public OnClickListener modeSelect = new OnClickListener() {
        public void onClick(View v) {
            select.setTitle(R.string.discovery_prompt)
            .setAdapter(adapter, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    obj.setMode(which);
                    dialog.dismiss();
                }
            }).create().show();
        }  
    };
    
    public OnClickListener startDiscovery = new OnClickListener() {
        public void onClick(View v) {
            obj.discoverHosts();
        }
    };
    



    public static void updateDiscovery(String successIp) {
        // TODO Auto-generated method stub
        
    }

    public static void updateProgressBar(Integer progress) {
        // TODO Auto-generated method stub
        
    }

    private static int line_count = 0;
    private static boolean isFull = false;
    public void resultPublish(String string) {
        Log.v("nsandroid", string);
        if(line_count == 10 || isFull)
        {
            String txt = obj.results.getText().toString();
            txt = txt.substring(txt.indexOf('\n') + 1);
            obj.results.setText(txt);
            isFull=true;
            line_count = 0;
        }
        line_count++;
        
        obj.results.append(string + "\n");
    }


}
