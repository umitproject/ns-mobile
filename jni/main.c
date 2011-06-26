#include <sys/socket.h>	
#include <sys/types.h>
#include <netinet/in.h>	
#include <netinet/ip.h>
#include <arpa/inet.h>	
#include <netinet/tcp.h>
#include <netdb.h>

#include <unistd.h>
#include <time.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <pcap.h>
#include <signal.h>

char datagram[4096]; /* datagram buffer */

pcap_t *session;

struct pseudo_hdr {
	u_int32_t src;			//src ip
	u_int32_t dst;			//dst ip
	u_char mbz;				//all 0 reserved bits
	u_char proto;			//protocol field
	u_int16_t len;			//tcp length
};

//TCP Header
typedef u_int tcp_seq;

struct tcpheader {
	u_short th_sport;               /* source port */
	u_short th_dport;               /* destination port */
	tcp_seq th_seq;                 /* sequence number */
	tcp_seq th_ack;                 /* acknowledgement number */
	u_char  th_off;
	u_char th_x2;             
#define TH_OFF(th)      (((th)->th_offx2 & 0xf0) >> 4)
	u_char  th_flags;
#define TH_FIN  0x01
#define TH_SYN  0x02
#define TH_RST  0x04
#define TH_PUSH 0x08
#define TH_ACK  0x10
#define TH_URG  0x20
#define TH_ECE  0x40
#define TH_CWR  0x80
#define TH_FLAGS        (TH_FIN|TH_SYN|TH_RST|TH_ACK|TH_URG|TH_ECE|TH_CWR)
	u_short th_win;                 /* window */
	u_short th_sum;                 /* checksum */
	u_short th_urp;                 /* urgent pointer */
};

/* IP header */
struct ipheader {
	u_char  ip_vhl;                 /* version << 4 | header length >> 2 */
	u_char  ip_tos;                 /* type of service */
	u_short ip_len;                 /* total length */
	u_short ip_id;                  /* identification */
	u_short ip_off;                 /* fragment offset field */
#define IP_RF 0x8000            /* reserved fragment flag */
#define IP_DF 0x4000            /* dont fragment flag */
#define IP_MF 0x2000            /* more fragments flag */
#define IP_OFFMASK 0x1fff       /* mask for fragmenting bits */
	u_char  ip_ttl;                 /* time to live */
	u_char  ip_p;                   /* protocol */
	u_short ip_sum;                 /* checksum */
	struct  in_addr ip_src,ip_dst;  /* source and dest address */
};
#define IP_HL(ip)               (((ip)->ip_vhl) &amp; 0x0f)
#define IP_V(ip)                (((ip)->ip_vhl) >> 4)


/* Ethernet header */
struct ethernetheader {
	u_char  ether_dhost[6];    /* destination host address */
	u_char  ether_shost[6];    /* source host address */
	u_short ether_type;                     /* IP? ARP? RARP? etc */
};

