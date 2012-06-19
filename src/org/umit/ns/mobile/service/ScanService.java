package org.umit.ns.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//TODO Handle Runtime Changes (may kill the thread)
//TODO Implement managing of Calling Activity, matching Scan Thread
//TODO Implement ScanObject
//TODO Handle Redelivered Intent
//TODO Notification for Service or Notification for each scan, but first design GUI with Ad and check with Adriano

public class ScanService extends Service implements ScanCommunication {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    //---Thread-specific vars (one for each scanning thread/request activity)
    private StringBuffer scanResults;
    private Messenger messengerActivity;
    private Future<?> futureScan;
    private String scanArguments;
    private boolean hasRoot;
    private boolean scanFinished;
    //--\Thread-specific vars

    @Override
    public void onCreate() {
        Log.d("UmitScanner","ScanService.onCreate()");
        //this.startForeground();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("UmitScanner","ScanService.onStartCommand()");
        return START_REDELIVER_INTENT;
    }


    @Override
    public void onDestroy() {
        Log.d("UmitScanner","ScanService.onDestroy()");
//        thread.stop();
    }

    //----BINDING-----
    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RQST_START_SCAN:
                    Log.d("UmitScanner","ScanService:RQST_START_SCAN");

                    scanFinished=false;

                    //Extract info from msg
                    messengerActivity = msg.replyTo;
                    String scanArguments = ((Bundle)msg.obj).getString("ScanArguments");
                    boolean hasRoot = ((Bundle)msg.obj).getBoolean("HasRoot");
                    scanResults = new StringBuffer();
                    Log.d("UmitScanner","FLAG1");

                    //Start scan with submit so we can call futureScan.cancel() if we want to stop it
                    futureScan = executorService.submit(
                            new NmapScanTask(mMessenger.getBinder(),scanArguments,scanResults,hasRoot));
                    Log.d("UmitScanner","FLAG2");
                    break;

                case RQST_STOP_SCAN:
                    Log.d("UmitScanner","ScanService:RQST_STOP_SCAN");
                    // :)
                    futureScan.cancel(true);
                    break;

                case RQST_PROGRESS:
                    Log.d("UmitScanner","ScanService:RQST_PROGRESS");
                    //TODO implement progress
                    break;

                case RQST_RESULTS:
                    Log.d("UmitScanner","ScanService:RQST_RESULTS");
                    //Build Message
                    Message messageActivity;
                    if(scanFinished){
                        messageActivity=Message.obtain(null, RESP_RESULTS_OK);
                        Bundle bundle = new Bundle();
                        bundle.putString("ScanResults",scanResults.toString());
                        messageActivity.obj=bundle;
                    } else {
                        messageActivity=Message.obtain(null, RESP_RESULTS_ERR);
                    }
                    //Send Message
                    try {
                        messengerActivity.send(messageActivity);
                    } catch (RemoteException e) {
                        Log.d("UmitScanner",
                                "Caught Remote Exception while sending RESP_RESULTS from ScanService to ScanActivity:"+e.toString());
                    }
                    break;

                case NOTIFY_SCAN_FINISHED:
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_FINISHED");

                    scanFinished=true;
                    Message messageActivity2 = Message.obtain(null,NOTIFY_SCAN_FINISHED);
                    try {
                        messengerActivity.send(messageActivity2);
                    } catch (RemoteException e) {
                        Log.d("UmitScanner",
                                "Caught Remote Exception while sending NOTIFY_SCAN_FINISHED from ScanService to ScanActivity:"+e.toString());
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new ScanServiceHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("UmitScanner","ScanService.onBind()");
        return mMessenger.getBinder();
    }
    //---\BINDING-----
}
