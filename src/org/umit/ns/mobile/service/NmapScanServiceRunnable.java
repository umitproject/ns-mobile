package org.umit.ns.mobile.service;

import android.os.*;
import android.util.Log;
import org.umit.ns.mobile.api.ScanCommunication;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NmapScanServiceRunnable implements Runnable, ScanCommunication {
    private final boolean rootAccess;
    private final StringBuffer scanResults;
    private final String scanArguments;
    private final Messenger mService;
    private final int id;
    private final String nativeInstallDir;

    private java.lang.Process p;

    public NmapScanServiceRunnable(final int id,
                                   final IBinder service,
                                   final String scanArguments,
                                   final StringBuffer scanResults,
                                   final boolean hasRoot,
                                   final String nativeInstallDir) {

        Log.d("UmitScanner","NmapScanTask.NmapScanTask() ID:" + id);
        this.id = id;
        this.scanResults=scanResults;
        this.scanArguments=scanArguments + " -oX " + id + ".xml";
        this.rootAccess=hasRoot;
        this.mService=new Messenger(service);
        this.nativeInstallDir = nativeInstallDir;
    }

    private void tellService(int RESP_CODE){
        Log.d("UmitScanner","tellService():RESP_CODE="+RESP_CODE);
        Message msg = Message.obtain(null,RESP_CODE,id,0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d("UmitScanner",
                    "Caught Remote Exception while sending from ScanTask to ScanService:"+e.toString());
        }
    }

    private void tellService(int RESP_CODE, String info) {
        Log.d("UmitScanner","tellService():RESP_CODE="+RESP_CODE+"; info="+info);
        Message msg = Message.obtain(null,RESP_CODE,id,0);
        Bundle bundle = new Bundle();
        bundle.putString("Info", info);
        msg.obj=bundle;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d("UmitScanner",
                    "Caught Remote Exception while sending from ScanTask to ScanService:"+e.toString());
        }
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
            if(rootAccess)
                p = Runtime.getRuntime().exec("su");
            else {
                p = Runtime.getRuntime().exec("sh");
            }

            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
            try {
                Log.d("UmitScanner","Executing $ "+"cd " + nativeInstallDir + "/nmap/bin \n");
                pOut.writeBytes("cd " + nativeInstallDir + "/nmap/bin \n");
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
                    tellService(NOTIFY_SCAN_PROBLEM,e.getMessage());
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
                tellService(NOTIFY_SCAN_FINISHED);
            }
            catch(IOException e) {
                if(Thread.currentThread().isInterrupted()) {
                    //manage abrupt stopping
                    Log.d("UmitScanner","Interrupted from blocked I/O");
                    p.destroy();
                } else {
                    tellService(NOTIFY_SCAN_PROBLEM,e.getMessage());
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
                tellService(NOTIFY_SCAN_PROBLEM, e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
