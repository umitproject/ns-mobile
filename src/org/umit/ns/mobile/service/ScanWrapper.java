package org.umit.ns.mobile.service;

import android.os.IBinder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ScanWrapper {
    private final int ID;
    protected final String arguments;
    protected final String scanResultsFilename;

    //Update regularly
    public boolean running=false;

    //THIS IS ONLY UPDATED WHEN A MESSAGE DOESN'T REACH THE CLIENT
    private int progress=0;

    protected Future<?> future;

    //Must use the same one to get the benefit of managed threads
    private final static ExecutorService executorService=ScanService.getExecutorService();
    private static IBinder serviceBinder = ScanService.getBinder();
    private final static String nativeInstallDir=ScanService.getNativeInstallDir();
    private boolean notifyProgress;

    public ScanWrapper(int scanID,
                       String scanArguments,
                       String scanResultsPath) {

        this.arguments=scanArguments;
        this.ID=scanID;
        this.scanResultsFilename=scanResultsPath+ID;
    }

    //Update runnning
    public void start(boolean rootAccess) {
        future = executorService.submit(
                new NmapScanServiceRunnable( ID, serviceBinder, arguments,
                        scanResultsFilename, rootAccess, nativeInstallDir));
        running=true;
    }

    //Update runnning
    //Stop only if started
    public void stop() {
        if(running){
            future.cancel(true);
            running=false;
        }
    }

    public int getID(){
        return this.ID;
    }

    protected void setProgress(int progress){
        this.progress=progress;
    }

    protected int getProgress() {
        return progress;
    }

    protected void setNotifyProgress(boolean notifyProgress) {
        this.notifyProgress=notifyProgress;
    }

    protected boolean getNotifyProgress() {
        return notifyProgress;
    }
}
