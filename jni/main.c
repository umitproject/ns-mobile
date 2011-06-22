#include <stdio.h>
#include <pcap.h>
#include <string.h>

void main()
{
	char *dev, errbuf[100];
	char * szLogThis;

	dev = pcap_lookupdev(errbuf);
	if (dev == NULL) {
		szLogThis = "Couldn't find default device";		
	}
	else szLogThis = dev;
	
	printf("Device %s", szLogThis);
}