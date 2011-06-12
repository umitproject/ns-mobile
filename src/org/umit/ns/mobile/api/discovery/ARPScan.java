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
 * Checks host availability using the isReachable Java method.
 * Runs a non-blocking asynchronous thread.
 * Updates the UI for Host Discovery.
 * 
 * @param params[0] - String ipAddress.
 * @param params[1] - int timeout
 * 
 * @return - updates the UI
 * 
 * @see isReachable
 */

package org.umit.ns.mobile.api.discovery;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.umit.ns.mobile.nsandroid;

import android.os.AsyncTask;

public class ARPScan extends AsyncTask<Void, String, String>{

    String ipAddress;
    @Override
    protected String doInBackground(Void... params)
    {   
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            try
            {
                while((line = br.readLine()) != null)
                {
                     String[] splitted = line.split(" +");
                     if (splitted != null && splitted.length >= 4) 
                     {
                         // Basic sanity check
                         String mac = splitted[3];
                         String ip = splitted[0];
                         if (mac.matches("..:..:..:..:..:..") && !mac.equals("00:00:00:00:00:00")) 
                         {
                             publishProgress(ip, mac);
                         }
                     }
                }
                br.close();
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
        }   
        return null;
    }

    protected void onProgressUpdate(String... params) {
        nsandroid.addHosts(params[0]);
    }

}
