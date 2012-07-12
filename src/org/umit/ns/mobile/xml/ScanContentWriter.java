package org.umit.ns.mobile.xml;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.provider.Scanner.Scans;
import org.umit.ns.mobile.provider.Scanner.Hosts;
import org.umit.ns.mobile.provider.Scanner.Details;

class ScanContentWriter {
	ScanContentWriter(ContentResolver contentResolver,
	                  String clientIDString,
	                  String scanIDString){
		this.contentResolver=contentResolver;
		this.clientID = Integer.valueOf(clientIDString);
		this.scanID = Integer.valueOf(scanIDString);
	}
	private ContentResolver contentResolver;
	private int clientID;
	private int scanID;

	public void writeScan(ContentValues values){
		Uri scanUri = Uri.parse(Scanner.SCANS_URI+"/"+clientID+"/"+scanID);
		contentResolver.update(scanUri,values,null,null);
	}
	public void writeHost(String hostIP, ContentValues values){
		Uri hostUri = Uri.parse(Scanner.HOSTS_URI+"/"+clientID+"/"+scanID+"/" + hostIP);
		contentResolver.update(hostUri,values,null,null);
	}
	public void writeDetail(String hostIP, String detailName, ContentValues values){
		Uri detailUri = Uri.parse(Scanner.DETAILS_URI+"/"+clientID+"/"+scanID+"/"+ hostIP + "/" + detailName);
		contentResolver.update(detailUri,values,null,null);
	}

}
