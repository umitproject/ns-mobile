#JavaSockets
Testing different methods of pinging a host from Android  
Testing different methods for Host Discovery.

##Host Discovery
Uses isReachable ping to scan the network. Uses SubnetUtils to get the range of IP addresses to scan.  
Gets the subnet mask and the ip address from WifiInfo.

##Wifi Info
Shows various Wifi Connection parameters.

##Network interfaces
Gets the network interfaces and their ip addresses available for the device. 

##isReachable
Pings using isReachable method

##Echo ping
Ping using port 7 UDP echo (Datagram Channel)

##Socket ping
Port 13 socket channel ping

##Commandline ping
Using the shell command ping

##TCP Socket
Opens a TCP socket on port 80

#TODO
1) Add TTL setting  
2) Use other methods for Host Discovery as well  
3) Current UI hangs when doing Host Discovery. Improve Async Task  
4) Improve scanning for larger number of hosts  
5) Add option for scanning a specific range of hosts

