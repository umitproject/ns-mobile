package org.umit.ns.mobile.xml;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.sax.*;
import android.sax.RootElement;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.provider.Scanner.Hosts;
import org.umit.ns.mobile.provider.Scanner.Details;

import org.xml.sax.Attributes;

public class NmapSaxParser extends BaseNmapXmlParser {

	public Host h;
	public Host prescript;
	public Host postscript;

	public Detail p;
	public Detail hostInfo;
	public Detail hostOS;
	public Detail hostScript;
	public Detail hostTrace;


	public NmapSaxParser(ContentResolver contentResolver,
	                     String clientID, String scanID,
	                     String scanResultsFilename) {
		super(contentResolver,clientID,scanID,scanResultsFilename);
	}

	public void parse() {
		RootElement root = new RootElement(ROOT);

		root.getChild("runstats").getChild("finished").setStartElementListener(runstats_finished_listener);


		root.getChild(PRESCRIPT)
				.setStartElementListener(prescript_listener);

		root.getChild(POSTSCRIPT)
				.setStartElementListener(postscript_listener);

		Element host = root.getChild(HOST);
		host.setElementListener(host_listener);

		host.getChild("status").setStartElementListener( host_status_listener );

		Element host_address = host.getChild(ADDRESS);
		host_address.setStartElementListener( host_address_listener );

		Element host_hostname = host.getChild(HOSTNAMES).getChild(HOSTNAME);
		host_hostname.setStartElementListener(host_hostname_listener);

		host.getChild("smurf").setStartElementListener(smurf_listener);

		Element port = host.getChild(PORTS).getChild(PORT);
		port.setElementListener(port_listener);

		Element port_state = port.getChild(PORT_STATE);
		port_state.setStartElementListener( port_state_listener );

		Element port_service = port.getChild(PORT_SERVICE);
		port_service.setStartElementListener( port_service_listener );

		Element port_service_cpe = port_service.getChild(PORT_SERVICE_CPE);
		port_service_cpe.setEndTextElementListener( new EndTextElementListener() {
			@Override
			public void end(String s) {
				p.data.append("CPE: " + s + "\n");
			}
		});

		Element port_script = port.getChild(SCRIPT);
		port_script.setStartElementListener( port_script_listener );

		Element host_os = host.getChild(OS);
		host_os.setElementListener( host_os_listener );

		host_os.getChild("portused").setStartElementListener(host_os_portused_listener);

		Element host_os_class = host_os.getChild(OS_CLASS);
		host_os_class.setStartElementListener(host_os_class_listener);

		Element host_os_match = host_os.getChild(OS_MATCH);
		host_os_match.setStartElementListener( host_os_match_listener );

		Element host_os_fingerprint = host.getChild(OS_FINGERPRINT);
		host_os_fingerprint.setStartElementListener( host_os_fingerprint_listener );

		Element host_script = host.getChild(HOSTSCRIPT);
		host_script.setStartElementListener( host_script_listener );

		host.getChild("distance").setStartElementListener( host_distance_listener );

		host.getChild("uptime").setStartElementListener( host_uptime_listener );

		host.getChild("tcpsequence").setStartElementListener( tcpsequence_listener );

		host.getChild("ipidsequence").setStartElementListener( ipidsequence_listener );

		host.getChild("tcptssequence").setStartElementListener( tcptssequence_listener );

		Element trace = host.getChild("trace");
		trace.setElementListener( trace_listener );

		trace.getChild("hop").setStartElementListener( trace_hop_listener );

		host.getChild("times").setStartElementListener( times_listener );

		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {
			Log.d("UmitScanner.ParserFail",e.toString());
		}
	}

