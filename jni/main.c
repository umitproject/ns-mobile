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

#include <pthread.h>

#define THREAD_COUNT 1

char datagram[4096];
struct ip *iph = (struct ip *) datagram;
struct tcpheader *tcph = (struct tcpheader *) (datagram + sizeof (struct ip));
struct sockaddr_in servaddr;
struct sigaction act;
pcap_t *handle;
char *victim;

int s_timeout = 10;
int timeout = 0;
unsigned short th_sport = 1234;

void setVictim(char* v) { victim = v; }

#define PING_TIMEOUT 2
struct in_addr ouraddr = { 0 };
unsigned long global_rtt = 0;

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

#define SIZE_ETHERNET 14

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

pcap_t* pcapSetup(char* dst_ip)
{
	//Setting up pcap
	char *dev = getDevice();
	char errbuf[100];
	struct bpf_program fp;
	
	char filter_exp[30] = "src host "; /* The filter expression */
	strncpy((char *)filter_exp+9, dst_ip, 16);
	bpf_u_int32 mask; /* The netmask of our sniffing device */
	bpf_u_int32 net; /* The IP of our sniffing device */ 
	
	//printf("Device %s\n", dev);
	//printf("Filter %s\n", filter_exp);
	
	if (pcap_lookupnet(dev, &net, &mask, errbuf) == -1) 
	{
		printf("Can't get netmask for device %s\n", dev);
		net = 0;
		mask = 0;
		exit(1);
	}
	
	//printf("Netmask %d, IP %d\n", net, mask);
	
	handle = pcap_open_live(dev, BUFSIZ, 1, 1000, errbuf);
	if (handle == NULL) 
	{
		printf("Couldn't open device %s: %s\n", dev, errbuf);
		exit(1);
	}
	
	if (pcap_compile(handle, &fp, filter_exp, 0, net) == -1) 
	{          
		printf("Couldn't parse filter %s: %s\n", filter_exp, pcap_geterr(handle));
		exit(1);
	} 
	
	if (pcap_setfilter(handle, &fp) == -1) 
	{
		printf("Couldn't install filter %s: %s\n", filter_exp, pcap_geterr(handle)); 
		exit(1);
	}
	
	return handle;
}

void got_packet(u_char *args, const struct pcap_pkthdr *header, const u_char *packet)
{
	struct tcpheader *tcph;
	struct ipheader *iph;
	struct ethernetheader *eh;
	
	int size_ip;
	int size_tcp;
	
	eh = (struct ethernetheader*) (packet);
	iph = (struct ipheader*) (packet + SIZE_ETHERNET);
	
	size_ip = IP_HL(iph)*4;
	
	if(size_ip < 20)
	{
		printf("Invalid IP Header length %d bytes\n", size_ip);
		return;
	}
	
	if(iph->ip_p != IPPROTO_TCP) 
	{
		printf("Not TCP \n");
		return;
	}
	
	tcph = (struct tcpheader*) (packet + SIZE_ETHERNET + size_ip);
	size_tcp = TH_OFF(tcph)*4;
	
	if(size_tcp < 20)
	{
		printf("Invalid TCP Header length %d bytes\n", size_tcp);
		return;
	}
	
	//printf("Port %d TCP %d\n", ntohs(tcph->th_sport), tcph->th_flags);
	
	if(((tcph->th_flags & 0x02) == TH_SYN) && (tcph->th_flags & 0x10) == TH_ACK) 
	{
		printf("Port open %d\n", ntohs(tcph->th_sport));
	}
	
	else if ((tcph->th_flags & 0x04) == TH_RST)
	{
		//printf("Port closed %d\n", (int)args);
	}
}

void sigfunc(int signum) 
{
	pcap_breakloop(handle);
}

