package org.umit.ns.mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;


import org.umit.ns.mobile.api.ScanClientActivity;
import org.umit.ns.mobile.model.ScanArgsConst;
import org.umit.ns.mobile.model.ScanArgumentsTokenizer;
import org.umit.ns.mobile.model.ScanMultiAutoCompleteTextView;
import org.umit.ns.mobile.provider.Scanner.Scans;
import org.umit.ns.mobile.provider.Scanner.Hosts;
import org.umit.ns.mobile.provider.Scanner.Details;

import java.util.List;
import java.util.Map;
import java.util.Set;

//TODO check script output in ports listview

public class ScanActivity extends ScanClientActivity implements ScanArgsConst{
	ScanMultiAutoCompleteTextView scanArgsTextView;
	Button actionButton;
	Button saveProfileButton;
	Button deleteProfileButton;
	Spinner profilesSpinner;
	ListView hostsListView;
	ListView portsListView;
	TextView taskName;
	ProgressBar progressBar;

	static Uri hostsUri;
	static Uri scanUri;
	static Uri detailsUri;

	Cursor h;
	Cursor p;
	Cursor s;

	HostsListAdapter hostsAdapter;
	SimpleCursorAdapter portsAdapter;

	static int hostsColumnState;
	static int hostsColumnIP;

	ScanContentObserver scanContentObserver;

	Map<String,?> profiles;
	ArrayAdapter<CharSequence> profilesAdapter;

	@Override
	protected void onNewIntent (Intent intent){
		super.onNewIntent(intent);
		this.setIntent(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_activity);

		actionButton = (Button) findViewById(R.id.actionbutton);
		actionButton.setOnClickListener(startScan);

		scanArgsTextView = (ScanMultiAutoCompleteTextView) findViewById(R.id.scanarguments);
		profilesSpinner = (Spinner) findViewById(R.id.profiles);
		taskName = (TextView) findViewById(R.id.taskname);
		progressBar = (ProgressBar) findViewById(R.id.progress);

		//Set up ScanArguments input
		ArrayAdapter<String> argsAdapter = new ArrayAdapter<String>(this, R.layout.scan_args_list_1item, FULL_ARGS);
		scanArgsTextView.setAdapter(argsAdapter);
		scanArgsTextView.setTokenizer(new ScanArgumentsTokenizer());

		//Set up hosts and ports ListView adapters
		hostsListView = (ListView) findViewById(R.id.hostsresults);
		hostsListView.setOnItemClickListener(hostClickListener);
		hostsListView.setOnItemLongClickListener(hostLongClickListener);
		hostsListView.setEnabled(false);

		portsListView = (ListView) findViewById(R.id.portsresults);
		portsListView.setEnabled(false);

		loadScanProfiles();
	}

