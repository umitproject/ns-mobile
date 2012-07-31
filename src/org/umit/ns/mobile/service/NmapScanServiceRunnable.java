package org.umit.ns.mobile.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.xml.NmapSaxParser;
import org.umit.ns.mobile.provider.Scanner.Scans;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NmapScanServiceRunnable implements Runnable, ScanCommunication {
	private final boolean rootAccess;
	private final String scanResultsFile;
	private final String scanArguments;
	private final Messenger mService;
	private final int scanID;
	private final int clientID;
	private final Uri scanUri;
	private final ContentResolver contentResolver;
	private final String nativeInstallDir;

	private java.lang.Process p;

	public NmapScanServiceRunnable(final int scanID,
	                               final int clientID,
	                               final ContentResolver contentResolver,
	                               final IBinder service,
	                               final String scanArguments,
	                               final String scanResultsFile,
	                               final boolean hasRoot,
	                               final String nativeInstallDir) {

		Log.d("UmitScanner", "NmapScanRunnable.NmapScanRunnable() ID:" + scanID);
		this.scanID = scanID;
		this.clientID = clientID;
		this.scanUri = Uri.parse(Scanner.SCANS_URI+"/"+clientID+"/"+scanID);
		this.contentResolver=contentResolver;
		this.scanResultsFile = scanResultsFile;
		this.rootAccess = hasRoot;
		this.mService = new Messenger(service);
		this.nativeInstallDir = nativeInstallDir;
		this.scanArguments = scanArguments + " -vv --stats-every 500ms -oX " + scanResultsFile;
	}

	private void tellService(int RESP_CODE) {
		Log.d("UmitScanner", "tellService():RESP_CODE=" + RESP_CODE);
		Message msg = Message.obtain(null, RESP_CODE, scanID, 0);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d("UmitScanner",
					"Caught Remote Exception while sending from ScanTask to ScanService:" + e.toString());
		}
	}

	private void tellService(int RESP_CODE, String info) {
		Log.d("UmitScanner", "tellService():RESP_CODE=" + RESP_CODE + "; info=" + info);
		Message msg = Message.obtain(null, RESP_CODE, scanID, 0);
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
		ContentValues tp;

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

				NmapSaxParser parser = new NmapSaxParser(contentResolver,
						Integer.toString(clientID),Integer.toString(scanID),scanResultsFile);

				Log.d("UmitScanner", "Executing $ " + "cd " + nativeInstallDir + "/nmap/bin \n");
				pOut.writeBytes("cd " + nativeInstallDir + "/nmap/bin \n");
				Log.d("UmitScanner", "Executing $ " + scanArguments + "\n");
				pOut.writeBytes(scanArguments + "\n");
				pOut.writeBytes("exit\n");
				pOut.flush();

				int read;
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				char[] buffer = new char[1024];

				try{
					StringBuffer output = new StringBuffer();
					StringBuilder keep = new StringBuilder();

					while ((read = reader.read(buffer)) > 0) {
						output.append(buffer, 0, read);
						String [] splitOutput = output.toString().split("\n",-1);

						if(splitOutput.length>0){
							keep.append(splitOutput[0]);
							matchTaskProgress(keep.toString());
							Log.d("UmitScanner.ScanOutput",keep.toString());

							keep = new StringBuilder();

							for(int i =1; i<(splitOutput.length-1); i++) {
								matchTaskProgress(splitOutput[i]);
								Log.d("UmitScanner.ScanOutput", splitOutput[i]);
							}

							keep.append(splitOutput[splitOutput.length-1]);
						} else {
							keep.append(output);
						}
						output = new StringBuffer();
					}

					if( ! TextUtils.isEmpty(keep)){
						matchTaskProgress(keep.toString());
						Log.d("UmitScanner.ScanOutput",keep.toString());
					}
				} catch(IOException e) {
					Log.d("UmitScannerService", e.toString());
				}

				p.waitFor();

				Log.d("UmitScannerRunnable", "Parsing results");

				tp=new ContentValues();
				tp.put(Scans.TASK,"Parsing Results");
				tp.put(Scans.TASK_PROGRESS,0);
				contentResolver.update(scanUri,tp,null,null);

				parser.parse();

				tp = new ContentValues();
				tp.put(Scans.TASK,"Scan Finished");
				tp.put(Scans.TASK_PROGRESS,0);
				tp.put(Scans.SCAN_STATE, Scans.SCAN_STATE_FINISHED);
				contentResolver.update(scanUri,tp,null,null);

			} catch (InterruptedException e) {
				//manage abrupt stopping
				Log.d("UmitScanner", "Interrupted from blocked I/O");
				p.destroy();
				return;
			} catch (IOException e) {
				tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
				e.printStackTrace();
			}
			//scan finished
//			File file = new File(scanResultsFile);
//			file.delete();


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

	/** Matches Nmap Scan Output with a Scan Progress line
	 * @return String Array with the first element Task Name and second Task progress
	**/
	private void matchTaskProgress(String line) {
		//TODO implement Scan Progress Matching

		ContentValues tp;

		Pattern pRun = Pattern.compile("^(.*)\\sTiming:\\sAbout\\s(\\d*)\\.?.*%\\sdone.*");
		Matcher mRun = pRun.matcher(line);

//		Pattern pFinish = Pattern.compile("^Completed\\s(.*)\\sat\\s\\d*:\\d*,.*elapsed.*");
//		Matcher mFinish = pFinish.matcher(line);

		if(mRun.matches() && mRun.groupCount()==2) {
			tp = new ContentValues();
			tp.put(Scans.TASK,mRun.group(1));
			tp.put(Scans.TASK_PROGRESS, mRun.group(2));
			contentResolver.update(scanUri,tp,null,null);
		} //else if(mFinish.matches() && mFinish.groupCount()==1) {
//			tp = new ContentValues();
//			tp.put(Scans.TASK,mRun.group(1));
//			tp.put(Scans.TASK_PROGRESS, 100);
//			contentResolver.update(scanUri,tp,null,null);
//		}
	}
}
