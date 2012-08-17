package org.umit.ns.mobile.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;import android.os.Handler;import android.os.IBinder;import android.os.Message;import android.os.Messenger;import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import org.umit.ns.mobile.R;
import org.umit.ns.mobile.ScanActivity;import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.provider.Scanner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.*;import java.lang.Math;import java.lang.Object;import java.lang.Override;import java.lang.String;import java.lang.StringBuffer;
import java.util.List;
import java.util.Random;

public abstract class ScanClientActivity extends Activity implements ScanCommunication {

	private Messenger msgrService;
	private boolean mBound;
	private final Messenger msgrLocal = new Messenger(new IncomingHandler());
	private Random random = new Random();

	private Integer clientID;
	private Scan scan;
	private boolean wasConnected = false;
	private boolean rootAccess = false;
	private boolean rootAccessReceived = false;

	private class Scan {
		public int ID;
		public int clientID;
		boolean rootAccess;

		boolean started = false;


		public Scan(int clientID, int scanID, boolean rootAccess) {
			this.clientID = clientID;
			this.ID = scanID;
			this.rootAccess = rootAccess;
		}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			msgrService = new Messenger(service);
			mBound = true;

			Bundle bundle = new Bundle();
			bundle.putInt("ClientID",clientID);
			bundle.putParcelable("Messenger", msgrLocal);
			bundle.putString("Action", getString(R.string.scanactivity_action));

			tellService(REGISTER_CLIENT,0,0,bundle,null);
		}

		public void onServiceDisconnected(ComponentName className) {
			msgrService = null;
			mBound = false;

		}
	};

	@Override
	protected void onNewIntent (Intent intent){
		this.setIntent(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		log("onCreate()-Start");
		super.onCreate(savedInstanceState);

		Scan retained_scan = (Scan) getLastNonConfigurationInstance();
		if (retained_scan != null) {
			wasConnected = true;
			scan = retained_scan;
			clientID = scan.clientID;
		} else {
			wasConnected = false;
      clientID = Math.abs(random.nextInt());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = getIntent();
		Uri content = intent.getData();
		int clID,scID,scanState;
		boolean started;

		if(content != null) {
			List<String> segments = content.getPathSegments();

			clID = Integer.parseInt(segments.get(1));
			scID = Integer.parseInt(segments.get(2));

			Cursor s = getContentResolver().query(content,null,null,null,null);
			int scanStateColumn= s.getColumnIndex(Scanner.Scans.SCAN_STATE);
			int rootAccessColumn = s.getColumnIndex(Scanner.Scans.ROOT_ACCESS);

			s.moveToFirst();

			scanState = s.getInt(scanStateColumn);
			rootAccess = (s.getInt(rootAccessColumn) == Scanner.Scans.ROOT_ACCESS_YES);
			rootAccessReceived = true;

			clientID=clID;
			scan = new Scan(clID,scID,rootAccess);
			scan.started = (scanState==Scanner.Scans.SCAN_STATE_STARTED);

			Intent serviceIntent = new Intent("org.umit.ns.mobile.service.ScanService");

			bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			log("onResume()- Rebound to service");
		} else {
			Intent serviceIntent = new Intent("org.umit.ns.mobile.service.ScanService");

			bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			log("onResume()-Bound to service");
		}

	}

	@Override
	protected void onPause(){
		super.onPause();
		if(mBound){
			log("onPause() - unbound from Service.");
			unbindService(serviceConnection);
		}
	}
	@Override
	public Object onRetainNonConfigurationInstance() {
		super.onRetainNonConfigurationInstance();
		final Scan s = scan;
		return s;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case REGISTER_CLIENT_RESP:
					rootAccess = (msg.arg1 == 1);
					rootAccessReceived = true;
					onRegisterClient(rootAccess);
					break;
				case RESP_START_SCAN_OK:
					int scanID = msg.arg1;
					rootAccess = (msg.arg2 == 1);
					rootAccessReceived=true;

					log("RESP_START_SCAN_OK");

					scan = new Scan(clientID, scanID, rootAccess);
					scan.started = true;
					onScanStart(clientID,scanID);
					break;

				case RESP_STOP_SCAN_OK:
					if (scan.ID != msg.arg1) {
						log("RESP_STOP_SCAN_OK: scanID not matching!");
						break;
					}
					scan = null;
					onScanStop();
					break;

				case NOTIFY_SCAN_FINISHED:
					if (scan == null) {
						log("NOTIFY_SCAN_FINISHED: scan object is null!");
						break;
					}

					if (scan.ID != msg.arg1) {
						log("NOTIFY_SCAN_PROGRESS: scanID not matching!");
						break;
					}
					onNotifyFinished();
					break;

				case NOTIFY_SCAN_PROBLEM:
					String info = (((Bundle) msg.obj).containsKey("Info") ?
							((Bundle) msg.obj).getString("Info") : "");
					log("NOTIFY_SCAN_PROBLEM:" + info);
					onNotifyProblem(info);
					break;

				default:
					super.handleMessage(msg);
			}
		}
	}



	//========API

	protected abstract void onRegisterClient(boolean rootAccess);

	public final void rqstStartScan(String scanArguments, String scanProfile) {
		if (!mBound)
			return;

		Bundle bundle = new Bundle();
		bundle.putString("ScanArguments", scanArguments);
		if(scanProfile!=null)
			bundle.putString("ScanProfile",scanProfile);

		tellService(RQST_START_SCAN, clientID, 0, bundle, null);
	}

	public final void rqstStopScan() {
		if (!mBound)
			return;

		if (scan == null) {
			log("stopScan(): scan object is null!");
			return;
		}

		if (!scan.started)
			return;

		tellService(RQST_STOP_SCAN, scan.clientID, scan.ID, null, null);
	}

	protected abstract void onScanStart(int clientID, int scanID);

	protected abstract void onScanStop();

	protected abstract void onNotifyProblem(String info);

	protected abstract void onNotifyFinished();

	//=======\API

	private boolean tellService(int RQST_CODE,
	                            int scanID,
	                            int msg_arg2,
	                            Bundle bundle,
	                            Messenger replyTo) {

		log("tellService():RESP_CODE=" + RQST_CODE);

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
			log(".tellService():could not send message.");
			return false;
		}
		return true;
	}

	private void log(String log_message) {
		android.util.Log.d("UmitScanner", "ScanClientActivity." + log_message);
	}
}
