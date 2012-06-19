package org.umit.ns.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//TODO Handle Runtime Changes (may kill the thread)
//TODO Implement managing of Calling Activity, matching Scan Thread
//TODO Implement ScanObject
//TODO Handle Redelivered Intent
//TODO Notification for Service or Notification for each scan, but first design GUI with Ad and check with Adriano

public class ScanService extends Service implements ScanCommunication {

    //---Thread-specific vars (one for each scanning thread/request activity)
    StringBuffer scanResults;
    Messenger messengerActivity;
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

    public void scanDone(int ID) {

    }


    //----BINDING-----
    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RQST_START_SCAN:
                    break;
                case RQST_STOP_SCAN:
                    break;
                case RQST_PROGRESS:
                    break;
                case RQST_RESULTS:
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
