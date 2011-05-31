/*
Various methods of Android Ping
Copyright (C) 2011 Angad Singh
angad@angad.sg

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


package org.umit.android.javasockets;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class javasockets extends Activity {
    
	//yahoo's ip address
	public static String serverip = "67.195.160.76";
	public static TextView t;
	EditText ip;
   	Process p;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ip = (EditText)findViewById(R.id.ip);
        Editable host = ip.getText();
        serverip = host.toString();	
        
        String macaddr = "MAC Address : " + getMACaddr() + "\n";
        
        t = (TextView)findViewById(R.id.msg);
        t.setText(macaddr);
        
        //isReachable
        Button reachable = (Button)findViewById(R.id.ping_reachable);
        reachable.setOnClickListener(ping_reachable);
        
        //network interfaces
        Button getNetwork = (Button)findViewById(R.id.network_interfaces);
        getNetwork.setOnClickListener(network_interfaces);
        
        //Echo Port 7 Ping (Datagram channel)
        Button echoping = (Button)findViewById(R.id.echo_ping);
        echoping.setOnClickListener(echo_ping);
        
        //Ping port 13 (Socket Channel)
        Button sockping = (Button)findViewById(R.id.socket_ping);
        sockping.setOnClickListener(socket_ping);
                
        //Ping shell command
        Button pingshell = (Button)findViewById(R.id.command_ping);
        pingshell.setOnClickListener(command_ping);
               
        //C code ping
        Button socket_tcp = (Button)findViewById(R.id.tcp_socket);
        socket_tcp.setOnClickListener(tcp_socket);
        
        Button hosts = (Button)findViewById(R.id.wifi_info);
        hosts.setOnClickListener(wifi_info);
    }
    
    private String getMACaddr() 
    {
    	WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    	WifiInfo wifiInf = wifiMan.getConnectionInfo();
    	String macaddr = wifiInf.getMacAddress();
    	return macaddr;
    }

	//isReachable
    private void checkReachable(String address)
    {
    	try {
    		showResult("Pinging " + address, "");
        	InetAddress addr = InetAddress.getByName(address);
        	boolean reachable = addr.isReachable(10000);
        	showResult("isReachable", " " + reachable);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
    }
    
    //Socket Ping - Port 13
    private void ping_socket(String addr)
    {
    	showResult("Pinging " + addr, "");
    	InetSocketAddress address;
    	SocketChannel channel = null;
    	try {
			address = new InetSocketAddress(InetAddress.getByName(addr), 13);
			try {
				channel = SocketChannel.open();
				channel.configureBlocking(false);
				boolean connected = channel.connect(address);
				
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
								
				showResult("socket_ping", connected + "");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
    	catch (UnknownHostException e) {
			e.printStackTrace();
		}
    }    
    
    //Echo ping using datagram channel - port 7
    private void ping_echo(String address)
    {
    	try {
    		showResult("Pinging " + address, "");
    		ByteBuffer msg = ByteBuffer.wrap("Hello".getBytes());
    		ByteBuffer response = ByteBuffer.allocate("Hello".getBytes().length);
    		
    		InetSocketAddress sockaddr = new InetSocketAddress(address, 7);
    		DatagramChannel dgc = DatagramChannel.open();
    		dgc.configureBlocking(false);
    		dgc.connect(sockaddr);
    		dgc.send(msg, sockaddr);
    		Thread.sleep(5000);
    		dgc.receive(response);
    		
    		String received = new String(response.array());
			showResult("echo_ping", received);
    		}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }

    //ping using the shell command
    private void ping_shell(String address)
    {
    	showResult("Pinging " + address, "");
    	ping_thread t = new ping_thread(address);
    	t.run();
    }
    
    private void socket_tcp(String address)
    {
    	showResult("TCP Socket to ", address);
    	try{
    		Socket s = new Socket(address, 80);
    		if(s.isConnected())
    		{
    			showResult("socket_tcp", "connected");
    		}
    		else showResult("socket_tcp", "not connected");
    	}
    	
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    private void interfaces()
    {
    	try {
			for(Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();)
			{
				NetworkInterface i = list.nextElement();
				showResult("network_interfaces", "display name " + i.getDisplayName());
				
				for(Enumeration<InetAddress> addresses = i.getInetAddresses(); addresses.hasMoreElements();)
				{
					String address = addresses.nextElement().toString().substring(1);
					showResult("InetAddress", address);
					
					
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
    }
    
    private void getWifiInfo()
    {
    	WifiManager w = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	DhcpInfo d = w.getDhcpInfo();
    	String dns1, dns2, gateway, ipAddress, leaseDuration, netmask, serverAddress;
    	
    	dns1 = intToIp(d.dns1);
    	dns2 = intToIp(d.dns2);
    	gateway = intToIp(d.gateway);
    	ipAddress = intToIp(d.ipAddress);
    	leaseDuration = String.valueOf(d.leaseDuration);
    	netmask = intToIp(d.netmask);
    	serverAddress = intToIp(d.serverAddress);
    	
    	showResult("DNS1 ", dns1);
    	showResult("DNS2 ", dns2);
    	showResult("Gateway ", gateway);
    	showResult("ipAddress ", ipAddress);
    	showResult("lease Duration ", leaseDuration);
    	showResult("netmask ", netmask);
    	showResult("serverAddress ", serverAddress);
    }
    
    public String intToIp(int i) {

    String t1 = ((i >> 24 ) & 0xFF ) + "";
    String t2 = ((i >> 16 ) & 0xFF) + ".";
    String t3 = ((i >> 8 ) & 0xFF) + ".";
    String t4 = ( i & 0xFF) + ".";
    
    return t4+t3+t2+t1;
    
    }
    
    public String reverse(String str)
    {
    	StringBuffer sb = new StringBuffer(str);
    	return sb.reverse().toString();
    }
    
    //---------onClick Event Handlers-----------//
    private OnClickListener ping_reachable = new OnClickListener() {
        public void onClick(View v) {
            Editable host = ip.getText();
            serverip = host.toString();
        	checkReachable(serverip);
        }
    };
        
    private OnClickListener socket_ping = new OnClickListener() {
        public void onClick(View v) {
            Editable host = ip.getText();
            serverip = host.toString();
        	ping_socket(serverip);
        }
    };
        
    private OnClickListener network_interfaces = new OnClickListener() {
        public void onClick(View v) {
        	interfaces();
        }
    };
    
    private OnClickListener echo_ping = new OnClickListener() {
        public void onClick(View v) {
            Editable host = ip.getText();
            serverip = host.toString();

        	ping_echo(serverip);
        }
    };
    
    private OnClickListener command_ping = new OnClickListener() {
        public void onClick(View v) {
            Editable host = ip.getText();
            serverip = host.toString();

        	ping_shell(serverip);
        }
    };
        
    private OnClickListener tcp_socket = new OnClickListener() {
        public void onClick(View v) {
            Editable host = ip.getText();
            serverip = host.toString();
            socket_tcp(serverip);
        }
    };
    
    private OnClickListener wifi_info = new OnClickListener() {
        public void onClick(View v) {
        	getWifiInfo();
        }
    };
    
    private static int line_count = 0;
    private static boolean isFull = false;
    public static void showResult(String method, String msg)
    {
    	if(line_count == 10 || isFull)
    	{
    		String txt = t.getText().toString();
    		txt = txt.substring(txt.indexOf('\n') + 1);
    		t.setText(txt);
    		isFull=true;
    		line_count = 0;
    	}
    	line_count++;
    	
    	Log.e("JAVASOCKET TEST", method + " " + msg);
    	t.append(method + ": " + msg + "\n");
    }
}//class
