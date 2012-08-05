package org.umit.ns.mobile;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import android.widget.TextView;
import org.umit.ns.mobile.provider.Scanner.Details;


public class HostDetails extends Activity {

	Uri hostDetailsUri;
	String host;

	SimpleCursorAdapter hostDetailsAdapter;
	Cursor d;

	ListView hostDetailsListView;
	TextView detailsItemTextView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_details_activity);
		hostDetailsUri = getIntent().getData();
		host = hostDetailsUri.getPathSegments().get(3);
		setTitle(host);

		hostDetailsListView = (ListView)findViewById(R.id.hostDetails);
		detailsItemTextView = (TextView)findViewById(R.id.hostDetailsItem);

		String[] detailsFromColumns = {Details.NAME };
		int[] detailsToViews = {R.id.port_listview_item};
		d = getContentResolver().query(hostDetailsUri,null,null,null,null);
		hostDetailsAdapter = new SimpleCursorAdapter(this,R.layout.port_item,
				d,detailsFromColumns,detailsToViews);
		hostDetailsListView.setAdapter(hostDetailsAdapter);
		hostDetailsListView.setEnabled(true);
		hostDetailsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				String detailName = ((TextView)view).getText().toString();
				Uri detailUri = hostDetailsUri.buildUpon().appendPath(detailName).build();
				Cursor detailItemCursor = getContentResolver().query(detailUri,null,null,null,null);
				detailItemCursor.moveToFirst();
				detailsItemTextView.setText(
						detailItemCursor.getString(
								detailItemCursor.getColumnIndex(Details.DATA)));
			}
		});
	}

	private static void log(String logString){
		if(logString!=null)
			Log.d("UmitScanner.HostDetails", logString);
		else
			Log.d("UmitScanner.HostDetails","NULL LOG STRING");
	}
}

