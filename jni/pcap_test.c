#include <stdio.h>
#include <pcap.h>
#include <string.h>

#include <netinet/tcp.h>
#include <netinet/in.h>

void main()
{	
	printf("Device %s", getDevice);
	
	
}

char* getDevice()
{
	char *dev, errbuf[100];
	char * szLogThis;

	dev = pcap_lookupdev(errbuf);
	if (dev == NULL) {
		szLogThis = "Couldn't find default device";		
	}
	else szLogThis = dev;
	
	return szLogThis;
}
