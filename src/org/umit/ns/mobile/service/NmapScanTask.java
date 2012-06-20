package org.umit.ns.mobile.service;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import org.umit.ns.mobile.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NmapScanTask implements Runnable, ScanCommunication {
    private final boolean hasRoot;
    private final StringBuffer scanResults;
    private final String scanArguments;
    private final Messenger mService;
    private final int id;

    private Process p;

    public NmapScanTask(final int id,
                        final IBinder service,
                        final String scanArguments,
                        final StringBuffer scanResults,
                        final boolean hasRoot) {

        Log.d("UmitScanner","NmapScanTask.NmapScanTask() ID:" + id);
        this.id = id;
        this.scanResults=scanResults;
        this.scanArguments=scanArguments;
        this.hasRoot=hasRoot;
        this.mService=new Messenger(service);
    }

    public void run() {
        Log.d("UmitScanner","NmapScanTask.run()");

        if(scanArguments==null) {
            Log.e("UmitScanner", "NmapScanTask.run() scanArguments is null");
            return;
        }
        if(scanResults==null) {
            Log.e("UmitScanner", "NmapScanTask.run() scanResults is null");
            return;
        }

        try{
            //TODO Necessary to request root/check for it in Activity
            p = Runtime.getRuntime().exec("su");
            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
            try {
                //TODO make it work with the correct location from string resource
                pOut.writeBytes("cd /data/local/nmap/bin \n");
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
                while ( ((read = reader.read(buffer)) > 0)) {
                    //TODO I'll probably use a ContentProvider in the future
                    scanResults.append(buffer, 0, read);
                }
                //scan finished
                p.destroy();
                Message msg = Message.obtain(null,NOTIFY_SCAN_FINISHED,id,0);
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    Log.d("UmitScanner",
                            "Caught Remote Exception while sending from ScanTask to ScanService:"+e.toString());
                }

                //TODO notify Service
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
