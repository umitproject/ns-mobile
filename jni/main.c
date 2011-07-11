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
#include <ctype.h>

#include <pthread.h>

#define MAX_THREADS 300

//Number of tries for a port if a response is not received within timeout
#define NUM_TRIES 1

#define ONE_SECOND 1000000
#define DEFAULT_RATE 30

char datagram[4096];
struct ip *iph = (struct ip *) datagram;
struct tcpheader *tcph = (struct tcpheader *) (datagram + sizeof (struct ip));
struct sockaddr_in servaddr;

pcap_t *handle;
char *victim;

pthread_t scanVictim[MAX_THREADS];
pthread_t sniffThread;
pthread_mutex_t mutex_probe;

unsigned short th_sport = 1234;

int threadCount;
unsigned long rateControl = 0;

struct ports {
	int port;
	struct ports* next;
	struct ports* prev;
};

int num_open = 0;
int num_closed = 0;
int num_awaited = 0;
int num_timedout = 0;

//Sent probes to ports will be kept in probeSent
//check probeSent if any of the ports has received responses
//if timedout, move to timedout list

struct ports* probeSent = NULL;
struct ports* open = NULL;
struct ports* closed = NULL;
struct ports* timedout = NULL;

int numports = 0;
int progress = 0;

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

//Gets the device to sniff packets on
char* getDevice()
{
	char *dev, errbuf[100];
	char * szLogThis;

	dev = pcap_lookupdev(errbuf);
	if (dev == NULL) {
		szLogThis = "Couldn't find default device";		
	}
	else {
		printf("Listening on Device %s\n", dev);
		szLogThis = dev;
	}
	
	return szLogThis;
}

//Checksum for TCP header
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

