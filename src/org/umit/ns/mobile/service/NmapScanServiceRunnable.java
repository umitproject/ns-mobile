package org.umit.ns.mobile.service;

import android.os.*;
import android.util.Log;
import org.umit.ns.mobile.api.ScanCommunication;

import java.io.DataOutputStream;
import java.io.IOException;

class NmapScanServiceRunnable implements Runnable, ScanCommunication {
	private final boolean rootAccess;
	private final String scanResultsFile;
	private final String scanArguments;
	private final Messenger mService;
	private final int id;
	private final String nativeInstallDir;

	private java.lang.Process p;

	public NmapScanServiceRunnable(final int id,
	                               final IBinder service,
	                               final String scanArguments,
	                               final String scanResultsFile,
	                               final boolean hasRoot,
	                               final String nativeInstallDir) {

		Log.d("UmitScanner", "NmapScanRunnable.NmapScanRunnable() ID:" + id);
		this.id = id;
		this.scanResultsFile = scanResultsFile;
		this.rootAccess = hasRoot;
		this.mService = new Messenger(service);
		this.nativeInstallDir = nativeInstallDir;
		//TODO expose time refresh to activity default is 1s
		//output to /dev/null so we don't fill "ze buffer" up
		this.scanArguments = scanArguments + " -vv --stats-every 1s -oX " + scanResultsFile + " > /dev/null";
	}

	private void tellService(int RESP_CODE) {
		Log.d("UmitScanner", "tellService():RESP_CODE=" + RESP_CODE);
		Message msg = Message.obtain(null, RESP_CODE, id, 0);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d("UmitScanner",
					"Caught Remote Exception while sending from ScanTask to ScanService:" + e.toString());
		}
	}

	private void tellService(int RESP_CODE, String info) {
		Log.d("UmitScanner", "tellService():RESP_CODE=" + RESP_CODE + "; info=" + info);
		Message msg = Message.obtain(null, RESP_CODE, id, 0);
		Bundle bundle = new Bundle();
		bundle.putString("Info", info);
		msg.obj = bundle;
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d("UmitScanner",
					"Caught Remote Exception while sending from ScanTask to ScanService:" + e.toString());
		}
	}

	public void run() {
		Log.d("UmitScanner", "NmapScanRunnable.run()");

		if (scanArguments == null) {
			Log.e("UmitScanner", "NmapScanRunnable.run() scanArguments is null");
			return;
		}

		try {
			if (rootAccess)
				p = Runtime.getRuntime().exec("su");
			else {
				p = Runtime.getRuntime().exec("sh");
			}

			DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
			try {
				Log.d("UmitScanner", "Executing $ " + "cd " + nativeInstallDir + "/nmap/bin \n");
				pOut.writeBytes("cd " + nativeInstallDir + "/nmap/bin \n");
				Log.d("UmitScanner", "Executing $ " + scanArguments + "\n");
				pOut.writeBytes(scanArguments + "\n");
				pOut.writeBytes("exit\n");
				pOut.flush();
				p.waitFor();
			} catch (IOException e) {
				//manage abrupt stopping
				if (Thread.currentThread().isInterrupted()) {
					Log.d("UmitScanner", "Interrupted from blocked I/O");
					p.destroy();
				} else {
					tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				if (Thread.currentThread().isInterrupted()) {
					Log.d("UmitScanner", "Interrupted from blocked I/O");
					p.destroy();
				} else {
					tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
					e.printStackTrace();
				}
			}
			//scan finished
			tellService(NOTIFY_SCAN_FINISHED);

		} catch (IOException e) {
			if (Thread.currentThread().isInterrupted()) {
				//manage abrupt stopping
				Log.d("UmitScanner", "Interrupted from blocked I/O");
				p.destroy();
			} else {
				tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
