package org.umit.ns.mobile;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;


import org.umit.ns.mobile.api.ScanClientActivity;
import org.umit.ns.mobile.model.ScanArgsConst;
import org.umit.ns.mobile.model.ScanArgumentsTokenizer;
import org.umit.ns.mobile.model.ScanMultiAutoCompleteTextView;
import org.umit.ns.mobile.provider.Scanner.Hosts;
import org.umit.ns.mobile.provider.Scanner.Details;

public class ScanActivity extends ScanClientActivity implements ScanArgsConst{
	ScanMultiAutoCompleteTextView scanArgsTextView;
	Button startButton;
	Button clearResultsButton;
	Spinner profilesSpinner;
	ListView hostsListView;
	ListView portsListView;
	boolean started = false;
	boolean finished = false;

	Uri hostsUri;
	Uri scanUri;
	Uri detailsUri;

	Cursor h;
	Cursor p;

	ContentResolver contentResolver;
	SimpleCursorAdapter hostsAdapter;
	SimpleCursorAdapter portsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_activity);
		startButton = (Button) findViewById(R.id.startscan);
		clearResultsButton= (Button) findViewById(R.id.clearresults);
		scanArgsTextView = (ScanMultiAutoCompleteTextView) findViewById(R.id.scanarguments);
		profilesSpinner = (Spinner) findViewById(R.id.profiles);
		hostsListView = (ListView) findViewById(R.id.hostsresults);
		portsListView = (ListView) findViewById(R.id.portsresults);

		//Set up ScanArguments input
		ArrayAdapter<String> argsAdapter = new ArrayAdapter<String>(this,R.layout.scan_args_list_1item, FULL_ARGS);

		scanArgsTextView.setAdapter(argsAdapter);
		scanArgsTextView.setTokenizer(new ScanArgumentsTokenizer());

		//Set up hosts and ports ListView adapters
		String[] hostFromColumns = { Hosts.IP };
		int[] hostToViews = {R.id.host_listview_item};

		String[] portsFromColumns = {Details.NAME };
		int[] portsToViews = {R.id.port_listview_item};

		hostsAdapter = new SimpleCursorAdapter(this, R.layout.host_item,
				null,	hostFromColumns, hostToViews);
		hostsListView.setAdapter(hostsAdapter);
		hostsListView.setOnItemClickListener(hostClickListener);
		hostsListView.setEnabled(false);

		portsAdapter = new SimpleCursorAdapter(this,R.layout.port_item,
				null,portsFromColumns,portsToViews);
		portsListView.setAdapter(portsAdapter);
		portsListView.setEnabled(false);


	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void startScan(View view) {
		if(!started)
			rqstStartScan("./ nmap"+ scanArgsTextView.getText().toString());
		else if(!finished){
			rqstStopScan();
		}
	}

	public void onScanStart(int clientID, int scanID) {
		hostsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/hosts/"+clientID+"/"+scanID);
		scanUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/scans/"+clientID+"/"+scanID);
		detailsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/details/"+clientID+"/"+scanID);
		started = true;
		startButton.setText("Stop");
	}

	public void onScanStop() {
		started=false;
		startButton.setText("Start");
	}

	public void onNotifyProgress(int progress) {
	}

	protected void onNotifyProblem(String info) {
		Log.e("UmitScanner", "Scan has crashed. Info: " + info);
		Toast.makeText(getApplicationContext(), "Scanning problem: " + info, Toast.LENGTH_LONG).show();
	}

	public void onNotifyFinished() {
		finished=true;
		startButton.setText("Finished");
		startButton.setEnabled(false);
		//show results
		h = getContentResolver().query(hostsUri,null,null,null,null);
		hostsListView.setEnabled(true);
		hostsAdapter.changeCursor(h);
		startManagingCursor(h);
		portsListView.setEnabled(true);
	}

	public void clearResults(View view) {
		//TODO Clear database
		hostsAdapter.changeCursor(null);
		hostsListView.setEnabled(false);
		stopManagingCursor(h);

		portsAdapter.changeCursor(null);
		portsListView.setEnabled(false);
		stopManagingCursor(p);

		startButton.setText("Start");
		startButton.setEnabled(true);
		started=false;
		finished=false;
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
}