	@Override
	protected void onResume() {
		super.onResume();
		scanContentObserver = new ScanContentObserver(s);

		Intent intent = this.getIntent();
		Uri content = intent.getData();
		if(content != null) {
			List<String> segments = content.getPathSegments();
			int clientID = Integer.parseInt(segments.get(1));
			int scanID = Integer.parseInt(segments.get(2));
			s = getContentResolver().query(content,null,null,null,null);
			int scanStateColumn= s.getColumnIndex(Scans.SCAN_STATE);
			s.moveToFirst();
			int scanState = s.getInt(scanStateColumn);
			switch (scanState){
				case Scans.SCAN_STATE_STARTED:
					if(null != hostsAdapter)
						hostsAdapter.changeCursor(null);
					if(null != portsAdapter)
						portsAdapter.changeCursor(null);
					onScanStart(clientID,scanID);
					break;
				case Scans.SCAN_STATE_FINISHED:
					scanUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/scans/"+clientID+"/"+scanID);
					hostsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/hosts/"+clientID+"/"+scanID);
					detailsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/details/"+clientID+"/"+scanID);

					//Set up scanContentObserver for Task Name and Progress
					this.getApplicationContext().getContentResolver().registerContentObserver(scanUri, true, scanContentObserver);
					taskName.setText("");
					progressBar.setProgress(0);
					onNotifyFinished();
					break;
				default:
					//Shouldn't happen
					Log.d("UmitScanner.onResume",
							"Unknown SCAN_STATE detected, someone has been tampering with the code");
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopManagingCursor(h);
		stopManagingCursor(p);
		stopManagingCursor(s);
		this.getContentResolver().unregisterContentObserver(scanContentObserver);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	public View.OnClickListener startScan = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			rqstStartScan("./nmap "+ scanArgsTextView.getText().toString());
			InputMethodManager imm =
					(InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
			taskName.setText("");
			progressBar.setProgress(0);

			hostsAdapter.changeCursor(null);
			stopManagingCursor(h);

			portsAdapter.changeCursor(null);
			stopManagingCursor(p);

			//Clear the database
			getContentResolver().delete(scanUri,null,null);

			actionButton.setText("Start");
			actionButton.setOnClickListener(startScan);
		}
	};

	public void onScanStart(int clientID, int scanID) {

		scanUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/scans/" + clientID + "/" + scanID);
		hostsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/hosts/"+clientID+"/"+scanID);
		detailsUri = Uri.parse("content://org.umit.ns.mobile.provider.Scanner/details/"+clientID+"/"+scanID);

		//Set up scanContentObserver for Task Name and Progress
		this.getApplicationContext().getContentResolver().registerContentObserver(scanUri, true, scanContentObserver);
		taskName.setText("");
		progressBar.setProgress(0);

		actionButton.setText("Stop");
		actionButton.setOnClickListener(stopScan);
	}

	public void onScanStop() {
		actionButton.setText("Start");
		actionButton.setOnClickListener(startScan);

		//Unregister ContentObserver
		this.getApplicationContext().getContentResolver().unregisterContentObserver(scanContentObserver);
		taskName.setText("");
		progressBar.setProgress(0);
	}

	protected void onNotifyProblem(String info) {
		Log.e("UmitScanner", "Scan has crashed. Info: " + info);
		Toast.makeText(getApplicationContext(), "Scanning problem: " + info, Toast.LENGTH_LONG).show();
		//Unregister ContentObserver
		this.getApplicationContext().getContentResolver().unregisterContentObserver(scanContentObserver);
		taskName.setText("");
		progressBar.setProgress(0);
	}

	public void onNotifyFinished() {
		actionButton.setText("Clear");
		actionButton.setOnClickListener(clearResults);

		//Unregister ContentObserver
		this.getApplicationContext().getContentResolver().unregisterContentObserver(scanContentObserver);
		progressBar.setProgress(100);

		//show results
		h = getContentResolver().query(hostsUri,null,null,null,null);
		hostsColumnIP = h.getColumnIndex(Hosts.IP);
		hostsColumnState = h.getColumnIndex(Hosts.STATE);
		startManagingCursor(h);
		hostsAdapter = new HostsListAdapter(this,h);

		hostsListView.setEnabled(true);
		hostsListView.setAdapter(hostsAdapter);

		String[] portsFromColumns = {Details.NAME };
		int[] portsToViews = {R.id.port_listview_item};
		portsAdapter = new SimpleCursorAdapter(this,R.layout.port_item,
				null,portsFromColumns,portsToViews);
		portsListView.setAdapter(portsAdapter);
		portsListView.setEnabled(true);
	}

	AdapterView.OnItemClickListener hostClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			Integer state = (Integer)view.getTag();
			if(state == Hosts.STATE_UP){
				Uri singleHostDetailsUri = detailsUri.buildUpon().appendPath(((TextView)view).getText().toString()).build();
				p = getContentResolver().query(singleHostDetailsUri,null,null,null,null);
				startManagingCursor(p);
				portsAdapter.changeCursor(p);
			}
		}
	};