void* scanport(void* port)
{
	int dst_port = (int)port;
	char* dst_ip = victim;
	char src_ip[17];
	snprintf(src_ip,16,"%s", inet_ntoa(ouraddr));  //src ip
		
	memset(datagram, 0, 4096); //clearing the buffer
	int s = socket(PF_INET, SOCK_RAW, IPPROTO_TCP);
	inet_pton(AF_INET, dst_ip, &servaddr.sin_addr);

	int tcpheader_size = sizeof(struct tcpheader);

	iph->ip_hl = 5;					//header length 5
	iph->ip_v = 4;					//version 4
	iph->ip_tos = 0;				//type of service
	iph->ip_len = sizeof(struct ip) + sizeof(struct tcpheader);  //no data
	iph->ip_id = htons(31337);		//id
	iph->ip_off = 0;				//no fragmentation
	iph->ip_ttl = 255;				//time to live
	iph->ip_p = IPPROTO_TCP;		//6
	iph->ip_sum = 0;				//let kernel fill the checksum
	inet_pton(AF_INET, src_ip, &(iph->ip_src));	//local device ip
	iph->ip_dst.s_addr = servaddr.sin_addr.s_addr;	//destination address

	tcph->th_sport = htons(th_sport);	//any port
	tcph->th_dport = htons(dst_port);	//destination port
	tcph->th_seq = htonl(31337);		//random
	tcph->th_ack = htonl(0);			//ACK not needed
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
	{
		printf("Cannot set HDRINCL for dest_port %d socket %d", dst_port, s);
	}

	if (sendto(s, datagram, iph->ip_len, 0, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0)
	{
		printf("Error in sending packet for port %d\n", dst_port);
	}
	close(s);
	
	sigaction (SIGALRM, &act, 0);
	alarm(s_timeout);

	pcap_dispatch(handle, -1, got_packet, (u_char *)NULL);
	alarm(0);

	if (timeout == -2) {
		printf("Timeout for port %d\n", dst_port);
	}
}


//
void synScanSetup()
{
	//incomplete
	//move global assignment statements from scanport to this function
	act.sa_handler = sigfunc;
	sigemptyset(&act.sa_mask);
	act.sa_flags = 0;
}

void syn(int low, int high)
{
	//Setup Syn scanning
	synScanSetup();
	
	pthread_t scanVictim[THREAD_COUNT];
	handle = (pcap_t *)pcapSetup(victim);
	 	
	int i=0, j=0;
	for(i=low; i<=high; i++)
	{
		pthread_create(&scanVictim[i%THREAD_COUNT], NULL, scanport, (void *)i);
		if(i%THREAD_COUNT==0)
		{
			for(j=0; j<THREAD_COUNT; j++)
			{
				int t = pthread_join(&scanVictim[j], NULL); 
			}
		}
		//scanport((void*)i);
	}
}

//nmap isup() method 
/* A relatively fast (or at least short ;) ping function.  Doesn't require a 
   seperate checksum function */
int isup(struct in_addr target) {
  int res, retries = 3;
  struct sockaddr_in sock;
  /*type(8bit)=8, code(8)=0 (echo REQUEST), checksum(16)=34190, id(16)=31337 */
#ifdef __LITTLE_ENDIAN_BITFIELD
  unsigned char ping[64] = { 0x8, 0x0, 0x8e, 0x85, 0x69, 0x7A };
#else
  unsigned char ping[64] = { 0x8, 0x0, 0x85, 0x8e, 0x7A, 0x69 };
#endif
  int sd;
  struct timeval tv;
  struct timeval start, end;
 fd_set fd_read;
  struct {
    struct iphdr ip;
    unsigned char type;
    unsigned char code;
    unsigned short checksum;
    unsigned short identifier;
    char crap[16536];
  }  response;

sd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);

bzero((char *)&sock,sizeof(struct sockaddr_in));
sock.sin_family=AF_INET;
sock.sin_addr = target;

gettimeofday(&start, NULL);
while(--retries) {
  if ((res = sendto(sd,(char *) ping,64,0,(struct sockaddr *)&sock,
		    sizeof(struct sockaddr))) != 64) {
    return 0;
  }
  FD_ZERO(&fd_read);
  FD_SET(sd, &fd_read);
  tv.tv_sec = 0; 
  tv.tv_usec = 1e6 * (PING_TIMEOUT / 3.0);
  while(1) {
    if ((res = select(sd + 1, &fd_read, NULL, NULL, &tv)) != 1) 
	{
		  break;
	}
    else {
      read(sd,&response,sizeof(response));
		//printf("Response %d %d %d %d\n", response.ip.saddr, response.type, response.code, response.identifier);

      if  (response.ip.saddr == target.s_addr &&  !response.type 
	   && !response.code   && response.identifier == 31337) {
	gettimeofday(&end, NULL);
	global_rtt = (end.tv_sec - start.tv_sec) * 1e6 + end.tv_usec - start.tv_usec;	
	ouraddr.s_addr = response.ip.daddr;
	close(sd);
	return 1;	
      }
    }
  }
}
close(sd);
printf("UNREACHABLE. SOCKET CLOSED \n");
return 0;
}



int main (int argc, const char * argv[]) {

	int low, high;

	if(argc==1 || argc>4)
	{
		printf("Usage: syns victim_ip [low port] [high port]\n");
		exit(1);
	}
	
	if(argc==2) 
	{
		printf("Port range not specified. Scanning 1-1024\n");
		low = 1;
		high = 1024;
	}
	
	if(argc==3)
	{
		printf("High port not specified. Scanning till 65536.\n");
		low = atoi(argv[2]);
		high = 65536;
	}
	
	if(argc == 4)
	{
		low = atoi(argv[2]);
		high = atoi(argv[3]);
	}
	
	struct timeval start, end;
	struct in_addr target;
	inet_aton(argv[1], &target.s_addr);

	printf("isup %s %d rtt %d ip %s\n", argv[1], isup(target), global_rtt, inet_ntoa(ouraddr));
		
	gettimeofday(&start, NULL);
	setVictim(argv[1]);
    syn(low, high);
	gettimeofday(&end, NULL);
	printf("Total time taken %d \n", end.tv_sec - start.tv_sec);
	
	return 0;	
}
