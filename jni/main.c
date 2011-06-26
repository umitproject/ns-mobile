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

//pcap_t *session;

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
	u_char  th_offx2;
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
#define IP_HL(ip)               (((ip)->ip_vhl) & 0x0f)
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

uint16_t csum (uint16_t *addr, int len) {   
//RFC 1071

	register long sum = 0;
	int count = len;
	uint16_t temp;

	while (count > 1)  {
		temp = htons(*addr++);
		sum += temp;
		count -= 2;
	}

	/*  Add left-over byte, if any */
	if(count > 0)
		sum += *(unsigned char *)addr;

	/*  Fold 32-bit sum to 16 bits */
	while (sum >> 16)
		sum = (sum & 0xffff) + (sum >> 16);

	uint16_t checksum = ~sum;
	return checksum;
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
	
	unsigned char tcp_flags = 0x02; //TH_SYN
	
	//Headers
	struct ip *iph = (struct ip *) datagram;
	struct tcpheader *tcph = (struct tcpheader *) (datagram + sizeof (struct ip));
	
	struct sockaddr_in servaddr;
	
	snprintf(src_ip,16,"%s", getLocalIP());  //src ip
	snprintf(dst_ip,16,"%s","209.85.175.104"); //google's ip
	
	printf("Source IP %s\nDestination IP %s\n", src_ip, dst_ip);
	
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
	iph->ip_ttl = 255;				//time to live
	iph->ip_p = IPPROTO_TCP;		//6
	iph->ip_sum = 0;				//let kernel fill the checksum
	
	printf("IP len %d\n", iph->ip_len);
	
	inet_pton(AF_INET, src_ip, &(iph->ip_src));	//local device ip
	iph->ip_dst.s_addr = servaddr.sin_addr.s_addr;	//destination address
	
	tcph->th_sport = htons(th_sport);	//any port
	tcph->th_dport = htons(dst_port);	//destination port
	tcph->th_seq = htonl(31337);		//random
	tcph->th_ack = htonl(0);			//ACK not needed
//	tcph->th_x2 = 0;					//
	tcph->th_offx2 = 0x50;	 			//data offset
	tcph->th_flags = 0x02;				//SYN flag
	tcph->th_win = htons(65535);		//window size
	tcph->th_sum = 0;					//later
	tcph->th_urp = 0;					//no urgent pointer
	
	struct pseudo_hdr *phdr = (struct pseudo_hdr *) (datagram + sizeof(struct ipheader) + sizeof(struct tcpheader));

	memset(phdr, 0, sizeof(phdr));
	
	phdr->src = iph->ip_src.s_addr;
	phdr->dst = iph->ip_dst.s_addr;
	phdr->mbz = 0;
	phdr->proto = IPPROTO_TCP;
	phdr->len = ntohs(0x14); //size of tcp header

	tcph->th_sum = htons(csum((unsigned short *)tcph, sizeof(struct pseudo_hdr)+ sizeof(struct tcpheader)));
		
	int one = 1;
	const int *val = &one;
	if(setsockopt(s, IPPROTO_IP, IP_HDRINCL, val, sizeof(one)) < 0)
		printf("Cannot set HDRINCL for port %d", th_sport);	
	
	if (sendto(s, datagram, iph->ip_len, 0, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0)
	{
		printf("Error in sending");
	}
	else printf("Sent\n");	
}

int main (int argc, const char * argv[]) {
    syn();
	return 0;	
}
