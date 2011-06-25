#define __USE_BSD
#include <sys/socket.h>	

#define __FAVOR_BSD
#include <netinet/in.h>	
#include <netinet/ip.h>
#include <arpa/inet.h>	
#include <netinet/tcp.h>
#include <unistd.h>
#include <time.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <pcap.h>

char datagram[4096]; /* datagram buffer */

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
#define TH_OFF(th)      (((th)->th_offx2 &amp; 0xf0) >> 4)
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

//gets interface from pcap
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
	
	struct ip *iph = (struct ip *) datagram;
	struct tcpheader *tcph = (struct tcpheader *) (datagram + sizeof (struct ip));
	struct sockaddr_in servaddr;
	
	memset(datagram, 0, 4096); //clearing the buffer
	
	printf("sizeof struct ip %d\n", sizeof(struct ip));
	
	snprintf(src_ip,16,"%s","10.0.2.2");  //src ip
	snprintf(src_ip,16,"%s","209.85.175.104"); //google's ip
	
	int s = socket(PF_INET, SOCK_RAW, IPPROTO_TCP);
	servaddr.sin_family = AF_INET;
	//servaddr.sin_port = htons(i);
	inet_pton(AF_INET, dst_ip, &servaddr.sin_addr);
		
	int tcpheader_size = sizeof(struct tcpheader);
	
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
	else printf("Sent");
}

int main (int argc, const char * argv[]) {
	printf("Device %s", getDevice());
    syn();
	
	return 0;
	
}
