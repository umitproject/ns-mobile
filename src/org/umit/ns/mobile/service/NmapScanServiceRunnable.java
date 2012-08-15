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
	private final Messenger mService;
	private final int scanID;
	private final int clientID;
	private final Uri scanUri;
	private final ContentResolver contentResolver;
	private final String nativeInstallDir;
	private final String scanArguments;

	private volatile boolean stopRequested;

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
		this.stopRequested =false;
	}

	public synchronized void rqstStop() {
		stopRequested=true;
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

		//Acquire Runtime
		try {
			if (rootAccess)
				p = Runtime.getRuntime().exec("su");
			else {
				p = Runtime.getRuntime().exec("sh");
			}
		} catch (IOException e) {
			Log.d("UmitScanner.NmapScanServiceRunnable",e.toString());
			tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
			return;
		}

		DataOutputStream pOut = new DataOutputStream(p.getOutputStream());

		NmapSaxParser parser = new NmapSaxParser(contentResolver,
				Integer.toString(clientID),Integer.toString(scanID),scanResultsFile);

		try {
			Log.d("UmitScanner", "Executing $ " + "cd " + nativeInstallDir + "/nmap/bin \n");
			pOut.writeBytes("cd " + nativeInstallDir + "/nmap/bin \n");
			Log.d("UmitScanner", "Executing $ " + scanArguments + "\n");
			pOut.writeBytes(scanArguments + "\n");
			pOut.writeBytes("exit\n");
			pOut.flush();
		} catch (IOException e) {
			Log.d("UmitScannerService", e.toString());
			tellService(NOTIFY_SCAN_PROBLEM,e.getMessage());
			return;
		}

		BufferedReader stdOut;
		BufferedReader stdErr;
		stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
		stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		int read;
		char[] buffer = new char[1024];
		StringBuffer output = new StringBuffer();
		String err;
		StringBuilder keep = new StringBuilder();


		try{
			while ((read = stdOut.read(buffer)) > 0 && ! stopRequested) {
				output.append(buffer, 0, read);

				String [] splitOutput = output.toString().split("\n",-1);

				if(splitOutput.length>0){
					keep.append(splitOutput[0]);
					matchTaskProgress(keep.toString());

					keep = new StringBuilder();

					for(int i =1; i<(splitOutput.length-1); i++) {
						matchTaskProgress(splitOutput[i]);
					}

					keep.append(splitOutput[splitOutput.length-1]);
				} else {
					keep.append(output);
				}
				output = new StringBuffer();
			}

			if( ! TextUtils.isEmpty(keep)){
				matchTaskProgress(keep.toString());
			}

			p.waitFor();

			int exitValue = p.exitValue();
			if(exitValue!=0) {
				err = readToEnd(stdErr);
				Log.d("UmitScanner.ScanResults",err);
				tellService(NOTIFY_SCAN_PROBLEM,err);
			}

			stdOut.close();
			pOut.close();

			if(stopRequested){
				Log.d("UmitScanner", "Stop requested, stopping scan");
				return;
			}

		} catch(IOException e) {
			if (Thread.currentThread().isInterrupted()) {
				//manage abrupt stopping
				Log.d("UmitScanner", "InterruptedException (Probably Stopped) "+ e.toString());
				p.destroy();
				return;
			} else {
				tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
				Log.d("UmitScanner.ScanService", "ScanProblem:" + e.toString());
				return;
			}
		} catch (InterruptedException e) {
			Log.d("UmitScanner", "InterruptedException (Probably Stopped) "+ e.toString());
			p.destroy();
			return;
		}

		Log.d("UmitScannerRunnable", "Parsing results");

		tp=new ContentValues();
		tp.put(Scans.TASK,"Parsing Results");
		tp.put(Scans.TASK_PROGRESS,100);
		contentResolver.update(scanUri,tp,null,null);

		parser.parse();

		Log.d("UmitScannerRunnable", "Scan Finished");

		tp = new ContentValues();
		tp.put(Scans.TASK,"Scan Finished");
		tp.put(Scans.TASK_PROGRESS,100);
		tp.put(Scans.SCAN_STATE, Scans.SCAN_STATE_FINISHED);
		contentResolver.update(scanUri,tp,null,null);

		//scan finished

		new File(scanResultsFile).delete();

		tellService(NOTIFY_SCAN_FINISHED);
	}

	/** Matches Nmap Scan Output with a Scan Progress line
	 * @return String Array with the first element Task Name and second Task progress
	**/
	private void matchTaskProgress(String line) {
		ContentValues tp;

		Pattern pRun = Pattern.compile("^(.*)\\sTiming:\\sAbout\\s(\\d*)\\.?.*%\\sdone.*");
		Matcher mRun = pRun.matcher(line);

		if(mRun.matches() && mRun.groupCount()==2) {
			tp = new ContentValues();
			tp.put(Scans.TASK,mRun.group(1));
			tp.put(Scans.TASK_PROGRESS, mRun.group(2));
			contentResolver.update(scanUri,tp,null,null);
		}
	}

	private String readToEnd(BufferedReader reader) {
		int read;
		char[] buffer = new char[1024];
		StringBuffer output = new StringBuffer();

		try{
			while ((read = reader.read(buffer)) > 0 && ! stopRequested) {
				output.append(buffer, 0, read);
			}
		} catch (IOException e) {
			return "";
		}

		return output.toString();
	}
}
