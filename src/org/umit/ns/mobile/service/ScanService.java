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

public class ScanService extends Service {

    static final int MSG_START_SCAN=1; //expecting obj with a bundle with "ScanArguments", as well as replyTo
    static final int MSG_STOP_SCAN=2;
    static final int MSG_GET_RESULTS=3;

    //---Thread-specific vars (one for each scanning thread/request activity)
    HandlerThread thread;
    Looper threadLooper;
    ScanThreadHandler threadHandler;
    StringBuffer scanResults;
    Messenger messengerActivity;
    java.lang.Process p;
    //--\Thread-specific vars

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("UmitScanner","ScanService.onStartCommand()");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.d("UmitScanner","ScanService.onCreate()");

        //this.startForeground();

    }

    @Override
    public void onDestroy() {
        Log.d("UmitScanner","ScanService.onDestroy()");
        thread.stop();
    }

    //The ScanThreadHandler is used only for running the scan
    private final class ScanThreadHandler extends Handler {

        public ScanThreadHandler(Looper looper) {
            super(looper);
        }

        /*  ScanThreadHandler only gets MSG_START_SCAN.
        *   No point in using other Messages because it
        *   doesn't handle them concurrently.
        */
        @Override
        public void handleMessage(Message msg) {
            Log.d("UmitScanner","ScanThread.handleMessage() #StartingScan");

            String cmd = ((Bundle)msg.obj).getString("ScanArguments");

            try{
                //TODO Necessary to request root/check for it
                p = Runtime.getRuntime().exec("su");
                DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
                try {
                    pOut.writeBytes("cd /data/local\n");
                    pOut.writeBytes(cmd + "\n");
                    pOut.writeBytes("exit\n");
                    pOut.flush();
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }

                int read;
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                char[] buffer = new char[1024];
                try{
                    while ((read = reader.read(buffer)) > 0) {
                        //TODO I'll probably use a ContentProvider in the future
                        scanResults.append(buffer, 0, read);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    //----BINDING-----
    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_START_SCAN:
                    Log.d("UmitScanner","ScanService:MSG_START_SCAN");

                    //Resolve msg.replyTo
                    messengerActivity=msg.replyTo;
                    Log.d("UmitScanner","StartScan:msgReplyTo:"+msg.replyTo.toString());

                    //Start Thread
                    thread = new HandlerThread("ScanningThread", Process.THREAD_PRIORITY_BACKGROUND);
                    thread.start();
                    threadLooper = thread.getLooper();
                    threadHandler = new ScanThreadHandler(threadLooper);

                    scanResults = new StringBuffer();

                    //pass scanning message
                    Message msg_thread = threadHandler.obtainMessage();
                    msg_thread.what=msg.what;
                    msg_thread.obj=msg.obj;

                    threadHandler.sendMessage(msg_thread);
                    break;

                case MSG_STOP_SCAN:
                    Log.d("UmitScanner","ScanService:MSG_STOP_SCAN");
                    p.destroy();
                    thread.stop();
                    break;

                case MSG_GET_RESULTS:
                    Log.d("UmitScanner","ScanService:MSG_GET_RESULTS");

                    try {
                        messengerActivity.send(Message.obtain(null, MSG_GET_RESULTS, (Object) (scanResults.toString())));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        p.destroy();
                        thread.stop();
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