	StartElementListener runstats_finished_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			StringBuilder info = new StringBuilder();
			info.append(attributes.getValue("time")).append(' ')
					.append( attributes.getValue("timestr")).append(' ')
					.append( attributes.getValue("summary")).append(' ')
					.append( attributes.getValue("exit")).append(' ')
					.append('\n');
			String errormsg = attributes.getValue("errormsg");
			ContentValues values = new ContentValues();
			if(errormsg!=null){
				values.put(Scanner.Scans.ERRORMESSAGE,"Error: " + errormsg);
			} else {
				values.put(Scanner.Scans.ERRORMESSAGE,info.toString());
			}
			writer.writeScan(values);
		}
	};

	StartElementListener prescript_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String id = attributes.getValue(SCRIPT_AT_ID);
			String output = attributes.getValue(SCRIPT_AT_OUTPUT);

			prescript= new Host();
			prescript.IP="Prescript";
			prescript.state=Hosts.STATE_UP;
			writer.writeHost(prescript.IP, prescript.getContentValues());

			Detail detail = new Detail();
			detail.type="Prescript";
			detail.name=id;
			detail.data.append(output);
			detail.state=Details.STATE_PORT_UNKNOWN;
			writer.writeDetail(prescript.IP, detail.name, detail.getContentValues());
		}
	};

	StartElementListener postscript_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String id = attributes.getValue(SCRIPT_AT_ID);
			String output = attributes.getValue(SCRIPT_AT_OUTPUT);

			postscript.IP="Postscript";
			postscript.state=Hosts.STATE_UP;
			writer.writeHost(postscript.IP, postscript.getContentValues());

			Detail detail = new Detail();
			detail.type="Postscript";
			detail.name=id;
			detail.data.append(output);
			detail.state=Details.STATE_PORT_UNKNOWN;
			writer.writeDetail(postscript.IP, detail.name, detail.getContentValues());
		}
	};

	ElementListener host_listener = new ElementListener() {
		@Override
		public void start(Attributes attributes) {
			h = new Host();
			hostInfo = new Detail();
			hostInfo.type="Info";
			hostInfo.name="Info";
		}

		@Override
		public void end() {
			writer.writeHost(h.IP, h.getContentValues());

			if(h.state == Hosts.STATE_UP)
				writer.writeDetail(h.IP, hostInfo.name, hostInfo.getContentValues());
		}
	};

	StartElementListener host_status_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String state = attributes.getValue("state");
			String reason = attributes.getValue("reason");

			if(TextUtils.equals(state,"up")){
				h.state = Hosts.STATE_UP;
			} else if(TextUtils.equals(state,"down")) {
				h.state = Hosts.STATE_DOWN;
			} else if(TextUtils.equals(state,"unknown")) {
				h.state = Hosts.STATE_UNKNOWN;
			} else if(TextUtils.equals(state,"skipped")) {
				h.state = Hosts.STATE_SKIPPED;
			} else {
				h.state = Hosts.STATE_NULL;
			}

			if(reason!=null){
				hostInfo.data.append("Status Reason: ").append(reason).append("\n");
			}
		}
	};

	StartElementListener host_address_listener=new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String addr = attributes.getValue(ADDR);
			String type = attributes.getValue("addrtype");
			String vendor = attributes.getValue("vendor");

			if(TextUtils.isEmpty(h.IP)){
				h.IP= addr;
				//Must be here to initiate creation of details table
				writer.writeHost(h.IP,h.getContentValues());
			}

			if(addr!=null)
				hostInfo.data.append("Address:").append(addr);
			if(type!=null)
				hostInfo.data.append(" Type:").append(type);
			if(vendor!=null)
				hostInfo.data.append(" Vendor:").append(vendor);
			hostInfo.data.append('\n');
		}
	};

	StartElementListener host_hostname_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String name = attributes.getValue("name");
			String type = attributes.getValue("type");
			if(TextUtils.isEmpty(h.name)) {
				h.name=name;
			}

			if(name!=null)
				hostInfo.data.append("Hostname:").append(name);
			if(type!=null)
				hostInfo.data.append(" Type:").append(type);
			hostInfo.data.append('\n');
		}
	};

	StartElementListener smurf_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String smurf = attributes.getValue("responses");
			if(smurf!=null && smurf=="1")
				hostInfo.data.append("Host is VULNERABLE to smurf attack.\n");
		}
	};

	ElementListener port_listener = new ElementListener() {
		@Override
		public void start(Attributes attributes) {
			String protocol = attributes.getValue(PORT_AT_PROTOCOL);
			String port_id = attributes.getValue(PORT_AT_ID);
			p = new Detail();
			p.name = port_id + ":" + protocol;
			writer.writeDetail(h.IP, p.name, p.getContentValues());
			String owner = attributes.getValue("owner");
			if(owner!=null)
				p.data.append("Owner:").append(owner).append('\n');
		}
		@Override
		public void end() {
			writer.writeDetail(h.IP, p.name, p.getContentValues());
			p = null;
		}
	};

	StartElementListener port_state_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String ps = attributes.getValue(PORT_STATE_AT_STATE);
			String reason = attributes.getValue("reason");
			String reason_ttl = attributes.getValue("reason_ttl");
			String reason_ip = attributes.getValue("reason_ip");
			if(TextUtils.equals(ps,"open")){
				p.state=Details.STATE_PORT_OPEN;
			} else if(TextUtils.equals(ps,"filtered")) {
				p.state=Details.STATE_PORT_FILTERED;
			} else if(TextUtils.equals(ps,"unfiltered")) {
				p.state=Details.STATE_PORT_UNFILTERED;
			} else if(TextUtils.equals(ps,"closed")) {
				p.state=Details.STATE_PORT_CLOSED;
			} else if(TextUtils.equals(ps,"open|filtered")) {
				p.state=Details.STATE_PORT_OPENFILTERED;
			} else if(TextUtils.equals(ps,"closed|filtered")) {
				p.state=Details.STATE_PORT_CLOSEDFILTERED;
			} else if(TextUtils.equals(ps,"unknown")) {
				p.state=Details.STATE_PORT_UNKNOWN;
			}

			p.type="Port";

			if(reason!=null)
				p.data.append("Reason:").append(reason);
			if(reason_ttl!=null)
				p.data.append(" Reason_TTL:").append(reason_ttl);
			if(reason_ip!=null)
				p.data.append(" Reason_IP:").append(reason_ip);
			p.data.append('\n');

		}
	};

	StartElementListener port_service_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {

			if(attributes.getValue(PORT_SERVICE_AT_NAME)!=null)
				p.data.append("Name: ").append(attributes.getValue(PORT_SERVICE_AT_NAME)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_CONFIDENCE)!=null)
				p.data.append("Confidence: ").append(attributes.getValue(PORT_SERVICE_AT_CONFIDENCE)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_METHOD)!=null)
				p.data.append("Method: " ).append( attributes.getValue(PORT_SERVICE_AT_METHOD)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_VERSION)!=null)
				p.data.append("Version: ").append( attributes.getValue(PORT_SERVICE_AT_VERSION)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_PRODUCT)!=null)
				p.data.append("Product: " ).append( attributes.getValue(PORT_SERVICE_AT_PRODUCT)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_EXTRAINFO)!=null)
				p.data.append("Extrainfo: " ).append( attributes.getValue(PORT_SERVICE_AT_EXTRAINFO)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_HOSTNAME)!=null)
				p.data.append("Hostname: " ).append( attributes.getValue(PORT_SERVICE_AT_HOSTNAME)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_OSTYPE)!=null)
				p.data.append("OSType: " ).append( attributes.getValue(PORT_SERVICE_AT_OSTYPE)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_DEVICETYPE)!=null)
				p.data.append("DeviceType: " ).append( attributes.getValue(PORT_SERVICE_AT_DEVICETYPE)).append("\n");
			if(attributes.getValue(PORT_SERVICE_AT_SERVICEFP)!=null)
				p.data.append("ServiceFP: " ).append( attributes.getValue(PORT_SERVICE_AT_SERVICEFP)).append("\n");

		}
	};

	StartElementListener port_script_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			p.data.append("Script ").append(attributes.getValue(SCRIPT_AT_ID)).append(":").append(
					attributes.getValue(SCRIPT_AT_OUTPUT)).append("\n");
		}
	};

	ElementListener host_os_listener = new ElementListener() {
		@Override
		public void end() {
			writer.writeDetail(h.IP, hostOS.name, hostOS.getContentValues());
			hostOS=null;
		}
		@Override
		public void start(Attributes attributes) {
			hostOS = new Detail();
			hostOS.name = "OS";
		}
	};

	StartElementListener host_os_portused_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String portid = attributes.getValue("portid");
			String proto = attributes.getValue("proto");
			String state = attributes.getValue("state");

			hostOS.data.append("Port used: ");

			if(portid!=null)
				hostOS.data.append(portid);
			if(proto!=null)
				hostOS.data.append(':').append(proto);
			if(state!=null)
				hostOS.data.append(":").append(state);
			hostOS.data.append('\n');
		}
	};

	StartElementListener host_os_class_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {

			if(attributes.getValue(OS_CLASS_AT_VENDOR)!=null)
				hostOS.data.append("Vendor: ").append(attributes.getValue(OS_CLASS_AT_VENDOR)).append("\n");
			if(attributes.getValue(OS_CLASS_AT_OSGEN)!=null)
				hostOS.data.append("OSGEN: ").append(attributes.getValue(OS_CLASS_AT_OSGEN)).append("\n");
			if(attributes.getValue(OS_CLASS_AT_TYPE)!=null)
				hostOS.data.append("Type: ").append(attributes.getValue(OS_CLASS_AT_TYPE)).append("\n");
			if(attributes.getValue(OS_CLASS_AT_ACCURACY)!=null)
				hostOS.data.append("Accuracy: ").append(attributes.getValue(OS_CLASS_AT_ACCURACY)).append("\n");
			if(attributes.getValue(OS_CLASS_AT_FAMILY)!=null)
				hostOS.data.append("Family: ").append(attributes.getValue(OS_CLASS_AT_FAMILY)).append("\n");

			String osFamily = attributes.getValue(OS_CLASS_AT_FAMILY);
			if(osFamily==null)
				h.OS = Hosts.OS_UNKNOWN;
			else if(TextUtils.equals(osFamily,"Linux"))
				h.OS = Hosts.OS_LINUX;
			else if(TextUtils.equals(osFamily,"Windows"))
				h.OS = Hosts.OS_WINDOWS;
			else if(TextUtils.equals(osFamily,"OpenBSD"))
				h.OS = Hosts.OS_OPENBSD;
			else if(TextUtils.equals(osFamily,"FreeBSD"))
				h.OS = Hosts.OS_FREEBSD;
			else if(TextUtils.equals(osFamily,"NetBSD"))
				h.OS = Hosts.OS_NETBSD;
			else if(TextUtils.equals(osFamily,"Solaris"))
				h.OS = Hosts.OS_SOLARIS;
			else if(TextUtils.equals(osFamily,"OpenSolaris"))
				h.OS = Hosts.OS_SOLARIS;
			else if(TextUtils.equals(osFamily,"IRIX"))
				h.OS = Hosts.OS_IRIX;
			else if(TextUtils.equals(osFamily,"Mac OS X"))
				h.OS = Hosts.OS_MACOSX;
			else if(TextUtils.equals(osFamily,"Mac OS"))
				h.OS = Hosts.OS_MACOSX;

		}
	};

	StartElementListener host_os_match_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			if(attributes.getValue(OS_MATCH_AT_NAME)!=null)
				hostOS.data.append("Name: ").append(attributes.getValue(OS_MATCH_AT_NAME)).append("\n");
			if(attributes.getValue(OS_MATCH_AT_ACCURACY)!=null)
				hostOS.data.append("Accuracy: ").append(attributes.getValue(OS_MATCH_AT_ACCURACY)).append("\n");
			if(attributes.getValue(OS_MATCH_AT_LINE)!=null)
				hostOS.data.append("Line: ").append(attributes.getValue(OS_MATCH_AT_LINE)).append("\n");

			String osMatch = attributes.getValue(OS_MATCH_AT_NAME);
			if(h.OS==Hosts.OS_LINUX && osMatch!=null){
				if(osMatch.toLowerCase().contains("ubuntu"))
					h.OS = Hosts.OS_UBUNTU;
				else if(osMatch.toLowerCase().contains("red hat"))
					h.OS = Hosts.OS_REDHAT;
			}
		}
	};

	StartElementListener host_os_fingerprint_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			hostOS.data.append("Fingerprint: ")
					.append(attributes.getValue(OS_FINGERPRINT_AT_FINGERPRINT)).append("\n");
		}
	};

	StartElementListener host_script_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			hostScript = new Detail();
			hostScript.name = attributes.getValue(SCRIPT_AT_ID);
			hostScript.data.append(attributes.getValue(SCRIPT_AT_OUTPUT)).append("\n");
			writer.writeDetail(h.IP, hostScript.name, hostScript.getContentValues());
			hostScript=null;
		}
	};

	StartElementListener host_distance_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String distance = attributes.getValue("value");
			if(distance!=null)
				hostInfo.data.append("Distance=").append(distance).append('\n');
		}
	};

	StartElementListener host_uptime_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String uptime = attributes.getValue("seconds");
			if(uptime!=null)
				hostInfo.data.append("Uptime=").append(uptime).append(" sec.\n");
		}
	};

	StartElementListener tcpsequence_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String index = attributes.getValue("index");
			String difficulty = attributes.getValue("difficulty");
			String values = attributes.getValue("values");

			if(index==null && difficulty==null && values==null)
				return;

			hostInfo.data.append("TCP Sequence:");
			if(index!=null)
				hostInfo.data.append(" Idx=").append(index);
			if(difficulty!=null)
				hostInfo.data.append(" Difficulty=").append(difficulty);
			if(values!=null)
				hostInfo.data.append(" Values:").append(values);
			hostInfo.data.append('\n');

		}
	};

	StartElementListener ipidsequence_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String ipid_class = attributes.getValue("class");
			String ipid_values = attributes.getValue("values");

			if(ipid_class==null && ipid_values==null)
				return;

			hostInfo.data.append("IPID Sequence:");

			if(ipid_class!=null)
				hostInfo.data.append(" Class=").append(ipid_class);
			if(ipid_values!=null)
				hostInfo.data.append(" Values=").append(ipid_values);

			hostInfo.data.append('\n');
		}
	};

	StartElementListener tcptssequence_listener =new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String tcpts_class = attributes.getValue("class");
			String tcpts_values = attributes.getValue("values");

			if(tcpts_class==null && tcpts_values==null)
				return;

			hostInfo.data.append("TCPTS Sequence:");

			if(tcpts_class!=null)
				hostInfo.data.append(" Class=").append(tcpts_class);
			if(tcpts_values!=null)
				hostInfo.data.append(" Values=").append(tcpts_values);

			hostInfo.data.append('\n');
		}
	};

	ElementListener trace_listener = new ElementListener() {
		@Override
		public void end() {
			writer.writeDetail(h.IP, hostTrace.name, hostTrace.getContentValues());
			hostTrace=null;
		}

		@Override
		public void start(Attributes attributes) {
			hostTrace = new Detail();
			hostTrace.name = "Traceroute";
			hostTrace.type = "Traceroute";
			String trace_proto = attributes.getValue("proto");
			String trace_port = attributes.getValue("port");

			if(trace_port!=null)
				hostTrace.data.append("Port:").append(trace_port).append(' ');
			if(trace_proto!=null)
				hostTrace.data.append("Protocol:").append(trace_proto).append(' ');

			if(trace_port!=null || trace_proto!=null)
				hostTrace.data.append('\n');
		}
	};

	StartElementListener trace_hop_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String ttl = attributes.getValue("ttl");
			String rtt = attributes.getValue("rtt");
			String ipaddr = attributes.getValue("ipaddr");
			String thost = attributes.getValue("host");

			if(ttl==null && rtt==null && ipaddr==null && thost==null)
				return;

			hostTrace.data.append("Hop: ");
			if(ttl!=null)
				hostTrace.data.append(ttl).append(' ');
			if(ipaddr!=null)
				hostTrace.data.append(ipaddr).append(' ');
			if(rtt!=null)
				hostTrace.data.append(rtt).append(' ');
			if(thost!=null)
				hostTrace.data.append(thost);

			hostTrace.data.append('\n');
		}
	};

	StartElementListener times_listener = new StartElementListener() {
		@Override
		public void start(Attributes attributes) {
			String srtt = attributes.getValue("srtt");
			String rttvar = attributes.getValue("rttvar");
			String to = attributes.getValue("to");

			if(srtt==null && rttvar==null && to==null)
				return;

			hostInfo.data.append("Times: ");
			if(srtt!=null)
				hostInfo.data.append("SRTT:").append(srtt).append(' ');
			if(rttvar!=null)
				hostInfo.data.append("RTTVar:").append(rttvar).append(' ');
			if(to!=null)
				hostInfo.data.append("To:").append(to).append(' ');

			hostInfo.data.append('\n');
		}
	};





}
