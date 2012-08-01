package org.umit.ns.mobile.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.os.Bundle;import android.os.Handler;import android.os.IBinder;import android.os.Message;import android.os.Messenger;import android.os.RemoteException;import android.widget.Toast;
import org.umit.ns.mobile.R;
import org.umit.ns.mobile.ScanActivity;import org.umit.ns.mobile.api.ScanCommunication;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.*;import java.lang.Math;import java.lang.Object;import java.lang.Override;import java.lang.String;import java.lang.StringBuffer;import java.util.Random;

//TODO Handle Runtime Changes
public abstract class ScanClientActivity extends Activity implements ScanCommunication {

	private Messenger msgrService;
	private boolean mBound;
	private final Messenger msgrLocal = new Messenger(new IncomingHandler());
	private Random random = new Random();

	//TODO keep when rebinding
	private int clientID;
	protected Scan scan;
	private boolean wasConnected = false;

	protected class Scan {
		public int ID;
		public int clientID;
		boolean rootAccess;

		String scanArguments;
		String scanResultsFilename;

		String scanResults;

		int progress = 0;
		boolean started = false;


		public Scan(int clientID, int scanID, boolean rootAccess, String scanResultsFilename) {
			this.clientID = clientID;
			this.ID = scanID;
			this.rootAccess = rootAccess;
			this.scanResultsFilename = scanResultsFilename;
		}
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

		Intent intent = new Intent("org.umit.ns.mobile.service.ScanService");
		intent.putExtra("Messenger", msgrLocal);
		intent.putExtra("ClientID", clientID);
    intent.putExtra("Action",getString(R.string.scanactivity_action));

		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		log("onCreate()-Bound to service");
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
		if (isFinishing()) {
			//TODO Pending intent may take place here so it rebinds or not... we'll see
		}

		unbindService(serviceConnection);
	}

	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case RESP_START_SCAN_OK:
					int scanID = msg.arg1;
					boolean rootAccess = (msg.arg2 == 1);
					String scanResultsFilename = ((Bundle) msg.obj).containsKey("ScanResultsFilename") ?
							((Bundle) msg.obj).getString("ScanResultsFilename") : null;

					if (scanResultsFilename == null) {
						log("RESP_START_SCAN_OK: ScanResultsFilename is null!");
						break;
					}

					log("RESP_START_SCAN_OK");

					scan = new Scan(clientID, scanID, rootAccess, scanResultsFilename);
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


				case NOTIFY_SCAN_PROGRESS:
					if (scan == null) {
						log("NOTIFY_SCAN_PROGRESS: scan object is null!");
						break;
					}

					if (scan.ID != msg.arg1) {
						log("NOTIFY_SCAN_PROGRESS: scanID not matching!");
						break;
					}

					scan.progress = msg.arg2;
					onNotifyProgress(scan.progress);
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

	public final void rqstStartScan(String scanArguments) {
		if (!mBound)
			return;

		Bundle bundle = new Bundle();
		bundle.putString("ScanArguments", scanArguments);

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

	//TODO remove unused methods and fields

	protected abstract void onScanStart(int clientID, int scanID);

	protected abstract void onScanStop();

	protected abstract void onNotifyProgress(int progress);

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
