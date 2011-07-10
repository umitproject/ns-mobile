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



//pcap_t* pcapSetup(char *host)
//{
//	char *dev, errbuf[100];	
//	struct bpf_program filter;
//	char filter_exp[30] = "host ";
//
//	strncpy((char *)filter_exp+9, host, 16);
//
//	
//	dev = pcap_lookupdev(errbuf);
//	if (dev == NULL) {
//		exit(1);
//	}
//	
//	session = pcap_open_live(dev, 65535, 0, 0, errbuf);
//	if (session == NULL) {
//		exit(1);
//	}
//	
//	if (pcap_compile(session, &filter, filter_exp, 0, 0) == -1) {
//		exit(1);
//	}
//
//	if (pcap_setfilter(session, &filter) == -1) {
//		exit(1);
//	}
//	
//	return session;
//}



//void getaddress(struct addrinfo hints)
//{
//	char ipstr[INET6_ADDRSTRLEN];
//	struct sockaddr *servaddr;
//	struct addrinfo *res, *p;
//	int status;
//	
//	if((status = getaddrinfo(NULL, "3490", &hints, &res)) !=0) {
//		printf("getaddrinfo %s\n", gai_strerror(status));
//		exit(1);
//	}
//	
//	for (p=res; p!=NULL; p=p->ai_next) {
//		void *addr;
//		
//		servaddr = (struct sockaddr *)p->ai_addr;
//		addr = servaddr->sa_data;
//		
//		//		if (p->ai_family == AF_INET) { //IPv4
//		//			servaddr = (struct sockaddr_in *)p->ai_addr;
//		//			addr = &(servaddr->sin_addr);
//		//			ipver = "IPv4";
//		//		}
//		//		else {
//		//			servaddr6 = (struct sockaddr_in6 *) p->ai_addr;
//		//			addr = &(servaddr6->sin6_addr);
//		//			ipver = "IPv6";
//		//		}		
//		inet_ntop(p->ai_family, addr, ipstr, sizeof ipstr);
//		printf("%s\n", ipstr);
//	}
//	
//	freeaddrinfo(res);
//}	
//
//void sigfunc(int signum) {       /* signal handler */	
//	pcap_breakloop(session);
//}

//void sniffer(u_char *args, const struct pcap_pkthdr *header, const u_char *packet) {
//	
//	const struct tcpheader *tcp;
//	const struct ip *iph;
//	const struct ethheader *ether;
//	struct servent *serv;
//	
//	int size_ip;
//	int size_tcp;
//	
//	ether = (struct ethheader*) (packet);
//	iph = (struct ip *) (packet + 14);  //SIZE_ETHERNET
//		
//	size_ip = IP_HL(iph)*4;
//	if (size_ip < 20) {
//		fprintf (stderr, "Invalid IP header length: %u bytes \n", size_ip);
//		return;
//	}
//	
//	if (iph->ip_p != IPPROTO_TCP) {
//		fprintf (stderr, "Returned Packet is not TCP protocol \n");
//		return;
//	}
//	
//	tcp = (struct tcpheader*)(packet + 14 + size_ip); //SIZE_ETHERNET
//	size_tcp = TH_OFF(tcp)*4;
//	if (size_tcp < 20) {
//		fprintf (stderr, " * Invalid TCP header length: %u bytes\n", size_tcp);
//		return;
//	}
//	
//	if (((tcp->th_flags & 0x02) == TH_SYN) && (tcp->th_flags & 0x10) == TH_ACK) {
//		serv = getservbyport ( htons((int)args), "tcp" );
//		//fprintf (stdout, "TCP port %d open , possible service: %s\n", args, serv->s_name);
//		printf("TCP Port open");
//		// RST is sent by kernel automatically
//	}
//	else if ((tcp->th_flags & 0x04 ) == TH_RST) {
//		//fprintf (stdout, "TCP port %d closed\n", args ); too much info on screen
//		printf("TCP PORT CLOSED");
//	}
//	
//	printf("no idea");
//}

// void syn()
//{
	//	//Setting up pcap
	//	struct sigaction act;
	//	act.sa_handler = sigfunc;
	//	sigemptyset(&act.sa_mask);
	//	act.sa_flags = 0;
	//	session = (pcap_t *)pcapSetup(dst_ip);
	
	//....send packet....
	////	sigaction(SIGALRM, &act, 0);
	//	alarm(1); //use a default timeout
	//	
	//	int timeout = 0;
	//	timeout = pcap_dispatch(session, -1, sniffer, (u_char *)dst_port);
	//	alarm(0);	
//}


