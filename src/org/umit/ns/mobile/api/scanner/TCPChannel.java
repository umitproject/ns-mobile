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
 * TCP Connect() scan using 
 * 
 * Incomplete
 * 
 * 
 */
package org.umit.ns.mobile.api.scanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.umit.ns.mobile.PortScanner;

import android.os.AsyncTask;

public class TCPChannel extends AsyncTask<String[], String, Void> {

    private Selector selector;
    private boolean select;

    public final static int OPEN = 0;
    public final static int CLOSED = 1;
    public final static int FILTERED = -1;
    public final static int UNREACHABLE = -2;
    public final static int TIMEOUT = -3;    
    
    @Override
    protected Void doInBackground(String[]... params) {
        
        String host = params[0][1];
        String[] ports = params[1];
        
        
        InetAddress ina = null;
        try {
            ina = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            //FIXME: Handle this exception.
            e.printStackTrace();
            return null;
        }
        portscan(ina, ports);
        return null;
    }
    
    protected void onProgressUpdate(String... params) {
        PortScanner.resultPublish(params[0] + "...");
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
    
    
    //Adapted from 
    //https://github.com/rorist/android-network-discovery/blob/master/src/info/lamatricexiste/network/AsyncPortscan.java
    //http://java.sun.com/developer/technicalArticles/releases/nio/
    private boolean portscan(InetAddress ina, String[] range)
    {
        select = true;
        try {
            selector = Selector.open();
            for(int i = 0; i < range.length; i++)
            {
                int port = Integer.parseInt(range[i]);
                SocketChannel socket = SocketChannel.open();
                socket.configureBlocking(false);
                socket.connect(new InetSocketAddress(ina, port));
                portdata data = new portdata();
                data.port = port;
                data.start = System.nanoTime();
                socket.register(selector, SelectionKey.OP_CONNECT, data);
            }
            
            while(select && selector.keys().size() > 0)
            {
                if(selector.select(300) > 0)
                {
                    synchronized (selector.selectedKeys()) {
                        
                        Iterator<SelectionKey> readyItor = selector.selectedKeys().iterator();
                        while(readyItor.hasNext()){
                            SelectionKey key = (SelectionKey) readyItor.next();
                            if(!key.isValid()) continue;
                            readyItor.remove();
                            portdata data = (portdata) key.attachment();
                            
                            if(key.isConnectable()) {
                                if(((SocketChannel) key.channel()).finishConnect()){
                                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                    data.state = OPEN; //OPEN
                                    //publishProgress(data.port);
                                }
                                else finishKey(key, OPEN, null);
                            }
                        }
                    }
                }
            }
            

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
        
    }
    
    private void finishKey(SelectionKey key, int state, String banner) {
        synchronized (key) {
            if(key == null || !key.isValid()){
                return;
            }
            closeChannel(key.channel());
            //portdata data = (portdata) key.attachment();
            //publishProgress(data.port, state, banner);
            key.attach(null);
            key.cancel();
            key = null;
        }
    }
    
    private void closeChannel(SelectableChannel channel) {
        if (channel instanceof SocketChannel) {
            Socket socket = ((SocketChannel) channel).socket();
            try{
                if (!socket.isInputShutdown()) socket.shutdownInput();
            } catch (IOException ex){
            }
            try{
                if (!socket.isOutputShutdown()) socket.shutdownOutput();
            } catch (IOException ex){
            }
            try{
                socket.close();
            } catch (IOException ex){
            }
        }
        try{
            channel.close();
        } catch (IOException ex){
        }
    }

    
    
    private static class portdata
    {
        int state = 0;
        int port;
        long start;
        int pass = 0;
    }
}

