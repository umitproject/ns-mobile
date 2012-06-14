package org.umit.ns.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.widget.Toast;

//TODO Implement managing of Calling Activity, matching Scan Thread
//TODO Implement ScanObject
//TODO Implement Messages

public class ScanService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this,"Scan service starting",Toast.LENGTH_SHORT).show();

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        //this.startForeground();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Scan service destroyed", Toast.LENGTH_SHORT).show();
    }

    private final class ScanThreadHandler extends Handler {
        public ScanThreadHandler(Looper looper) {
            super(looper);
        }

        static final int MSG_SAY_HELLO = 1;
        static final int MSG_SCAN = 2;
        static final int MSG_STOP_SCAN = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SCAN:
                    Toast.makeText(getApplicationContext(), "The thread has started", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    //----BINDING-----
    static final int MSG_SAY_HELLO = 1;
    static final int MSG_SCAN = 2;
    static final int MSG_STOP_SCAN = 3;

    private final class ScanServiceHandler extends Handler {
        HandlerThread thread;
        Looper threadLooper;
        ScanThreadHandler threadHandler;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    break;

                case MSG_SCAN:
                    Toast.makeText(getApplicationContext(), "Scanning and spawning a new thread",Toast.LENGTH_SHORT).show();

                    thread = new HandlerThread("ScanningThread", Process.THREAD_PRIORITY_FOREGROUND);
                    thread.start();
                    threadLooper = thread.getLooper();
                    threadHandler = new ScanThreadHandler(threadLooper);

                    Message msg_thread = threadHandler.obtainMessage();
                    msg_thread.what=msg.what;
                    threadHandler.sendMessage(msg_thread);

                    break;

                case MSG_STOP_SCAN:
                    Toast.makeText(getApplicationContext(), "Killing",Toast.LENGTH_SHORT).show();
                    thread.stop();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new ScanServiceHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

        return mMessenger.getBinder();
    }
    //---\BINDING-----
}
