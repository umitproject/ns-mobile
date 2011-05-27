package org.umit.android.javasockets;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;

import android.app.Activity;
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
        
        t = (TextView)findViewById(R.id.msg);
        t.setText("");
        
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
    }
    
    //isReachable
    private void checkReachable(String address)
    {
    	try {
    		showResult("Pinging " + address, "");
        	InetAddress addr = InetAddress.getByName(address);
        	boolean reachable = addr.isReachable(2000);
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
				if(connected) {
					channel.close();
					Log.e("JAVASOCKET TEST socket_ping", "connected");	
					showResult("socket_ping", "false");
				}
				else showResult("socket_ping", "not connected");
				
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
    		Thread.sleep(1000);
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
        	try {
                Editable host = ip.getText();
                serverip = host.toString();

        		for(Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();){
        			NetworkInterface i = list.nextElement();
        			//Log.e("JAVASOCKET TEST network_interfaces", "display name " + i.getDisplayName());
        			showResult("network_interfaces", "display name " + i.getDisplayName());
        			for(Enumeration<InetAddress> addresses = i.getInetAddresses(); addresses.hasMoreElements();)
        			{
        				String address = addresses.nextElement().toString().substring(1);
        				//Log.e("JAVASOCKET TEST InetAddress", address);
        				showResult("InetAddress", address);
        				
        				//ping_echo(address);
        				checkReachable(address);
        				//ping_socket(address);
        				//ping_shell(address);
        			}
        		}
        	}
        	catch (Exception e)
        	{
        		e.printStackTrace();
        	}
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
    
    private static int line_count = 0;
    private static boolean isFull = false;
    public static void showResult(String method, String msg)
    {
    	if(line_count == 5 || isFull)
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
