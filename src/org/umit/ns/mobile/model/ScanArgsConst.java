package org.umit.ns.mobile.model;

import android.webkit.HttpAuthHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public interface ScanArgsConst {
	public static ArrayList<String> ARGS = new ArrayList<String>(Arrays.asList(
			"-iR",
			"--exclude",
			"-sL",
			"-sn",  //TODO -PR arp ping and maybe others?
			"-Pn",
			"-PS",
			"-PA",
			"-PU",
			"-PY",
			"-PE",
			"-PP",
			"-PM",
			"-PO",
			"-n",
			"-R",
			"--dns-servers",
			"--system-dns",
			"--traceroute",
			"-sS",
			"-sT",
			"-sA",
			"-sW",
			"-sM",
			"-sU",
			"-sN",
			"-sF",
			"-sX",
			"--scanflags",
			"-sI",
			"-sY",
			"-sZ",
			"-sO",
			"-b",
			"-p",
			"-F",
			"-r",
			"--top-ports",
			"--port-ratio",
			"-sV",
			"--version-intensity",
			"--version-light",
			"--version-all",
			"--version-trace",
			"-sC",
			"--script=",
			"--script-args=",
			"--script-trace",
			"--script-updatedb",
			"--script-help=",
			"-O",
			"--osscan-limit",
			"--osscan-guess",
			"-T",
			"--min-hostgroup",
			"--max-hostgroup",
			"--min-parallelism",
			"--max-parallelism",
			"--min-rtt-timeout",
			"--max-rtt-timeout",
			"--initial-rtt-timeout",
			"--max-retries",
			"--host-timeout",
			"--scan-delay",
			"--max-scan-delay",
			"--min-rate",
			"--max-rate",
			"-f",
			"--mtu",
			"-D",
			"-S",
			"-e",
			"-g",
			"--source-port",
			"--data-length",
			"--ip-options",
			"--ttl",
			"--spoof-mac",
			"--badsum",
			"--reason",
			"--open",
			"-6",
			"-A",
			"--send-eth",
			"--send-ip"));

	public static ArrayList<String> FULL_ARGS = new ArrayList<String>(Arrays.asList(
			"-iR <num hosts>: Choose random targets",
			"--exclude <host1[,host2][,host3],...>: Exclude hosts/networks",
			"-sL: List Scan - simply list targets to scan",
			"-sn: Ping Scan - disable port scan",
			"-Pn: Treat all hosts as online -- skip host discovery",
			"-PS[portlist]: TCP SYN discovery to given ports",
			"-PA[portlist]: TCP ACK discovery to given ports",
			"-PU[portlist]: UDP discovery to given ports",
			"-PY[portlist]: SCTP discovery to given ports",
			"-PE: ICMP echo discovery probes",
			"-PP: Timestamp discovery probes",
			"-PM: Netmask request discovery probes",
			"-PO[protocol list]: IP Protocol Ping",
			"-n: Never do DNS resolution [default: sometimes]",
			"-R: Always resolve [default: sometimes]",
			"--dns-servers <serv1[,serv2],...>: Specify custom DNS servers",
			"--system-dns: Use OS's DNS resolver",
			"--traceroute: Trace hop path to each host",
			"-sS: TCP SYN scan",
			"-sT: Connect() scan",
			"-sA: ACK scan",
			"-sW: Window scan",
			"-sM: Maimon scan",
			"-sU: UDP Scan",
			"-sN: TCP Null scan",
			"-sF: FIN scan",
			"-sX: Xmas scan",
			"--scanflags <flags>: Customize TCP scan flags",
			"-sI <zombie host[:probeport]>: Idle scan",
			"-sY: SCTP INIT scan",
			"-sZ: COOKIE-ECHO scan",
			"-sO: IP protocol scan",
			"-b <FTP relay host>: FTP bounce scan",
			"-p <port ranges>: Only scan specified ports",
			"-F: Fast mode - Scan fewer ports than the default scan",
			"-r: Scan ports consecutively - don't randomize",
			"--top-ports <number>: Scan <number> most common ports",
			"--port-ratio <ratio>: Scan ports more common than <ratio>",
			"-sV: Probe open ports to determine service/version info",
			"--version-intensity <level>: Set from 0 (light) to 9 (try all probes)",
			"--version-light: Limit to most likely probes (intensity 2)",
			"--version-all: Try every single probe (intensity 9)",
			"--version-trace: Show detailed version scan activity (for debugging)",
			"-sC: equivalent to --script=default",
			"--script=<Lua scripts>: <Lua scripts> is a comma separated list of directories, script-files or script-categories",
			"--script-args=<n1=v1,[n2=v2,...]>: Provide arguments to scripts",
			"--script-trace: Show all data sent and received",
			"--script-updatedb: Update the script database.",
			"--script-help=<Lua scripts>: Show help about scripts. <Lua scripts> is a comma separted list of script-files or script-categories.",
			"-O: Enable OS detection",
			"--osscan-limit: Limit OS detection to promising targets",
			"--osscan-guess: Guess OS more aggressively",
			"-T<0-5>: Set timing template (higher is faster)",
			"--min-hostgroup <size>: Parallel host scan group sizes",
			"--max-hostgroup <size>: Parallel host scan group sizes",
			"--min-parallelism <numprobes>: Probe parallelization",
			"--max-parallelism <numprobes>: Probe parallelization",
			"--min-rtt-timeout <time>: Specifies probe round trip time.",
			"--max-rtt-timeout <time>: Specifies probe round trip time.",
			"--initial-rtt-timeout <time>: Specifies probe round trip time.",
			"--max-retries <tries>: Caps number of port scan probe retransmissions.",
			"--host-timeout <time>: Give up on target after this long",
			"--scan-delay <time>: Adjust delay between probes",
			"--max-scan-delay <time>: Adjust delay between probes",
			"--min-rate <number>: Send packets no slower than <number> per second",
			"--max-rate <number>: Send packets no faster than <number> per second",
			"-f: Fragment packets",
			"--mtu <val>: Fragment packets with given MTU",
			"-D <decoy1,decoy2[,ME],...>: Cloak a scan with decoys",
			"-S <IP_Address>: Spoof source address",
			"-e <iface>: Use specified interface",
			"-g <portnum>: Use given port number",
			"--source-port <portnum>: Use given port number",
			"--data-length <num>: Append random data to sent packets",
			"--ip-options <options>: Send packets with specified ip options",
			"--ttl <val>: Set IP time-to-live field",
			"--spoof-mac <mac address/prefix/vendor name>: Spoof your MAC address",
			"--badsum: Send packets with a bogus TCP/UDP/SCTP checksum",
			"--reason: Display the reason a port is in a particular state",
			"--open: Only show open (or possibly open) ports",
			"-6: Enable IPv6 scanning",
			"-A: Enable OS detection, version detection, script scanning, and traceroute",
			"--send-eth: Send using raw ethernet frames",
			"--send-ip: Send using IP packets"
	));

	public static final HashMap<String, String> ARGS_MAP = new HashMap<String, String>()
	{{
			put( "-iR <num hosts>: Choose random targets" , "-iR" );
			put( "--exclude <host1[,host2][,host3],...>: Exclude hosts/networks" , "--exclude" );
			put( "-sL: List Scan - simply list targets to scan" , "-sL" );
			put( "-sn: Ping Scan - disable port scan" , "-sn" );
			put( "-Pn: Treat all hosts as online -- skip host discovery" , "-Pn" );
			put( "-PS[portlist]: TCP SYN discovery to given ports" , "-PS" );
			put( "-PA[portlist]: TCP ACK discovery to given ports" , "-PA" );
			put( "-PU[portlist]: UDP discovery to given ports" , "-PU" );
			put( "-PY[portlist]: SCTP discovery to given ports" , "-PY" );
			put( "-PE: ICMP echo discovery probes" , "-PE" );
			put( "-PP: Timestamp discovery probes" , "-PP" );
			put( "-PM: Netmask request discovery probes" , "-PM" );
			put( "-PO[protocol list]: IP Protocol Ping" , "-PO" );
			put( "-n: Never do DNS resolution [default: sometimes]" , "-n" );
			put( "-R: Always resolve [default: sometimes]" , "-R" );
			put( "--dns-servers <serv1[,serv2],...>: Specify custom DNS servers" , "--dns-servers" );
			put( "--system-dns: Use OS's DNS resolver" , "--system-dns" );
			put( "--traceroute: Trace hop path to each host" , "--traceroute" );
			put( "-sS: TCP SYN scan" , "-sS" );
			put( "-sT: Connect() scan" , "-sT" );
			put( "-sA: ACK scan" , "-sA" );
			put( "-sW: Window scan" , "-sW" );
			put( "-sM: Maimon scan" , "-sM" );
			put( "-sU: UDP Scan" , "-sU" );
			put( "-sN: TCP Null scan" , "-sN" );
			put( "-sF: FIN scan" , "-sF" );
			put( "-sX: Xmas scan" , "-sX" );
			put( "--scanflags <flags>: Customize TCP scan flags" , "--scanflags" );
			put( "-sI <zombie host[:probeport]>: Idle scan" , "-sI" );
			put( "-sY: SCTP INIT scan" , "-sY" );
			put( "-sZ: COOKIE-ECHO scan" , "-sZ" );
			put( "-sO: IP protocol scan" , "-sO" );
			put( "-b <FTP relay host>: FTP bounce scan" , "-b" );
			put( "-p <port ranges>: Only scan specified ports" , "-p" );
			put( "-F: Fast mode - Scan fewer ports than the default scan" , "-F" );
			put( "-r: Scan ports consecutively - don't randomize" , "-r" );
			put( "--top-ports <number>: Scan <number> most common ports" , "--top-ports" );
			put( "--port-ratio <ratio>: Scan ports more common than <ratio>" , "--port-ratio" );
			put( "-sV: Probe open ports to determine service/version info" , "-sV" );
			put( "--version-intensity <level>: Set from 0 (light) to 9 (try all probes)" , "--version-intensity" );
			put( "--version-light: Limit to most likely probes (intensity 2)" , "--version-light" );
			put( "--version-all: Try every single probe (intensity 9)" , "--version-all" );
			put( "--version-trace: Show detailed version scan activity (for debugging)" , "--version-trace" );
			put( "-sC: equivalent to --script=default" , "-sC" );
			put( "--script=<Lua scripts>: <Lua scripts> is a comma separated list of directories, script-files or script-categories" , "--script=" );
			put( "--script-args=<n1=v1,[n2=v2,...]>: Provide arguments to scripts" , "--script-args=" );
			put( "--script-trace: Show all data sent and received" , "--script-trace" );
			put( "--script-updatedb: Update the script database." , "--script-updatedb" );
			put( "--script-help=<Lua scripts>: Show help about scripts. <Lua scripts> is a comma separted list of script-files or script-categories." , "--script-help=" );
			put( "-O: Enable OS detection" , "-O" );
			put( "--osscan-limit: Limit OS detection to promising targets" , "--osscan-limit" );
			put( "--osscan-guess: Guess OS more aggressively" , "--osscan-guess" );
			put( "-T<0-5>: Set timing template (higher is faster)" , "-T" );
			put( "--min-hostgroup <size>: Parallel host scan group sizes" , "--min-hostgroup" );
			put( "--max-hostgroup <size>: Parallel host scan group sizes" , "--max-hostgroup" );
			put( "--min-parallelism <numprobes>: Probe parallelization" , "--min-parallelism" );
			put( "--max-parallelism <numprobes>: Probe parallelization" , "--max-parallelism" );
			put( "--min-rtt-timeout <time>: Specifies probe round trip time." , "--min-rtt-timeout" );
			put( "--max-rtt-timeout <time>: Specifies probe round trip time." , "--max-rtt-timeout" );
			put( "--initial-rtt-timeout <time>: Specifies probe round trip time." , "--initial-rtt-timeout" );
			put( "--max-retries <tries>: Caps number of port scan probe retransmissions." , "--max-retries" );
			put( "--host-timeout <time>: Give up on target after this long" , "--host-timeout" );
			put( "--scan-delay <time>: Adjust delay between probes" , "--scan-delay" );
			put( "--max-scan-delay <time>: Adjust delay between probes" , "--max-scan-delay" );
			put( "--min-rate <number>: Send packets no slower than <number> per second" , "--min-rate" );
			put( "--max-rate <number>: Send packets no faster than <number> per second" , "--max-rate" );
			put( "-f: Fragment packets" , "-f" );
			put( "--mtu <val>: Fragment packets with given MTU" , "--mtu" );
			put( "-D <decoy1,decoy2[,ME],...>: Cloak a scan with decoys" , "-D" );
			put( "-S <IP_Address>: Spoof source address" , "-S" );
			put( "-e <iface>: Use specified interface" , "-e" );
			put( "-g <portnum>: Use given port number" , "-g" );
			put( "--source-port <portnum>: Use given port number" , "--source-port" );
			put( "--data-length <num>: Append random data to sent packets" , "--data-length" );
			put( "--ip-options <options>: Send packets with specified ip options" , "--ip-options" );
			put( "--ttl <val>: Set IP time-to-live field" , "--ttl" );
			put( "--spoof-mac <mac address/prefix/vendor name>: Spoof your MAC address" , "--spoof-mac" );
			put( "--badsum: Send packets with a bogus TCP/UDP/SCTP checksum" , "--badsum" );
			put( "--reason: Display the reason a port is in a particular state" , "--reason" );
			put( "--open: Only show open (or possibly open) ports" , "--open" );
			put( "-6: Enable IPv6 scanning" , "-6" );
			put( "-A: Enable OS detection, version detection, script scanning, and traceroute" , "-A" );
			put( "--send-eth: Send using raw ethernet frames" , "--send-eth" );
			put( "--send-ip: Send using IP packets" , "--send-ip" );
	}};


	public static ArrayList<String> NO_SPACE_ARGS = new ArrayList<String>(
			Arrays.asList(
					"-PS",
					"-PA",
					"-PU",
					"-PY",
					"-PO",
					"--script=",
					"--script-args=",
					"--script-help=",
					"-T"));

	public static ArrayList<String> SPACE_ARGS = new ArrayList<String>(
			Arrays.asList(
					"-iR",
					"--exclude",
					"-sL",
					"-sn",
					"-Pn",
					"-PE",
					"-PP",
					"-PM",
					"-n",
					"-R",
					"--dns-servers",
					"--system-dns",
					"--traceroute",
					"-sS",
					"-sT",
					"-sA",
					"-sW",
					"-sM",
					"-sU",
					"-sN",
					"-sF",
					"-sX",
					"--scanflags",
					"-sI",
					"-sY",
					"-sZ",
					"-sO",
					"-b",
					"-p",
					"-F",
					"-r",
					"--top-ports",
					"--port-ratio",
					"-sV",
					"--version-intensity",
					"--version-light",
					"--version-all",
					"--version-trace",
					"-sC",
					"--script-trace",
					"--script-updatedb",
					"-O",
					"--osscan-limit",
					"--osscan-guess",
					"--min-hostgroup",
					"--max-hostgroup",
					"--min-parallelism",
					"--max-parallelism",
					"--min-rtt-timeout",
					"--max-rtt-timeout",
					"--initial-rtt-timeout",
					"--max-retries",
					"--host-timeout",
					"--scan-delay",
					"--max-scan-delay",
					"--min-rate",
					"--max-rate",
					"-f",
					"--mtu",
					"-D",
					"-S",
					"-e",
					"-g",
					"--source-port",
					"--data-length",
					"--ip-options",
					"--ttl",
					"--spoof-mac",
					"--badsum",
					"--reason",
					"--open",
					"-6",
					"-A",
					"--send-eth",
					"--send-ip"
					));

	public static ArrayList<String> ROOT_ARGS = new ArrayList<String>(Arrays.asList(
			"-PS",
			"-PY",
			"-sS",
			"-sA",
			"-sW",
			"-sM",
			"-sU",
			"-sN",
			"-sF",
			"-sX",
			"--scanflags",
			"-sI",
			"-sY",
			"-sZ",
			"-sO",
			"-O",
			"--osscan-limit",
			"--osscan-guess",
			"-f",
			"--mtu",
			"-D",
			"-S",
			"-e",
			"-g",
			"--source-port",
			"--data-length",
			"--ip-options",
			"--ttl",
			"--spoof-mac",
			"--badsum",
			"-A",
			"--send-eth",
			"--send-ip"));
}
