package org.umit.ns.mobile.model;

import java.util.ArrayList;
import java.util.Arrays;

public interface ScanArgsConst {
	public static ArrayList<String> ARGS = new ArrayList<String>(Arrays.asList("-iR","--exclude","-sL","-sn","-Pn",
			"-PS","-PA","-PU","-PY","-PE","-PP","-PM","-PO","-n","-R","--dns-servers","--system-dns","--traceroute",
			"-sS","-sT","-sA","-sW","-sM","-sU","-sN","-sF","-sX","--scanflags","-sI","-sY","-sZ","-sI","-b","-p","-F","-r",
			"--top-ports","--port-ratio","-sV","--version-intensity","--version-light","--version-all","--version-trace",
			"-sC","--script=","--script-ARGS=","--script-trace","--script-updatedb","--script-help=","-O","--osscan-limit",
			"--osscan-guess","-T","--min-hostgroup","--max-hostgroup","--min-parallelism","--max-parallelism",
			"--min-rtt-timeout","--max-rtt-timeout","--initial-rtt-timeout","--max-retries","--host-timeout","--scan-delay",
			"--min-rate","--max-rate","-f","--mtu","-D","-S","-e","-g","--source-port","--data-length","--ip-options","--ttl",
			"--spoof-mac","--badsum","--reason","--open","-6","-A",	"--send-eth","--send-ip"));
	public static ArrayList<String> NO_SPACE_ARGS = new ArrayList<String>(Arrays.asList("-PS","-PA","-PU","-PY","-PO",
			"--script=","--script-ARGS=","--script-help=","-T"));
}