//Gets local IP
char* getLocalIP()
{
	char errbuf[100];
	
	pcap_if_t *alldev;
	
	if((pcap_findalldevs(&alldev, errbuf))== -1) {
		printf("%s \n", errbuf);
		exit(1);
	}
	
	struct pcap_addr *address = alldev->addresses;
	struct sockaddr_in *ip;
	
	while(address) {
		if(address->addr) {
			ip = (struct sockaddr_in *) address->addr;
			//printf("Device %s\n", alldev->name);
			return inet_ntoa(ip->sin_addr);
		}
		address = address->next;
	}
	return "";
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


//computing TCP Checksum
uint16_t csum (uint16_t * addr, int len)
{
	int nleft = len;
	uint32_t sum = 0;
	uint16_t *w = addr;
	uint16_t answer = 0;
	
	while( nleft > 1 ) {
		sum += *w++;
		nleft -= 2;
	}
	if (nleft == 1) {
		*(unsigned char *)  (&answer) = *(unsigned char *) w;
		sum += answer;
	}
	sum = (sum >> 16)+(sum & 0xffff);
	sum += (sum >> 16);
	answer = ~sum;
	return (answer);
}

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


void syn()
{
	//	if(getuid())
	//	{
	//		printf("Run this program as root");
	//	}
	
	
	char src_ip[17];
	char dst_ip[17];
	
	short dst_port = 80;
	short th_sport = 1234;
	
	short tcp_flags = TH_SYN;
	
	//Headers
	struct ip *iph = (struct ip *) datagram;
	struct tcpheader *tcph = (struct tcpheader *) (datagram + sizeof (struct ip));
	
	struct sockaddr_in servaddr;
	
	snprintf(src_ip,16,"%s", getLocalIP());  //src ip
	snprintf(dst_ip,16,"%s","209.85.175.104"); //google's ip
	
	printf("Source IP %s\nDestination IP %s\n", src_ip, dst_ip);
	
//	//Setting up pcap
//	struct sigaction act;
//	act.sa_handler = sigfunc;
//	sigemptyset(&act.sa_mask);
//	act.sa_flags = 0;
//	session = (pcap_t *)pcapSetup(dst_ip);
	
	memset(datagram, 0, 4096); //clearing the buffer
	
	int s = socket(PF_INET, SOCK_RAW, IPPROTO_TCP);
	servaddr.sin_family = AF_INET;
	inet_pton(AF_INET, dst_ip, &servaddr.sin_addr);
	
	int tcpheader_size = sizeof(struct tcpheader);
	
	printf("TCP Header Size %d\n", tcpheader_size);
	
	iph->ip_hl = 5;					//header length 5
	iph->ip_v = 4;					//version 4
	iph->ip_tos = 0;				//type of service
	iph->ip_len = sizeof(struct ip) + sizeof(struct tcpheader);  //no data
	iph->ip_id = htons(31337);		//id
	iph->ip_off = 0;				//no fragmentation
	iph->ip_ttl = 250;				//time to live
	iph->ip_p = IPPROTO_TCP;		//6
	iph->ip_sum = 0;				//let kernel fill the checksum
	
	inet_pton(AF_INET, src_ip, &(iph->ip_src));	//local device ip
	iph->ip_dst.s_addr = servaddr.sin_addr.s_addr;	//destination address
	
	tcph->th_sport = htons(th_sport);	//any port
	tcph->th_dport = htons(dst_port);	//destination port
	tcph->th_seq = htonl(31337);		//random
	tcph->th_ack = htonl(0);			//ACK not needed
	tcph->th_x2 = 0;					//
	tcph->th_off = 10;					//data offset
	tcph->th_flags = tcp_flags;			//SYN flag
	tcph->th_win = htons(65535);		//window size
	tcph->th_sum = 0;					//later
	tcph->th_urp = 0;					//no urgent pointer
	
	if(tcpheader_size % 4 != 0) //padding to 32 bits
		tcpheader_size = ((tcpheader_size % 4) + 1) * 4;
	printf("TCP Header Size %d\n", tcpheader_size);
	
	struct pseudo_hdr *phdr = (struct pseudo_hdr *) (datagram + sizeof(struct ip) + sizeof(struct tcpheader));
	memset(phdr, 0, sizeof(phdr));
	
	phdr->src = iph->ip_src.s_addr;
	phdr->dst = iph->ip_dst.s_addr;
	phdr->mbz = 0;
	phdr->len = ntohs(0x14);
	tcph->th_sum = csum((uint16_t *)phdr, tcpheader_size + 12);
	
	int one = 1;
	const int *val = &one;
	if(setsockopt(s, IPPROTO_IP, IP_HDRINCL, val, sizeof(one)) < 0)
		printf("Cannot set HDRINCL for port %d", th_sport);	
	
	if (sendto(s, datagram, iph->ip_len, 0, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0)
	{
		printf("Error in sending");
	}
	else printf("Sent\n");
	
//	sigaction(SIGALRM, &act, 0);
//	alarm(1); //use a default timeout
//	
//	int timeout = 0;
//	timeout = pcap_dispatch(session, -1, sniffer, (u_char *)dst_port);
//	alarm(0);	
}


int main (int argc, const char * argv[]) {
    syn();
	return 0;	
}
