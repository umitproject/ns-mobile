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

public class scan_thread implements Runnable{

	String address;
	int method;
	public scan_thread(String address, int method)
	{
		this.address = address;
		this.method = method;
	}
	
	@Override
	public void run() {
		
		switch(method)
		{
			case 1:	javasockets.checkReachable(address); break;
			case 2: javasockets.ping_socket(address); break;
			case 3: javasockets.ping_echo(address); break;
			case 4: javasockets.ping_shell(address); break;
			case 5: javasockets.socket_tcp(address); break;
			default:javasockets.checkReachable(address); break;
		}
		
	}

}
