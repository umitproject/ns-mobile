package org.umit.ns.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.provider.Scanner.Scans;

import android.database.Cursor;

import java.util.Random;

public class ScanOverviewActivity extends Activity implements ScanCommunication {

	ListView scansListView;
	ScansAdapter scansAdapter;

	static int scanArgumentsColumn;
	static int taskNameColumn;
	static int taskProgressColumn;
	static int clientIDColumn;
	static int scanIDColumn;
	static int clientActionColumn;

	private Messenger msgrService;
	private volatile boolean mBound;

	Cursor cursor;

	private int fakeClientID = new Random().nextInt(); // we need this really bad

	private final Messenger fakeMsgrLocal = new Messenger(new Handler());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_overview_activity);

		scansListView = (ListView)findViewById(R.id.Scans);

		cursor = getContentResolver().query(Scanner.SCANS_URI, null, null, null, null);
		startManagingCursor(cursor);

		scanArgumentsColumn = cursor.getColumnIndex(Scans.SCAN_ARGUMENTS);
		taskNameColumn = cursor.getColumnIndex(Scans.TASK);
		taskProgressColumn = cursor.getColumnIndex(Scans.TASK_PROGRESS);
		clientIDColumn = cursor.getColumnIndex(Scans.CLIENT_ID);
		scanIDColumn = cursor.getColumnIndex(Scans.SCAN_ID);
		clientActionColumn = cursor.getColumnIndex(Scans.CLIENT_ACTION);

		scansAdapter = new ScansAdapter(this,cursor);
		scansListView.setAdapter(scansAdapter);
		int count = scansListView.getCount();
	}

	@Override
	protected void onResume(){
		super.onResume();
		//Bind to service
		Intent intent = new Intent("org.umit.ns.mobile.service.ScanService");
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

		Log.d("UmitScanner.ScanOverviewActivity", "onResume() - Bound to service");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(mBound){
			Log.d("UmitScanner.ScanOverviewActivity", "onPause() - Unbound from service");
			unbindService(serviceConnection);
		}
		stopManagingCursor(cursor);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.scan_activity_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_about:
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setIcon(R.drawable.icon);
				alert.setTitle("Umit Project");

				alert.setMessage("Umit Network Scanner for Android");

				alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
				alert.setPositiveButton("Visit Web Site", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("http://dev.umitproject.org"));
						startActivity(intent);
					}
				});
				alert.show();
				return true;

			case R.id.menu_exit:
				tellService(STOP_SCAN_SERVICE,fakeClientID,0,null,null);
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			msgrService = new Messenger(service);
			mBound = true;

			//Register the fake client
			Bundle bundle = new Bundle();
			bundle.putInt("ClientID",fakeClientID);
			bundle.putParcelable("Messenger", fakeMsgrLocal);
			bundle.putString("Action", getString(R.string.scanactivity_action));
			tellService(REGISTER_CLIENT,0,0,bundle,null);
		}

		public void onServiceDisconnected(ComponentName className) {
			msgrService = null;
			mBound = false;
		}
	};

	private boolean tellService(int RQST_CODE,
	                            int scanID,
	                            int msg_arg2,
	                            Bundle bundle,
	                            Messenger replyTo) {

		Log.d("UmitScanner.ScanOverviewActivity", "tellService():RESP_CODE=" + RQST_CODE);

		Message msg;

		if (bundle != null)
			msg = Message.obtain(null, RQST_CODE, scanID, msg_arg2, bundle);
		else
			msg = Message.obtain(null, RQST_CODE, scanID, msg_arg2);

		if (replyTo != null)
			msg.replyTo = replyTo;

		try {
			msgrService.send(msg);
		} catch (RemoteException e) {
			Log.d("UmitScanner.ScanOverviewActivity", ".tellService():could not send message.");
			return false;
		}
		return true;
	}

	public static class ScansAdapter extends CursorAdapter {
		private ScanOverviewActivity scanOverviewActivity;

		public ScansAdapter(Context context, Cursor c) {
			super(context, c);
			scanOverviewActivity = (ScanOverviewActivity)context;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			final RelativeLayout view =  (RelativeLayout)inflater.inflate(
					R.layout.scan_overview_item, parent, false);

			TextView scanArguments = (TextView) view.findViewById(R.id.ScanArguments);
		  ProgressBar progress = (ProgressBar) view.findViewById(R.id.Progress);
			TextView taskName = (TextView) view.findViewById(R.id.TaskName);
			Button stopButton = (Button) view.findViewById(R.id.StopButton);

			scanArguments.setText(cursor.getString(scanArgumentsColumn));
			progress.setProgress(cursor.getInt(taskProgressColumn));
			taskName.setText(cursor.getString(taskNameColumn));

			Integer clientID = cursor.getInt(clientIDColumn);
			Integer scanID = cursor.getInt(scanIDColumn);
			stopButton.setTag(R.id.clientIDkey,clientID);
			stopButton.setTag(R.id.scanIDkey,scanID);

			stopButton.setOnClickListener(stopClickListener);

			String clientAction = cursor.getString(clientActionColumn);
			Uri contentUri = Uri.parse(Scanner.SCANS_URI.toString()+"/"+clientID+"/"+scanID);

			scanArguments.setTag(R.id.clientActionkey, clientAction);
			scanArguments.setTag(R.id.scanContentkey, contentUri);
			scanArguments.setOnClickListener(scanActivityInitiator);

			progress.setTag(R.id.clientActionkey, clientAction);
			progress.setTag(R.id.scanContentkey, contentUri);
			progress.setOnClickListener(scanActivityInitiator);

			return view;
		}


		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			ViewGroup view = (ViewGroup) v;

			TextView scanArguments = (TextView) view.findViewById(R.id.ScanArguments);
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.Progress);
			TextView taskName = (TextView) view.findViewById(R.id.TaskName);
			Button stopButton = (Button) view.findViewById(R.id.StopButton);

			scanArguments.setText(cursor.getString(scanArgumentsColumn));
			progress.setProgress(cursor.getInt(taskProgressColumn));
			String task = cursor.getString(taskNameColumn);
			taskName.setText(task);

			int clientID = cursor.getInt(clientIDColumn);
			int scanID = cursor.getInt(scanIDColumn);


			stopButton.setTag(R.id.clientIDkey,clientID);
			stopButton.setTag(R.id.scanIDkey,scanID);

			if(TextUtils.equals(task,"Scan Finished")){
				stopButton.setText("Clear");
				stopButton.setOnClickListener(clearClickListener);
			} else {
				stopButton.setText("Stop");
				stopButton.setOnClickListener(stopClickListener);
			}

			String clientAction = cursor.getString(clientActionColumn);
			Uri contentUri = Uri.parse(Scanner.SCANS_URI.toString()+"/"+clientID+"/"+scanID);

			scanArguments.setTag(R.id.clientActionkey,clientAction);
			scanArguments.setTag(R.id.scanContentkey,contentUri);
			scanArguments.setOnClickListener(scanActivityInitiator);

			progress.setTag(R.id.clientActionkey,clientAction);
			progress.setTag(R.id.scanContentkey,contentUri);
			progress.setOnClickListener(scanActivityInitiator);
		}

		View.OnClickListener scanActivityInitiator = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String clientAction = (String)view.getTag(R.id.clientActionkey);
				Uri content = (Uri)view.getTag(R.id.scanContentkey);

				Intent intent = new Intent(clientAction, content);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				view.getContext().startActivity(intent);
			}
		};

		View.OnClickListener stopClickListener =  new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int clientID = (Integer)view.getTag(R.id.clientIDkey);
				int scanID = (Integer)view.getTag(R.id.scanIDkey);
				scanOverviewActivity.rqstStopScan(clientID,scanID);
			}
		};

		View.OnClickListener clearClickListener =  new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Integer clientID = (Integer)view.getTag(R.id.clientIDkey);
				Integer scanID = (Integer)view.getTag(R.id.scanIDkey);
				Uri scanUri = Scanner.SCANS_URI.buildUpon()
						.appendPath(clientID.toString())
						.appendPath(scanID.toString()).build();
				view.getContext().getContentResolver().delete(scanUri,null,null);
			}
		};

	}

	private class ClearDatabaseTask extends AsyncTask<Uri, Integer, Integer> {
		protected Integer doInBackground(Uri... uris) {
			int count = uris.length;
			for (int i = 0; i < count; i++) {
				getContentResolver().delete(uris[i],null,null);
				// Escape early if cancel() is called
				if (isCancelled()) break;
			}
			return 0;
		}
		protected void onProgressUpdate(Integer... progress) {
		}
		protected void onPostExecute(Integer result) {
		}
	}

	public final void rqstStopScan(int clientID, int scanID) {
		if (!mBound)
			return;
		tellService(RQST_STOP_SCAN, clientID, scanID, null, null);
	}

}
