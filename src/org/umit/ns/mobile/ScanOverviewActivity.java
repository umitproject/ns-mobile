package org.umit.ns.mobile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

	private Messenger msgrService;
	private boolean mBound;

	private int fakeClientID = 0xc0ff33; // we need this really bad

	private final Messenger fakeMsgrLocal = new Messenger(new Handler());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_overview_activity);

		scansListView = (ListView)findViewById(R.id.Scans);

		Cursor cursor = getContentResolver().query(Scanner.SCANS_URI, null, null, null, null);
		startManagingCursor(cursor);

		scanArgumentsColumn = cursor.getColumnIndex(Scans.SCAN_ARGUMENTS);
		taskNameColumn = cursor.getColumnIndex(Scans.TASK);
		taskProgressColumn = cursor.getColumnIndex(Scans.TASK_PROGRESS);
		clientIDColumn = cursor.getColumnIndex(Scans.CLIENT_ID);
		scanIDColumn = cursor.getColumnIndex(Scans.SCAN_ID);

		scansAdapter = new ScansAdapter(this,cursor);
		scansListView.setAdapter(scansAdapter);
		int count = scansListView.getCount();

		//Bind to service

		Intent intent = new Intent("org.umit.ns.mobile.service.ScanService");
		intent.putExtra("Messenger", fakeMsgrLocal);
		intent.putExtra("ClientID", fakeClientID);
		intent.putExtra("Action",getString(R.string.scanactivity_action));

		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		Log.d("UmitScanner.ScanOverviewActivity", "onCreate()-Bound to service");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			msgrService = new Messenger(service);
			mBound = true;
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

			stopButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int clientID = (Integer)view.getTag(R.id.clientIDkey);
					int scanID = (Integer)view.getTag(R.id.scanIDkey);
					scanOverviewActivity.rqstStopScan(clientID,scanID);
				}
			});

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
			taskName.setText(cursor.getString(taskNameColumn));

			int clientID = cursor.getInt(clientIDColumn);
			int scanID = cursor.getInt(scanIDColumn);

			stopButton.setTag(R.id.clientIDkey,clientID);
			stopButton.setTag(R.id.scanIDkey,scanID);

			stopButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int clientID = (Integer)view.getTag(R.id.clientIDkey);
					int scanID = (Integer)view.getTag(R.id.scanIDkey);
					scanOverviewActivity.rqstStopScan(clientID,scanID);
				}
			});
		}
	}

	public final void rqstStopScan(int clientID, int scanID) {
		if (!mBound)
			return;
		tellService(RQST_STOP_SCAN, clientID, scanID, null, null);
	}

}