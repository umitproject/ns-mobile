/*
Various methods of Android Ping
Network Information, Host Discovery

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

import org.umit.android.javasockets.SubnetUtils.SubnetInfo;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class javasockets extends Activity {
    
	public static String serverip;
	public static int time_to_live;
	
	public static TextView t;
	EditText ip;
   	Process p;
   	public static ProgressBar progress;
   	
   	public static int hosts_found = 0;
	EditText ttl_t;
   	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);
        
        ip = (EditText)findViewById(R.id.ip);
        Editable host = ip.getText();
        serverip = host.toString();	
        
        ttl_t = (EditText)findViewById(R.id.ttl);
        Editable time = ttl_t.getText();
        time_to_live = Integer.parseInt(time.toString());
        
        String macaddr = "MAC Address : " + getMACaddr() + "\n";
        
        t = (TextView)findViewById(R.id.msg);
        t.setText(macaddr);
        
        progress = (ProgressBar)findViewById(R.id.progress);
        
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
        
        Button wifi = (Button)findViewById(R.id.wifi_info);
        wifi.setOnClickListener(wifi_info);
        
        Button hosts = (Button)findViewById(R.id.host_discovery);
        hosts.setOnClickListener(discovery);
        
        Button arp = (Button)findViewById(R.id.read_arp);
        arp.setOnClickListener(read_arp);
    }
    
    public static void updateProgressBar(int l)
    {
    	progress.setProgress(l);
    }
    
    public static void resetProgressBar()
    {
    	progress.setProgress(0);
    }
    
    public static void progressBar_isFull()
    {
    	if(progress.getProgress() > 99)
        	javasockets.showResult("Discovered", javasockets.hosts_found + " hosts");
    }
    
    public static void fillProgressBar()
    {
    	resetProgressBar();
    	updateProgressBar(100);
    }
    
    //Gets WIFI MAC address
    private String getMACaddr() 
    {
    	WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    	WifiInfo wifiInf = wifiMan.getConnectionInfo();
    	String macaddr = wifiInf.getMacAddress();
    	return macaddr;
    }

	//isReachable
    public static boolean checkReachable(String address)
    {
    	boolean reachable = false;
    	try {
        	InetAddress addr = InetAddress.getByName(address);
        	reachable = addr.isReachable(time_to_live);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
		return reachable;
    }

    //Socket Ping - Port 13
    public static boolean ping_socket(String addr)
    {
    	InetSocketAddress address;
    	SocketChannel channel = null;
    	boolean connected = false;
    	
    	try {
			address = new InetSocketAddress(InetAddress.getByName(addr), 13);
			try {
				channel = SocketChannel.open();
				channel.configureBlocking(false);
				connected = channel.connect(address);
				
				try {
					Thread.sleep(time_to_live);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
    	catch (UnknownHostException e) {
			e.printStackTrace();
		}
    	return connected;
    }
    
    //Echo ping using datagram channel - port 7
    public static boolean ping_echo(String address)
    {
    	boolean r = false;
    	try {
    		ByteBuffer msg = ByteBuffer.wrap("Hello".getBytes());
    		ByteBuffer response = ByteBuffer.allocate("Hello".getBytes().length);
    		
    		InetSocketAddress sockaddr = new InetSocketAddress(address, 7);
    		DatagramChannel dgc = DatagramChannel.open();
    		dgc.configureBlocking(false);
    		dgc.connect(sockaddr);
    		dgc.send(msg, sockaddr);
    		Thread.sleep(time_to_live);
    		dgc.receive(response);
    		
    		String received = new String(response.array());
    		if(received == "")
    			r = false;
    		else r = true;
    		}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	return r;
    }

    //ping using the shell command
    public static void ping_shell(String address)
    {
    	ping_thread t = new ping_thread(address);
    	t.run();
    }
    
    //TCP Connect test on multiple ports
    public static boolean socket_tcp(String address)
    {    	
    	boolean connected = false;
    
    	for(int i = 0; i < ports.port.length; i++)
    	{
    		try
    		{
    			Socket s = new Socket();
    			s.bind(null);
    			s.connect(new InetSocketAddress(address, ports.port[i]), time_to_live);
    			connected = s.isConnected();
    			s.close();
    		}
    		catch(Exception e){
    			e.printStackTrace();
    		}
    		if(connected) return connected;
    	}
    	return connected;
    }
    
    //get network interface information
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
    
    //get dhcp wifi information
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
    	
    	SubnetUtils su = new SubnetUtils(getIP(), getNetmask());
    	SubnetInfo si = su.getInfo();
    	
    	showResult("High Address", si.getHighAddress());
    	showResult("Low Address", si.getLowAddress());
    }
    
    //converts integer to IP
    public String intToIp(int i) 
    {
    	String t1 = ((i >> 24 ) & 0xFF ) + "";
    	String t2 = ((i >> 16 ) & 0xFF) + ".";
    	String t3 = ((i >> 8 ) & 0xFF) + ".";
    	String t4 = ( i & 0xFF) + ".";
    
    	return t4+t3+t2+t1;
    }
    
    //reverses a string
    public String reverse(String str)
    {
    	StringBuffer sb = new StringBuffer(str);
    	return sb.reverse().toString();
    }
    
    private String getNetmask()
    {
    	WifiManager w = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	DhcpInfo d = w.getDhcpInfo();
    	return intToIp(d.netmask);	
    }
    
    private String getIP()
    {
    	WifiManager w = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	DhcpInfo d = w.getDhcpInfo();
    	return intToIp(d.ipAddress);
    }
    
    private void host_discovery()
    {
    	hosts_found = 0;
    	SubnetUtils su = new SubnetUtils(getIP(), getNetmask());
    	SubnetInfo si = su.getInfo();
    	String[] all = si.getAllAddresses();
    	
    	showResult("Scanning ", all.length + " hosts...");
    	scan_all_async(all);
    }
    
    private void scan_all_async(String[] all)
    {
    	AsyncTask<Object[], Integer, Void> sa = new scan_async();
    	sa.execute((Object[])all);
    }
    
    //Host discovery using arp cache at /proc/net/arp
    public static void read_arp()
    {
    	AsyncTask<Void, String, Integer> aa = new arp_async();
    	aa.execute();
    }
    
    	
    //---------onClick Event Handlers-----------//
    private OnClickListener ping_reachable = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	Editable host = ip.getText();
            serverip = host.toString();
    		showResult("Pinging " + serverip, "");
    		boolean success = checkReachable(serverip);
    		showResult("isReachable ", success + "");
        }
    };
        
    private OnClickListener socket_ping = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	Editable host = ip.getText();
            serverip = host.toString();
            showResult("Pinging " + serverip, "");
        	boolean success = ping_socket(serverip);
        	showResult("Socket Ping", success + "");
        }
    };
        
    private OnClickListener network_interfaces = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	interfaces();
        }
    };
    
    private OnClickListener echo_ping = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	Editable host = ip.getText();
            serverip = host.toString();
            showResult("Pinging " + serverip, "");
            boolean success = ping_echo(serverip);
            showResult("Echo ping ", success + "");
        }
    };
    
    private OnClickListener command_ping = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	Editable host = ip.getText();
            serverip = host.toString();
            showResult("Pinging " + serverip, "");
        	ping_shell(serverip);
        }
    };
        
    private OnClickListener tcp_socket = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	Editable host = ip.getText();
            serverip = host.toString();
            showResult("Pinging " + serverip, "");
        	boolean success = socket_tcp(serverip);
        	showResult("TCP Socket", success + "");
        }
    };
    
    private OnClickListener wifi_info = new OnClickListener() {
        public void onClick(View v) {
        	resetProgressBar();
        	getWifiInfo();
        }
    };
    
    private OnClickListener discovery = new OnClickListener() {
        public void onClick(View v) {
        	host_discovery();
        }
    };
    
    private OnClickListener read_arp = new OnClickListener() {
        public void onClick(View v) {
        	read_arp();
        }
    };
    private static int line_count = 0;
    private static boolean isFull = false;
    public static void showResult(String method, String msg)
    {
    	if(line_count == constants.debug_lines || isFull)
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
