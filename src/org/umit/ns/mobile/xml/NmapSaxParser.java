package org.umit.ns.mobile.xml;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.sax.*;
import android.sax.RootElement;
import android.text.TextUtils;
import android.util.Xml;

import org.umit.ns.mobile.provider.Scanner.Scans;
import org.umit.ns.mobile.provider.Scanner.Hosts;
import org.umit.ns.mobile.provider.Scanner.Details;

import org.xml.sax.Attributes;

public class NmapSaxParser extends BaseNmapXmlParser {

	public Host h;
	public Detail d;

	public NmapSaxParser(ContentResolver contentResolver,
	                     String clientID, String scanID,
	                     String scanResultsFilename) {
		super(contentResolver,clientID,scanID,scanResultsFilename);
	}

	public void parse() {
		RootElement root = new RootElement(ROOT);

		root.getChild(TASK_BEGIN)
				.setStartElementListener( new StartElementListener () {
			@Override
			public void start(Attributes attributes) {
				String task = attributes.getValue(TASK);
				String progress = "0";
				ContentValues values = new ContentValues();
				values.put(Scans.TASK,task);
				values.put(Scans.TASK_PROGRESS,progress);
				writer.writeScan(values);
			}
		});

		root.getChild(TASK_PROGRESS)
				.setStartElementListener(new StartElementListener() {
					@Override
					public void start(Attributes attributes) {
						String task = attributes.getValue(TASK);
						String progress = attributes.getValue(PERCENT);
						ContentValues values = new ContentValues();
						values.put(Scans.TASK,task);
						values.put(Scans.TASK_PROGRESS,progress);
						writer.writeScan(values);
					}
				});

		root.getChild(TASK_END)
				.setStartElementListener(new StartElementListener() {
					@Override
					public void start(Attributes attributes) {
						String task = attributes.getValue(TASK);
						String progress = "100";
						ContentValues values = new ContentValues();
						values.put(Scans.TASK,task);
						values.put(Scans.TASK_PROGRESS,progress);
						writer.writeScan(values);
					}
				});

		//TODO add support for prescript and postscript
//		root.getChild(PRESCRIPT)
//				.setStartElementListener(new StartElementListener() {
//					@Override
//					public void start(Attributes attributes) {
//						String id = attributes.getValue(SCRIPT_AT_ID);
//						String output = attributes.getValue(SCRIPT_AT_OUTPUT);
//						Host host = new Host()
//					}
//				});
//
//		root.getChild(POSTSCRIPT)
//				.setStartElementListener(new StartElementListener() {
//					@Override
//					public void start(Attributes attributes) {
//						String id = attributes.getValue(SCRIPT_AT_ID);
//						String output = attributes.getValue(SCRIPT_AT_OUTPUT);
//					}
//				});


		Element host = root.getChild(HOST);
		host.setElementListener(new ElementListener() {
			@Override
			public void start(Attributes attributes) {
				h = new Host();
			}

			@Override
			public void end() {
				writer.writeHost(h.IP, h.getContentValues());
				d = null;
				h = null;
			}
		});

		Element host_status = host.getChild(STATUS);
		host_status.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				String state = attributes.getValue(STATUS_AT_STATE);
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
			}
		});

		Element host_address = host.getChild(ADDRESS);
		host_address.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				h.IP=attributes.getValue(ADDR);
				writer.writeHost(h.IP,h.getContentValues());
			}
		});

		//TODO Add support for multiple hostnames
		Element host_hostname = host.getChild(HOSTNAMES).getChild(HOSTNAME);
		host_hostname.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(TextUtils.isEmpty(h.name)) {
					h.name=attributes.getValue("name");
					writer.writeHost(h.IP,h.getContentValues());
				}
			}
		});

		Element port = host.getChild(PORTS).getChild(PORT);
		port.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				String protocol = attributes.getValue(PORT_AT_PROTOCOL);
				String port_id = attributes.getValue(PORT_AT_ID);
				d = new Detail();
				d.name = protocol+" : "+port_id;
				writer.writeDetail(h.IP, d.name, d.getContentValues());
			}
		});
		port.setEndElementListener( new EndElementListener() {
			@Override
			public void end() {
				d = null;
			}
		});

		Element port_state = port.getChild(PORT_STATE);
		port_state.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				String ps = attributes.getValue("state");
				if(TextUtils.equals(ps,"open")){
					d.state=Details.STATE_PORT_OPEN;
				} else if(TextUtils.equals(ps,"filtered")) {
					d.state=Details.STATE_PORT_FILTERED;
				} else if(TextUtils.equals(ps,"unfiltered")) {
					d.state=Details.STATE_PORT_UNFILTERED;
				} else if(TextUtils.equals(ps,"closed")) {
					d.state=Details.STATE_PORT_CLOSED;
				} else if(TextUtils.equals(ps,"open|filtered")) {
					d.state=Details.STATE_PORT_OPENFILTERED;
				} else if(TextUtils.equals(ps,"closed|filtered")) {
					d.state=Details.STATE_PORT_CLOSEDFILTERED;
				} else if(TextUtils.equals(ps,"unknown")) {
					d.state=Details.STATE_PORT_UNKNOWN;
				}
				d.type="Port";
				writer.writeDetail(h.IP, d.name, d.getContentValues());
			}
		});

		Element port_service = port.getChild(PORT_SERVICE);
		port_service.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				StringBuilder data = new StringBuilder();

				if(attributes.getValue(PORT_SERVICE_AT_NAME)!=null)
					data.append("Name: ").append(attributes.getValue(PORT_SERVICE_AT_NAME)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_CONFIDENCE)!=null)
					data.append("Confidence: ").append(attributes.getValue(PORT_SERVICE_AT_CONFIDENCE)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_METHOD)!=null)
					data.append("Method: " ).append( attributes.getValue(PORT_SERVICE_AT_METHOD)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_VERSION)!=null)
					data.append("Version: ").append( attributes.getValue(PORT_SERVICE_AT_VERSION)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_PRODUCT)!=null)
					data.append("Product: " ).append( attributes.getValue(PORT_SERVICE_AT_PRODUCT)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_EXTRAINFO)!=null)
					data.append("Extrainfo: " ).append( attributes.getValue(PORT_SERVICE_AT_EXTRAINFO)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_HOSTNAME)!=null)
					data.append("Hostname: " ).append( attributes.getValue(PORT_SERVICE_AT_HOSTNAME)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_OSTYPE)!=null)
					data.append("OSType: " ).append( attributes.getValue(PORT_SERVICE_AT_OSTYPE)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_DEVICETYPE)!=null)
					data.append("DeviceType: " ).append( attributes.getValue(PORT_SERVICE_AT_DEVICETYPE)).append("\n");
				if(attributes.getValue(PORT_SERVICE_AT_SERVICEFP)!=null)
					data.append("ServiceFP: " ).append( attributes.getValue(PORT_SERVICE_AT_SERVICEFP)).append("\n");
				d.data += data.toString();
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});
		Element port_service_cpe = port_service.getChild(PORT_SERVICE_CPE);
		port_service_cpe.setEndTextElementListener( new EndTextElementListener() {
			@Override
			public void end(String s) {
				d.data+="CPE: " + s +"\n";
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});

		Element port_script = port.getChild(SCRIPT);
		port_script.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				d.data += "Script " + attributes.getValue(SCRIPT_AT_ID) + ":" +
						attributes.getValue(SCRIPT_AT_OUTPUT) + "\n";
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});

		Element host_os = host.getChild(OS);
		host_os.setElementListener( new ElementListener() {
			@Override
			public void end() {
				writer.writeDetail(h.IP,d.name,d.getContentValues());
				d=null;
			}
			@Override
			public void start(Attributes attributes) {
				d = new Detail();
				d.name = "OS";
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});

		Element host_os_class = host.getChild(OS_CLASS);
		host_os_class.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				StringBuilder data = new StringBuilder();
				
				if(attributes.getValue(OS_CLASS_AT_VENDOR)!=null)
					data.append("Vendor: ").append(attributes.getValue(OS_CLASS_AT_VENDOR)).append("\n");
				if(attributes.getValue(OS_CLASS_AT_OSGEN)!=null)
					data.append("OSGEN: ").append(attributes.getValue(OS_CLASS_AT_OSGEN)).append("\n");
				if(attributes.getValue(OS_CLASS_AT_TYPE)!=null)
					data.append("Type: ").append(attributes.getValue(OS_CLASS_AT_TYPE)).append("\n");
				if(attributes.getValue(OS_CLASS_AT_ACCURACY)!=null)
					data.append("Accuracy: ").append(attributes.getValue(OS_CLASS_AT_ACCURACY)).append("\n");
				if(attributes.getValue(OS_CLASS_AT_FAMILY)!=null)
					data.append("Family: ").append(attributes.getValue(OS_CLASS_AT_FAMILY)).append("\n");

				d.data += data.toString();
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});
		Element host_os_match = host.getChild(OS_MATCH);
		host_os_match.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				StringBuilder data = new StringBuilder();

				if(attributes.getValue(OS_MATCH_AT_NAME)!=null)
					data.append("Name: ").append(attributes.getValue(OS_MATCH_AT_NAME)).append("\n");
				if(attributes.getValue(OS_MATCH_AT_ACCURACY)!=null)
					data.append("Accuracy: ").append(attributes.getValue(OS_MATCH_AT_ACCURACY)).append("\n");
				if(attributes.getValue(OS_MATCH_AT_LINE)!=null)
					data.append("Line: ").append(attributes.getValue(OS_MATCH_AT_LINE)).append("\n");

				d.data += data.toString();
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});

		Element host_os_fingerprint = host.getChild(OS_FINGERPRINT);
		host_os_fingerprint.setStartElementListener( new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				d.data += "Fingerprint: "+attributes.getValue(OS_FINGERPRINT_AT_FINGERPRINT)+"\n";
				writer.writeDetail(h.IP,d.name,d.getContentValues());
			}
		});

		Element host_script = host.getChild(HOSTSCRIPT);
		host_script.setElementListener( new ElementListener() {
			@Override
			public void end() {
			}

			@Override
			public void start(Attributes attributes) {
				d = new Detail();
				d.name = attributes.getValue(SCRIPT_AT_ID);
				d.data = attributes.getValue(SCRIPT_AT_OUTPUT)+"\n";
				writer.writeDetail(h.IP,d.name,d.getContentValues());
				d=null;
			}
		});

		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
