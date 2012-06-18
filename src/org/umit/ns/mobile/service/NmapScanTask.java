package org.umit.ns.mobile.service;

import android.util.Log;
import org.umit.ns.mobile.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NmapScanTask implements Runnable {
    private final boolean hasRoot;
    private final StringBuffer scanResults;
    private final String scanArguments;
    private volatile boolean cancelled;
    private Process p;

    public synchronized void cancel() {
        cancelled=true;
    }

    public synchronized boolean isCancelled(){
        return cancelled;
    }

    public NmapScanTask(final String scanArguments,
                        final StringBuffer scanResults,
                        final boolean hasRoot) {

        Log.d("UmitScanner","NmapScanTask:NmapScanTask()");
        this.scanResults=scanResults;
        this.scanArguments=scanArguments;
        this.hasRoot=hasRoot;
        cancelled=false;
    }

    public void run() {
        Log.d("UmitScanner","NmapScanTask:run()");

        try{
            //TODO Necessary to request root/check for it in Activity
            p = Runtime.getRuntime().exec("su");
            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
            try {
                //TODO make it work with the correct location
                pOut.writeBytes("cd /data/local\n");
                pOut.writeBytes(scanArguments + "\n");
                pOut.writeBytes("exit\n");
                pOut.flush();
            }
            catch (IOException e) {
                //manage abrupt stopping
                if(Thread.currentThread().isInterrupted()) {
                    Log.d("UmitScanner","Interrupted from blocked I/O");
                    p.destroy();
                } else {
                    //TODO manage exception
                    //throw new RuntimeException(e);
                    e.printStackTrace();
                }
            }

            int read;
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            char[] buffer = new char[1024];
            try{
                while ( ((read = reader.read(buffer)) > 0) & ! isCancelled()) {
                    //TODO I'll probably use a ContentProvider in the future
                    scanResults.append(buffer, 0, read);
                }
                p.destroy();
            }
            catch(IOException e) {
                if(Thread.currentThread().isInterrupted()) {
                    //manage abrupt stopping
                    Log.d("UmitScanner","Interrupted from blocked I/O");
                    p.destroy();
                } else {
                    //TODO manage exception
                    //throw new RuntimeException(e);
                    e.printStackTrace();
                }
            }
        }
        catch(IOException e) {
            if(Thread.currentThread().isInterrupted()) {
                //manage abrupt stopping
                Log.d("UmitScanner","Interrupted from blocked I/O");
                p.destroy();
            } else {
                //TODO manage exception
                //throw new RuntimeException(e);
                e.printStackTrace();
            }
        }
    }
}