//Pcap setup
pcap_t* pcapSetup(char* dst_ip)
{
	//Setting up pcap
	char *dev = getDevice();
	char errbuf[PCAP_ERRBUF_SIZE];
	struct bpf_program fp;
	
	char filter_exp[30] = "src host "; //The filter expression 
	strncpy((char *)filter_exp+9, dst_ip, 16);
	bpf_u_int32 mask; //The netmask of our sniffing device
	bpf_u_int32 net; //The IP of our sniffing device 
	
	if (pcap_lookupnet(dev, &net, &mask, errbuf) == -1) 
	{
		printf("Can't get netmask for device %s\n", dev);
		net = 0;
		mask = 0;
		exit(1);
	}
		
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

//Packet sniffer and the actual SYN scanner
void got_packet(u_char *args, const struct pcap_pkthdr *header, const u_char *packet)
{
	pthread_mutex_lock(&mutex_probe);	
	
	struct tcpheader *tcph;
	struct ipheader *iph;
	struct ethernetheader *eh;
	
	int size_ip;
	int size_tcp;
	
	int src_port;
	
	eh = (struct ethernetheader*) (packet);
	iph = (struct ipheader*) (packet + SIZE_ETHERNET);
	
	size_ip = IP_HL(iph)*4;
	
	if(size_ip < 20)
	{
		//printf("Invalid IP Header length %d bytes\n", size_ip);
		pthread_mutex_unlock(&mutex_probe);
		return;
	}
	
	if(iph->ip_p != IPPROTO_TCP) 
	{
		//printf("Not TCP \n");
		pthread_mutex_unlock(&mutex_probe);
		return;
	}
	
	tcph = (struct tcpheader*) (packet + SIZE_ETHERNET + size_ip);
	size_tcp = TH_OFF(tcph)*4;
	
	if(size_tcp < 20)
	{
		//printf("Invalid TCP Header length %d bytes\n", size_tcp);
		pthread_mutex_unlock(&mutex_probe);
		return;
	}
	
	src_port = ntohs(tcph->th_sport);
	//printf("Port %d TCP %d\n", ntohs(tcph->th_sport), tcph->th_flags);
	
	if(((tcph->th_flags & 0x02) == TH_SYN) && (tcph->th_flags & 0x10) == TH_ACK)
	{
		printf("Port open %d\n", src_port);
		struct ports* new_open = malloc(sizeof(struct ports));
		new_open->port = src_port;
		new_open->next = open;
		open = new_open;
		num_open ++;
	}
	
	else if ((tcph->th_flags & 0x04) == TH_RST)
	{
//		printf("Port closed %d\n", src_port);
		struct ports* new_closed = malloc(sizeof(struct ports));
		new_closed->port = src_port;
		new_closed->next = closed;
		closed = new_closed;
		num_closed ++;
	}
	
	struct ports *a = probeSent;
	for(;a!=NULL; a=a->next)
	{
		//head
		if(a->port == src_port)
		{
			if(a->prev == NULL) // deleting from HEAD
			{
				probeSent = a->next;
			}
			else if(a->next==NULL) //deleting from END
			{
				a->prev->next = NULL;
			}
			else a->prev->next = a->next;
		 	num_awaited--;
//			printf("deleting %d\n", a->port);
		}
	}
	pthread_mutex_unlock(&mutex_probe);
}

void* synScanPort(void* port)
{		
	pthread_mutex_lock(&mutex_probe);
	
	//Source and Destination addresses
	int dst_port = (int)port;
	char* dst_ip = victim;
	char src_ip[17];
	snprintf(src_ip,16,"%s", inet_ntoa(ouraddr));  //src ip
		
	memset(datagram, 0, 4096); //clearing the buffer
	int s = socket(PF_INET, SOCK_RAW, IPPROTO_TCP);
	inet_pton(AF_INET, dst_ip, &servaddr.sin_addr);

	int tcpheader_size = sizeof(struct tcpheader);
	int timeout = 0;

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
		pthread_mutex_unlock(&mutex_probe);
		return;
	}
	
	if (sendto(s, datagram, iph->ip_len, 0, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0)
	{
		printf("Error in sending packet for port %d\n", dst_port);
		pthread_mutex_unlock(&mutex_probe);
		return;
	}

	struct ports* new_probe = malloc(sizeof(struct ports));
	new_probe->port = dst_port;
	new_probe->next = probeSent;
	if(probeSent != NULL)
		probeSent->prev = new_probe;
	new_probe->prev = NULL;
	probeSent = new_probe;
	num_awaited++;
	close(s);
	pthread_mutex_unlock(&mutex_probe);
}

void* finScanPort(void* port)
{		
	pthread_mutex_lock(&mutex_probe);
	
	//Source and Destination addresses
	int dst_port = (int)port;
	char* dst_ip = victim;
	char src_ip[17];
	snprintf(src_ip,16,"%s", inet_ntoa(ouraddr));  //src ip
		
	memset(datagram, 0, 4096); //clearing the buffer
	int s = socket(PF_INET, SOCK_RAW, IPPROTO_TCP);
	inet_pton(AF_INET, dst_ip, &servaddr.sin_addr);

	int tcpheader_size = sizeof(struct tcpheader);
	int timeout = 0;

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
	tcph->th_flags = 0x01;				//FIN flag
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
		pthread_mutex_unlock(&mutex_probe);
		return;
	}
	
	if (sendto(s, datagram, iph->ip_len, 0, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0)
	{
		printf("Error in sending packet for port %d\n", dst_port);
		pthread_mutex_unlock(&mutex_probe);
		return;
	}

	struct ports* new_probe = malloc(sizeof(struct ports));
	new_probe->port = dst_port;
	new_probe->next = probeSent;
	if(probeSent != NULL)
		probeSent->prev = new_probe;
	new_probe->prev = NULL;
	probeSent = new_probe;
	num_awaited++;
	close(s);
	pthread_mutex_unlock(&mutex_probe);
}


void* sniffPacket(void* a)
{
	pcap_loop(handle, -1, got_packet, (u_char *)NULL);
}

void syn(struct ports* head, int retry)
{
	if(retry==0)
	{
		printf("DONE!");
		printf("AWAITED %d\n", num_awaited);
			return;
	}
	else retry--;
	
	probeSent = NULL;
//	rateControl = global_rtt;
	
	handle = (pcap_t *)pcapSetup(victim);
	struct timeval start, end;
	unsigned long timeTaken = 0;
	
	pthread_create(&sniffThread, NULL, sniffPacket, NULL);
	
	int j=0;
	struct ports* current = head;
	for(; current!=NULL; current=current->next)
	{
		// if(current->port % threadCount == 1)
		// {
		// 	gettimeofday(&start, NULL);
		// }
		
//		printf("SCANNING %d\n", current->port);
		pthread_create(&scanVictim[current->port % threadCount], NULL, synScanPort, (void *)current->port);
		usleep(rateControl);
		if(current->port % threadCount==0)
		{
			for(j=0; j<threadCount; j++)
			{
				pthread_join(scanVictim[j], NULL);
			}
//			usleep(rateControl);
//			gettimeofday(&end, NULL);
//			timeTaken = (end.tv_sec - start.tv_sec) * 1e6 + end.tv_usec - start.tv_usec;
//			printf("timetaken %d\n", timeTaken);
//			rateControl = timeTaken/threadCount;
			//- (rateControl * threadCount);
		}
	}
	usleep(ONE_SECOND);
	pcap_breakloop(handle);
	
	//just for sanity
	// for(j=0; j<threadCount; j++)
	// 	{
	// 		pthread_join(scanVictim[j], NULL);
	// 	}
	// pthread_join(sniffThread, NULL);
	
	if(retry==0)
	{
		printf("DONE!");
		printf("AWAITED %d\n", num_awaited);
		return;
	}
	syn(probeSent, retry);
}

void fin(struct ports* head)
{
	probeSent = NULL;
	handle = (pcap_t *)pcapSetup(victim);
	struct timeval start, end;
	unsigned long timeTaken = 0;

	pthread_create(&sniffThread, NULL, sniffPacket, NULL);

	int j=0;
	struct ports* current = head;
	for(; current!=NULL; current=current->next)
	{
		// if(current->port % threadCount == 1)
		// {
		// 	gettimeofday(&start, NULL);
		// }
	//		printf("SCANNING %d\n", current->port);
		pthread_create(&scanVictim[current->port % threadCount], NULL, finScanPort, (void *)current->port);
		usleep(rateControl);
		if(current->port % threadCount==0)
		{
			for(j=0; j<threadCount; j++)
			{
				pthread_join(scanVictim[j], NULL);
			}
//			usleep(rateControl);
//			gettimeofday(&end, NULL);
//			timeTaken = (end.tv_sec - start.tv_sec) * 1e6 + end.tv_usec - start.tv_usec;
//			printf("timetaken %d\n", timeTaken);
//			rateControl = timeTaken/threadCount;
			//- (rateControl * threadCount);
		}
	}
	usleep(ONE_SECOND);
	pcap_breakloop(handle);
	
	printf("%d ports scanned. %d ports confirmed closed\n", numports, numports-num_awaited);
	if(num_awaited!=0) 
	{
		struct ports* o = probeSent;
		for(;o!=NULL; o=o->next)
		{
			printf("%d\topen|filtered\n", o->port);
		}
	}
}



//A ping function
//Adapted from nmap isup() function
//from the original Art of Port Scanning
int isup(struct in_addr target) 
{
	int res, retries = 3;
	struct sockaddr_in sock;
	
	#ifdef __LITTLE_ENDIAN_BITFIELD
	unsigned char ping[64] = { 0x8, 0x0, 0x8e, 0x85, 0x69, 0x7A };
	#else
	unsigned char ping[64] = { 0x8, 0x0, 0x85, 0x8e, 0x7A, 0x69 };
	#endif
	
	int sd;
	struct timeval tv;
	struct timeval start, end;

	fd_set fd_read;
	struct 
	{
		struct iphdr ip;
		unsigned char type;
		unsigned char code;
		unsigned short checksum;
		unsigned short identifier;
		char crap[16536];
	} response;

	sd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);

	bzero((char *)&sock,sizeof(struct sockaddr_in));
	sock.sin_family=AF_INET;
	sock.sin_addr = target;

	gettimeofday(&start, NULL);
	while(--retries)
	{
		if ((res = sendto(sd,(char *) ping,64,0,(struct sockaddr *)&sock, sizeof(struct sockaddr))) != 64) 
		{
			return 0;
		}
		
		FD_ZERO(&fd_read);
		FD_SET(sd, &fd_read);
		tv.tv_sec = 0; 
		tv.tv_usec = 1e6 * (PING_TIMEOUT / 3.0);
		while(1) 
		{
			if ((res = select(sd + 1, &fd_read, NULL, NULL, &tv)) != 1) 
			{
				break;
			}
			else 
			{
				read(sd,&response,sizeof(response));
				if  (response.ip.saddr == target.s_addr &&  !response.type && !response.code   && response.identifier == 31337) 
				{
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
		
	int i;	
	int low, high;

	if(argc==1 || argc>4)
	{
		printf("Usage: synscanner victim_ip [low port] [high port]\n");
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
		
	numports = high - low + 1;
	progress = numports / 100;
	
	struct timeval start, end;
	struct in_addr target;
	inet_aton(argv[1], &target.s_addr);

	if(isup(target)==0)
	{
		printf("Host %s seems to be down. Exiting.\n", argv[1]);
		exit(1);
	}
		
	printf("RTT %d microseconds\n", global_rtt);
	printf("Enter Rate Control: [0 for default - 30]: ");
	
	unsigned int packetrate;
	scanf("%d", &packetrate);
	if(packetrate == 0) 
		packetrate = DEFAULT_RATE;

	rateControl = ONE_SECOND/packetrate;
	
	threadCount = 5 * 1e6 / global_rtt;
	if(threadCount > MAX_THREADS)
		threadCount = MAX_THREADS;
	printf("Number of threads %d\n", threadCount);
	
	unsigned long t;
	gettimeofday(&start, NULL);
	setVictim(argv[1]);
	
	struct ports* all = NULL;

	for(i=high; i>=low; i--)
	{
		struct ports* new_all = malloc(sizeof(struct ports));
		new_all->port = i;
		new_all->next = all;
		if(probeSent != NULL)
			all->prev = new_all;
		new_all->prev = NULL;
 		all = new_all;
	}
	
//    syn(all, NUM_TRIES);
	fin(all);
	gettimeofday(&end, NULL);
	t = (end.tv_sec - start.tv_sec) * 1e6 + end.tv_usec - start.tv_usec;
	printf("Total time taken %d microseconds\n", t);
	
	return 0;
}