	AdapterView.OnItemLongClickListener hostLongClickListener = new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
			Integer state = (Integer)view.getTag();
			if(state == Hosts.STATE_UP){
				Uri singleHostDetailsUri = detailsUri.buildUpon().appendPath(((TextView)view).getText().toString()).build();
				String hostDetailsAction = getString(R.string.hostdetailsactivity_action);
				Intent intent = new Intent(hostDetailsAction, singleHostDetailsUri);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				view.getContext().startActivity(intent);
			}
			return true;
		}
	};

	private class ScanContentObserver extends ContentObserver {
		private Cursor cursor;

		public ScanContentObserver(Cursor cursor) {
			super(null);
			this.cursor=cursor;
		}
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onScanChange();
				}
			});
		}
	}

	private void onScanChange() {
		s = getContentResolver().query(scanUri,null,null,null,null);

		int task_progress_column = s.getColumnIndex(Scans.TASK_PROGRESS);
		int task_name_column = s.getColumnIndex(Scans.TASK);
		if(s.getCount()==1){
			s.moveToFirst();
			int task_progress = s.getInt(task_progress_column);
			progressBar.setProgress(task_progress);
			String task_name = s.getString(task_name_column);
			if(task_name!=null){
				taskName.setText("Task: " + task_name);
			}
		}
	}

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
					view.setTag(Hosts.STATE_UP);
					view.setBackgroundColor(GREEN_COLOR);
					break;
				case Hosts.STATE_DOWN:
					view.setTag(Hosts.STATE_DOWN);
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
					view.setTag(Hosts.STATE_UP);
					view.setBackgroundColor(GREEN_COLOR);
					break;
				case Hosts.STATE_DOWN:
					view.setTag(Hosts.STATE_DOWN);
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

	private void deleteScanProfile(String name){
		SharedPreferences sharedPreferences = getSharedPreferences("profiles",MODE_WORLD_WRITEABLE);
		String test = sharedPreferences.getString(name,"No");
		if(TextUtils.equals(test,"No")){
			Toast.makeText(this,"A profile with that name doesn't exists.",Toast.LENGTH_SHORT).show();
			return;
		}
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(name);
		if(TextUtils.equals(name,""))
			editor.putString("","");
		editor.commit();
		loadScanProfiles();
	}

	private void saveScanProfile(String name, String inputArgs) {
		SharedPreferences sharedPreferences = getSharedPreferences("profiles",MODE_WORLD_WRITEABLE);
		String test = sharedPreferences.getString(name,"No");

		if( ! TextUtils.equals(test,"No") ){
			Toast.makeText(this,"A profile with that name already exists.",Toast.LENGTH_SHORT).show();
			return;
		}

		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(name,inputArgs);
		editor.commit();
		loadScanProfiles();
		int pos = profilesAdapter.getPosition(name);
		if(pos>0)
			profilesSpinner.setSelection(pos);
	}

	private void loadScanProfiles() {
		SharedPreferences sharedPreferences = getSharedPreferences("profiles",MODE_WORLD_WRITEABLE);
		String test = sharedPreferences.getString("Intense scan","No");
		if(TextUtils.equals(test,"No")){
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString("","");
			editor.putString("Intense scan","-T4 -A ");
			editor.putString("Intense scan plus UDP","-sS -sU -T4 -A ");
			editor.putString("Intense scan, all TCP ports","-p 1-65535 -T4 -A ");
			editor.putString("Intense scan, no ping","-T4 -A -Pn ");
			editor.putString("Ping scan","-sn ");
			editor.putString("Quick scan","-T4 -F ");
			editor.putString("Quick scan plus","-sV -T4 -O -F --version-light ");
			editor.putString("Slow, comprehensive scan", "-sS -sU -T4 -A -v -PE -PP -PS80,443 -PA3389 -PU40125 -PY -g 53 --script \"default or (discovery and safe)\" ");
			editor.commit();
		}
		profiles = sharedPreferences.getAll();
		Set <String> profileNamesSet =  profiles.keySet();
		String[] profileNamesArray = profileNamesSet.toArray(new String[0]);

		profilesAdapter = new ArrayAdapter<CharSequence>( this,
				android.R.layout.simple_spinner_item,
				profileNamesArray);
		profilesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		profilesSpinner.setAdapter(profilesAdapter);
		profilesSpinner.setOnItemSelectedListener(
			new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					String profileName = profilesAdapter.getItem(pos).toString();
					String profileArgs = (String)profiles.get(profileName);
					scanArgsTextView.setText(profileArgs);
					int position = scanArgsTextView.length();
					Editable argsEdit = scanArgsTextView.getText();
					Selection.setSelection(argsEdit,position);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					scanArgsTextView.setText("");
				}
		});

		saveProfileButton=(Button)findViewById(R.id.SaveProfileButton);
		deleteProfileButton=(Button)findViewById(R.id.DeleteProfileButton);

		saveProfileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());

				alert.setTitle("Save Scanning Profile");
				alert.setMessage("Insert name for new scanning profile.");

				final EditText input = new EditText(view.getContext());
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String name = input.getText().toString();
						String inputArgs = scanArgsTextView.getText().toString();
						saveScanProfile(name,inputArgs);
					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				alert.show();
			}
		});

		deleteProfileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());

				alert.setTitle("Delete Scanning Profile");
				alert.setMessage("Really delete selected Scanning Profile?");

				alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String name = (String)profilesSpinner.getSelectedItem();
						deleteScanProfile(name);
					}
				});

				alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				alert.show();
			}
		});

	}
}

