package org.umit.ns.mobile.service;

import android.content.ContentResolver;
import android.os.IBinder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class ScanWrapper {
	private final int scanID;
	private final int clientID;
	private final ContentResolver contentResolver;
	protected final String arguments;
	protected final String scanResultsFilename;

	//Update regularly
	public boolean running = false;

	//THIS IS ONLY UPDATED WHEN A MESSAGE DOESN'T REACH THE CLIENT
	private int progress = 0;

	private NmapScanServiceRunnable runnable;
	protected Future<?> future;

	//Must use the same one to get the benefit of managed threads
	private final static ExecutorService executorService = ScanService.getExecutorService();
	private static IBinder serviceBinder = ScanService.getBinder();
	private final static String nativeInstallDir = ScanService.getNativeInstallDir();
	private boolean notifyProgress;

	public ScanWrapper(int scanID,
	                   int clientID,
	                   ContentResolver contentResolver,
	                   String scanArguments,
	                   String scanResultsPath) {

		this.arguments = scanArguments;
		this.scanID = scanID;
		this.clientID = clientID;
		this.contentResolver = contentResolver;
		this.scanResultsFilename = scanResultsPath + this.scanID;
	}

	//Update runnning
	public void start(boolean rootAccess) {
		runnable = new NmapScanServiceRunnable(scanID,clientID,contentResolver, serviceBinder, arguments,
				scanResultsFilename, rootAccess, nativeInstallDir);
		future = executorService.submit(runnable);
		running = true;
	}

	//Update runnning
	//Stop only if started
	public void stop() {
		if (running) {
			runnable.rqstStop();
			future.cancel(true);
			running = false;
		}
	}

	public int getScanID() {
		return this.scanID;
	}

	protected void setProgress(int progress) {
		this.progress = progress;
	}

	protected int getProgress() {
		return progress;
	}

	protected void setNotifyProgress(boolean notifyProgress) {
		this.notifyProgress = notifyProgress;
	}

	protected boolean getNotifyProgress() {
		return notifyProgress;
	}
}
