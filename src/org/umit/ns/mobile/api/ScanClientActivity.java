package org.umit.ns.mobile.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import org.umit.ns.mobile.R;

public abstract class ScanClientActivity extends Activity implements ScanCommunication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindService( new Intent("org.umit.ns.mobile.service.ScanService"),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
    //---Binding---
    private Messenger msgrService;
    private boolean mBound;

    private final Messenger msgrLocal = new Messenger(new IncomingHandler());

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            msgrService = new Messenger(service);
            mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            msgrService = null;
            mBound = false;
        }
    };


    private final class Scan {
        Scan(){
            this.started = false;
            this.finished = false;
        }

        public int id;
        public boolean rootAccess;
        public String scanArguments;
        public String scanResults;
        public int progress;
        public boolean started;
        public boolean finished;
    }

    protected Scan scan;

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESP_SCAN_ID_OK:
                    //Handle locally
                    scan.id = msg.arg1;
                    scan.rootAccess = (msg.arg2==1);
                    if(scan.rootAccess)
                        Toast.makeText(getApplicationContext(), R.string.service_acquire_root_ok,Toast.LENGTH_SHORT).show();
                    else {
                        Toast.makeText(getApplicationContext(), R.string.service_acquire_root_err,Toast.LENGTH_SHORT).show();
                    }

                    if(!mBound){
                        return;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("ScanArguments",scan.scanArguments);
                    tellService(RQST_START_SCAN, scan.id, 0, bundle, msgrLocal);
                    break;

                case RESP_START_SCAN_OK:
                    scan.started=true;
                    onScanStart();
                    break;

                case RESP_STOP_SCAN_OK:
                    scan=new Scan();
                    onScanStop();
                    break;

                case RESP_PROGRESS_OK:
                    scan.progress=msg.arg2;
                    onScanProgressReceive(scan.progress);
                    break;

                case RESP_RESULTS_OK:
                    scan.scanResults=((Bundle)msg.obj).getString("Info");
                    onScanResultsReceive(scan.scanResults);
                    break;

                case NOTIFY_SCAN_FINISHED:
                    scan.finished=true;
                    onScanFinish();
                    break;

                case RESP_SCAN_ID_ERR:
                case RESP_START_SCAN_ERR:
                case RESP_STOP_SCAN_ERR:
                case RESP_PROGRESS_ERR:
                case RESP_RESULTS_ERR:
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


    private boolean tellService(int RESP_CODE,
                                int scanID,
                                int msg_arg2,
                                Bundle bundle,
                                Messenger replyTo){

        Log.d("UmitScanner","ScanActivity.tellService():RESP_CODE="+RESP_CODE);

        Message msg;

        if(bundle != null)
            msg = Message.obtain(null,RESP_CODE,scanID,msg_arg2,bundle);
        else
            msg = Message.obtain(null,RESP_CODE,scanID,msg_arg2);

        if (replyTo!=null)
            msg.replyTo=replyTo;

        try {
            msgrService.send(msg);
        } catch (RemoteException e) {
            Log.d("UmitScanner", "ScanActivity.tellService():could not send message.");
            return false;
        }
        return true;
    }

    //---API---
    public final void startScan(String scanArguments) {
        //Check if bound
        if(!mBound){
            return;
        }
        //getID
        tellService(RQST_SCAN_ID,0,0,null,msgrLocal);
        scan = new Scan();
        scan.scanArguments=scanArguments;
        //The scan continues to start once we've gotten an ID: RESP_SCAN_ID
    }


    public final void stopScan() {
        if(!mBound)
            return;

        if(!scan.started)
            return;

        tellService(RQST_STOP_SCAN,scan.id,0,null,msgrLocal);
    }

    public final void getScanResults() {
        if(!mBound)
            return;
        if(!scan.started)
            return;
        //check if finished?
        tellService(RQST_RESULTS,scan.id,0,null,msgrLocal);
    }

    public final void getScanProgress() {
        if(!mBound)
            return;
        if(!scan.started)
            return;
        tellService(RQST_PROGRESS,scan.id,0,null,msgrLocal);
    }
    //--\API---

    //---Events---
    public abstract void onScanStart();
    public abstract void onScanStop();
    public abstract void onScanFinish();
    public abstract void onScanResultsReceive(String scanResults);
    public abstract void onScanProgressReceive(int progress);
    public abstract void onScanCrash(int RESP_CODE,String info);
    //--\Events---
}
