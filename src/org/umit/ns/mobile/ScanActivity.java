package org.umit.ns.mobile;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;


import org.umit.ns.mobile.api.ScanClientActivity;
import org.umit.ns.mobile.model.ScanArgsConst;
import org.umit.ns.mobile.model.ScanArgumentsTokenizer;
import org.umit.ns.mobile.model.ScanMultiAutoCompleteTextView;
import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.provider.Scanner.Hosts;
import org.umit.ns.mobile.provider.Scanner.Details;

public class ScanActivity extends ScanClientActivity implements ScanArgsConst{
	ScanMultiAutoCompleteTextView scanArgsTextView;
	Button actionButton;
	Spinner profilesSpinner;
	ListView hostsListView;
	ListView portsListView;

	static Uri hostsUri;
	static Uri scanUri;
	static Uri detailsUri;

	Cursor h;
	Cursor p;

	ContentResolver contentResolver;
	HostsListAdapter hostsAdapter;
	SimpleCursorAdapter portsAdapter;

	static int hostsColumnState;
	static int hostsColumnIP;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_activity);
		actionButton = (Button) findViewById(R.id.actionbutton);
		actionButton.setOnClickListener(startScan);
		scanArgsTextView = (ScanMultiAutoCompleteTextView) findViewById(R.id.scanarguments);
		profilesSpinner = (Spinner) findViewById(R.id.profiles);

		//Set up ScanArguments input
		ArrayAdapter<String> argsAdapter = new ArrayAdapter<String>(this, R.layout.scan_args_list_1item, FULL_ARGS);

		scanArgsTextView.setAdapter(argsAdapter);
		scanArgsTextView.setTokenizer(new ScanArgumentsTokenizer());

		//Set up hosts and ports ListView adapters
		hostsListView = (ListView) findViewById(R.id.hostsresults);
		hostsListView.setOnItemClickListener(hostClickListener);
		hostsListView.setEnabled(false);

		portsListView = (ListView) findViewById(R.id.portsresults);
		portsListView.setEnabled(false);

		String[] portsFromColumns = {Details.NAME };
		int[] portsToViews = {R.id.port_listview_item};
		portsAdapter = new SimpleCursorAdapter(this,R.layout.port_item,
				null,portsFromColumns,portsToViews);
		portsListView.setAdapter(portsAdapter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}



	public View.OnClickListener startScan = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			rqstStartScan("./nmap "+ scanArgsTextView.getText().toString());
		}
	};

	public View.OnClickListener stopScan = new View.OnClickListener(){
		@Override
		public void onClick(View view) {
			rqstStopScan();
		}
	};

	public View.OnClickListener clearResults = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			//Clear the database
			getContentResolver().delete(scanUri,null,null);

			hostsAdapter.changeCursor(null);
			hostsListView.setEnabled(false);
			stopManagingCursor(h);

			portsAdapter.changeCursor(null);
			portsListView.setEnabled(false);
			stopManagingCursor(p);

			actionButton.setText("Start");
			actionButton.setOnClickListener(startScan);;
		}
	};



	public void onScanStart(int clientID, int scanID) {
		scanUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/scans/"+clientID+"/"+scanID);
		hostsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/hosts/"+clientID+"/"+scanID);
		detailsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/details/"+clientID+"/"+scanID);

		actionButton.setText("Stop");
		actionButton.setOnClickListener(stopScan);
	}

	public void onScanStop() {
		actionButton.setText("Start");
		actionButton.setOnClickListener(startScan);
	}

	public void onNotifyProgress(int progress) {
	}

	protected void onNotifyProblem(String info) {
		Log.e("UmitScanner", "Scan has crashed. Info: " + info);
		Toast.makeText(getApplicationContext(), "Scanning problem: " + info, Toast.LENGTH_LONG).show();
	}

	public void onNotifyFinished() {
		actionButton.setText("Clear");
		actionButton.setOnClickListener(clearResults);

		//show results
		h = getContentResolver().query(hostsUri,null,null,null,null);
		hostsColumnIP = h.getColumnIndex(Hosts.IP);
		hostsColumnState = h.getColumnIndex(Hosts.STATE);
		startManagingCursor(h);
		hostsAdapter = new HostsListAdapter(this,h);

		hostsListView.setEnabled(true);
		hostsListView.setAdapter(hostsAdapter);

		portsListView.setEnabled(true);
	}

	AdapterView.OnItemClickListener hostClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			Uri singleDetailUri = detailsUri.buildUpon().appendPath(((TextView)view).getText().toString()).build();
			p = getContentResolver().query(singleDetailUri,null,null,null,null);
			startManagingCursor(p);
			portsAdapter.changeCursor(p);
		}
	};

	public static class HostsListAdapter extends CursorAdapter{
		public static final int BLACK_COLOR = 0xFF000000;
		public static final int RED_COLOR = 0xFF330000;
		public static final int GREEN_COLOR = 0xFF003300;

		public HostsListAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			final TextView view = (TextView) inflater.inflate(
					R.layout.host_item, parent, false);
			view.setText(cursor.getString(hostsColumnIP));
			switch (cursor.getInt(hostsColumnState)){
				case Hosts.STATE_UP:
					view.setBackgroundColor(GREEN_COLOR);
					break;
				case Hosts.STATE_DOWN:
					view.setBackgroundColor(RED_COLOR);
					break;
				default:
					view.setBackgroundColor(BLACK_COLOR);
			}
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view).setText(cursor.getString(hostsColumnIP));
			switch (cursor.getInt(hostsColumnState)){
				case Hosts.STATE_UP:
					view.setBackgroundColor(GREEN_COLOR);
					break;
				case Hosts.STATE_DOWN:
					view.setBackgroundColor(RED_COLOR);
					break;
				default:
					view.setBackgroundColor(BLACK_COLOR);
			}
		}

		@Override
		public String convertToString(Cursor cursor) {
			return cursor.getString(hostsColumnIP);
		}
	}
}