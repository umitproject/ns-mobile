package org.umit.ns.mobile.xml;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.*;

public abstract class BaseNmapXmlParser {
	protected String scanResultsFilename;
	public static ScanContentWriter writer;

	//Nmap XML tags
	static final String ROOT = "nmaprun";

	static final String TASK_BEGIN = "taskbegin";
	static final String TASK_PROGRESS = "taskprogress";
	static final String TASK_END = "taskend";
		static final String TASK = "task";
		static final String PERCENT = "percent";

	static final String PRESCRIPT = "prescript";
	//CONTAINS SCRIPT
	static final String POSTSCRIPT = "postscript";
	//CONTAINS SCRIPT
	static final String HOST = "host";
		static final String STATUS = "status";
			static final String STATUS_AT_STATE = "state";
			static final String STATUS_AT_REASON = "reason";
		static final String ADDRESS = "address";
			static final String ADDR = "addr";
		static final String HOSTNAMES="hostnames";
			static final String HOSTNAME ="hostname";
		static final String PORTS="ports";
			static final String PORT = "port";
				static final String PORT_AT_ID="portid";
				static final String PORT_AT_PROTOCOL = "protocol";
				static final String PORT_STATE="state";
					static final String PORT_STATE_AT_STATE="state";
				static final String PORT_SERVICE = "service";
					static final String PORT_SERVICE_AT_NAME="name";
					static final String PORT_SERVICE_AT_CONFIDENCE="conf";
					static final String PORT_SERVICE_AT_METHOD="method";
					static final String PORT_SERVICE_AT_VERSION="version";
					static final String PORT_SERVICE_AT_PRODUCT="product";
					static final String PORT_SERVICE_AT_EXTRAINFO="extrainfo";
					static final String PORT_SERVICE_AT_HOSTNAME="hostname";
					static final String PORT_SERVICE_AT_OSTYPE="ostype";
					static final String PORT_SERVICE_AT_DEVICETYPE="devicetype";
					static final String PORT_SERVICE_AT_SERVICEFP="servicefp";
					static final String PORT_SERVICE_CPE="cpe";
				static final String SCRIPT = "script";
					static final String SCRIPT_AT_ID = "id";
					static final String SCRIPT_AT_OUTPUT = "output";
		static final String OS="os";
			static final String OS_CLASS="osclass";
				static final String OS_CLASS_AT_VENDOR="vendor";
				static final String OS_CLASS_AT_OSGEN="osgen";
				static final String OS_CLASS_AT_TYPE="type";
				static final String OS_CLASS_AT_ACCURACY="accuracy";
				static final String OS_CLASS_AT_FAMILY="osfamily";
			static final String OS_MATCH="osmatch";
				static final String OS_MATCH_AT_NAME="name";
				static final String OS_MATCH_AT_ACCURACY="accuracy";
				static final String OS_MATCH_AT_LINE="line";
			static final String OS_FINGERPRINT="osfingerprint";
				static final String OS_FINGERPRINT_AT_FINGERPRINT="fingerprint";
		static final String HOSTSCRIPT="hostscript";
			//CONTAINS SCRIPT

	protected BaseNmapXmlParser(ContentResolver contentResolver,String clientID, String scanID,String scanResultsFilename){
		this.scanResultsFilename = scanResultsFilename;
		writer = new ScanContentWriter(contentResolver,clientID,scanID);
	}

	protected InputStream getInputStream() {
		try {
			return new BufferedInputStream(new FileInputStream(scanResultsFilename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	abstract void parse();
}
