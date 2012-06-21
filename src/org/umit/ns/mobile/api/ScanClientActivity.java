package org.umit.ns.mobile.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;

public abstract class ScanClientActivity extends Activity implements ScanCommunication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindService( new Intent("org.umit.ns.mobile.service.ScanService"),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    //---Binding---
    private Messenger msgService;
    private boolean mBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            msgService = new Messenger(service);
            mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            msgService = null;
            mBound = false;
        }
    };


    private final class Scan {
        Scan(int id){
            this.id = id;
            this.started = false;
            this.finished = false;
        }

        protected final int id;
        public boolean started;
        public boolean finished;
    }

    private Scan scan;

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESP_SCAN_ID:
                    //Handle locally
                    scan = new Scan(msg.arg1);
                    break;

                case RESP_START_SCAN_OK:
                    onScanStart();
                    break;

                case RESP_STOP_SCAN_OK:
                    onScanStop();
                    break;

                case RESP_PROGRESS_OK:
                    //Handle locally
                    break;

                case RESP_RESULTS_OK:
                    //Handle locally
                    break;

                case NOTIFY_SCAN_FINISHED:
                    onScanFinish();
                    break;

//                case RESP_START_SCAN_ERR:
//                case RESP_STOP_SCAN_ERR:
//                case RESP_PROGRESS_ERR:
//                case RESP_RESULTS_ERR:
                case NOTIFY_SCAN_PROBLEM:
                    String info = ( ((Bundle)msg.obj).containsKey("Info") ?
                            ((Bundle)msg.obj).getString("Info") : "" );
                    onScanCrash(msg.arg1,info);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }
    //--\Binding---

    //---API---
    public final void startScan(String scanArguments) throws Exception {  //TODO Exception type?

    }

    public final void stopScan() throws Exception {   //TODO Exception type?
    }

    public final String getScanResults() throws Exception { //TODO Exception type?
    }

    public final int getScanProgress() throws Exception {
    }
    //--\API---

    //---Events---
    public abstract void onScanStart();
    public abstract void onScanStop();
    public abstract void onScanFinish();
    public abstract void onScanCrash(int RESP_CODE,String info);
    //--\Events---
}
