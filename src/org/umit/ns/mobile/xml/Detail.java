package org.umit.ns.mobile.xml;

import android.content.ContentValues;
import android.text.TextUtils;
import org.umit.ns.mobile.provider.Scanner.Details;


class Detail{
	Detail(){}
	public String type = "";
	public String name = "";
	public int state = -1;
	public StringBuilder data = new StringBuilder();

	public ContentValues getContentValues() {
		ContentValues values= new ContentValues();

		if( ! TextUtils.isEmpty(type))
			values.put(Details.TYPE,type);

		if( ! TextUtils.isEmpty(name))
			values.put(Details.NAME,name);

		if(state != -1)
			values.put(Details.STATE,state);

		if( ! TextUtils.isEmpty(data.toString()))
			values.put(Details.DATA,data.toString());

		return values;
	}

}
