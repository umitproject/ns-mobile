package org.umit.ns.mobile.xml;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.text.TextUtils;
import org.umit.ns.mobile.provider.Scanner.Hosts;

import java.util.ArrayList;

class Host {
	Host(){}
	public String IP="";
	public String name = "";
	public int OS = Hosts.OS_UNKNOWN; //Unknown
	public int state = Hosts.STATE_NULL; //Unknown

	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(Hosts.IP,IP);
		if( ! TextUtils.isEmpty(name))
			values.put(Hosts.NAME,name);

		values.put(Hosts.OS,OS);
		values.put(Hosts.STATE,state);
		return values;
	}

}
