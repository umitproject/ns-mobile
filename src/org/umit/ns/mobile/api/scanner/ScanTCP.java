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
 * Implements TCP Conenct() scan.
 * 
 * 
 */
package org.umit.ns.mobile.api.scanner;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.umit.ns.mobile.PortScanner;
import org.umit.ns.mobile.nsandroid;

import android.os.AsyncTask;

public class ScanTCP extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        
        String host = params[0];
        String port = params[1];
        
        publishProgress(host + ":" + port);
        if(portscan(host, port)) {
            return host + ":" + port;
        }
        else return "";
    }
    
    protected void onProgressUpdate(String... params) {
        nsandroid.resultPublish(params[0] + "...");
    }
    
    protected void onPostExecute(String hostport) {
        if(!hostport.equals("")) {
            String host = hostport.substring(0, hostport.indexOf(':'));
            String port = hostport.substring(hostport.indexOf(':')+1);
            PortScanner.addPort(host, port);
        }
        else 
            PortScanner.updateProgress();
    }

    private boolean portscan(String host, String port){

        try {
            Socket s = new Socket(host, Integer.parseInt(port));
            //s.connect(new InetSocketAddress(host, Integer.parseInt(port)), Constants.timeout);
            s.close();
        } catch (NoRouteToHostException e) {
            publishProgress("no route to host");
            return false;
        } catch (NumberFormatException e) {
            publishProgress("number format");
            return false;
        } catch (SocketTimeoutException e) {
            publishProgress("socket timeout");
            return false;
        } catch (ConnectException e) {
            e.printStackTrace();
            publishProgress("connect exception");
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            publishProgress("ioexception");
            return false;
        } 
        //  if got this far, its open
        return true;
    }
}