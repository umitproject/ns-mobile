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
 * 
 * Implements various modes of Host Discovery
 * 1. Insane mode
 * 2. Normal mode
 * 3. Speed mode
 * 
 * In all modes, if ARP cache is available - uses that.
 * 
 * Insane - Uses all ping methods
 * Normal - isReachable, TCP Connect
 * Speed - isReachable
 * 
 * Implements Async Task for various modes.
 * 
 */

package org.umit.ns.mobile.core;

import org.umit.ns.mobile.nsandroid;
import org.umit.ns.mobile.view.UIController;

import android.os.AsyncTask;

public class hostDiscovery extends AsyncTask<Object[], Integer, Void>{

    String[] range;
    int method;
    @Override
    protected Void doInBackground(Object[]... params) {
    
        range = (String[]) params[0];
        method = (Integer) params[1][0];
        
        switch(method) {
        case 0: speed(range); break;
        case 1: normal(range); break;
        case 2: insane(range); break;
        }
        return null;
    }
    
    
    /**
     * 
     * 
     * @param r
     * Range of the IP addresses to do the scanning.
     * Calls publishProgress.
     *  
     * @return void
     * 
     */
    
    private void insane(String[] r) {
        
    }
    
    private void normal(String[] r) {
        
    }
    
    private void speed(String[] r) {
        
    }
    
    protected void onProgressUpdate(Integer... progress) 
    {
        UIController.updateProgressBar(progress[0]);
        nsandroid.hosts++;
        
        if(progress[0] > 99){
            UIController.resultDiscovery("Discovered " + nsandroid.hosts + " hosts");
        }
    }
    

}
