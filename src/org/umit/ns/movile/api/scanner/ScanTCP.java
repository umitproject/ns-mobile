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
package org.umit.ns.movile.api.scanner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.umit.ns.mobile.Constants;
import org.umit.ns.mobile.PortScanner;

import android.os.AsyncTask;

public class ScanTCP extends AsyncTask<String, String, String> {

    String host;
    String port;
    @Override
    protected String doInBackground(String... params) {
        
        host = params[0];
        port = params[1];
        
        if(portscan(host, port)) {
            return port;
        }
        else return "";
    }
    
    protected void onPublishProgress(String... params) {
        
    }
    
    protected void onPostExecute(String port) {
        if(!port.equals(""))
            PortScanner.addPort(host, port);
        else 
            PortScanner.updateProgress();
    }
    
    private boolean portscan(String host, String port){

        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, Integer.parseInt(port)), Constants.timeout);
            s.close();
        } catch (NoRouteToHostException e) {
            return false;
        } catch (NumberFormatException e) {
            return false;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return false;
        } 
    
        //  if got this far, its open
        return true;
    }

}
