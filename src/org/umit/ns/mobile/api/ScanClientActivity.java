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

import java.io.*;

public abstract class ScanClientActivity extends Activity implements ScanCommunication {

    private Messenger msgrService;
    private boolean mBound;
    private final Messenger msgrLocal = new Messenger(new IncomingHandler());

    protected Scan scan;
    public boolean was_connected=false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            msgrService = new Messenger(service);
            mBound = true;
            if(was_connected&&scan!=null){
                tellService(RQST_REBIND_CLIENT,scan.clientID,0,null,msgrLocal);
            } else {
                tellService(RQST_REG_CLIENT,0,0,null,msgrLocal);
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            msgrService = null;
            mBound = false;

        }
    };

    protected final class Scan {
        Scan(){
            this.started = false;
            this.finished = false;
        }
        Scan(Scan s){
            this.started=s.started;
            this.finished=s.finished;
            this.clientID=s.clientID;
            this.id  = s.id;
            this.rootAccess=s.rootAccess;
            this.scanArguments=s.scanArguments;
            this.scanResults=s.scanResults;
            this.scanResultsFile=s.scanResultsFile;
            this.progress=s.progress;
        }
        public int clientID;

        public int id;
        public boolean rootAccess;
        public String scanArguments;
        public String scanResults;
        public String scanResultsFile;
        public int progress;
        public boolean started;
        public boolean finished;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Scan retained_scan = (Scan)getLastNonConfigurationInstance();
        if(retained_scan!=null) {
            was_connected=true;
            scan = retained_scan;
        }

        bindService( new Intent("org.umit.ns.mobile.service.ScanService"),
                serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("UmitScanner","BoundToService");
    }

    @Override
    public Object onRetainNonConfigurationInstance ()  {
        super.onRetainNonConfigurationInstance();
        final Scan s = scan;
        return  s;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isFinishing()) {

        }

        unbindService(serviceConnection);
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESP_REG_CLIENT_OK:
                    scan = new Scan();
                    scan.clientID=msg.arg1;
                    scan.rootAccess=(msg.arg2==1);
                    onRegisterClient(scan.rootAccess);
                    break;

                case RESP_NEW_SCAN_OK:
                    scan.id=msg.arg1;
                    scan.scanResultsFile=((Bundle)msg.obj).getString("Info");
                    onNewScan(scan.id);
                    break;

                case RESP_START_SCAN_OK:
                    scan.started=true;
                    onScanStart();
                    break;

                case RESP_STOP_SCAN_OK:
                    scan = new Scan();
                    onScanStop();
                    break;

                case RESP_REBIND_CLIENT_OK:
                    Log.d("UmitScanner","RESP_REBIND_CLIENT");
                    onRebindClient();
                    break;

                case NOTIFY_SCAN_PROGRESS:
                    scan.progress = msg.arg2;
                    onNotifyProgress(scan.progress);
                    break;

                case NOTIFY_SCAN_FINISHED:
                    scan.finished=true;
                    //TODO get results from file here
                    try {
                        BufferedReader inputStream = new BufferedReader(new FileReader(scan.scanResultsFile));
                        StringBuffer scanResults = new StringBuffer();
                        int read;
                        char[] buffer = new char[1024];
                        while ( ((read = inputStream.read(buffer)) > 0)) {
                            //TODO I'll probably use a ContentProvider in the future
                            scanResults.append(buffer, 0, read);
                        }
                        scan.scanResults=scanResults.toString();
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getApplicationContext(),"Could not open scan results file.",Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Log.d("UmitScanner","Could not close fileInputStream:"+e.getMessage()) ;
                    }

                    onNotifyFinished(scan.scanResults);
                    break;

                case NOTIFY_SCAN_PROBLEM:
                case RESP_REG_CLIENT_ERR:
                case RESP_NEW_SCAN_ERR:
                case RESP_START_SCAN_ERR:
                case RESP_STOP_SCAN_ERR:
                case RESP_REBIND_CLIENT_ERR:
                    String info = ( ((Bundle)msg.obj).containsKey("Info") ?
                            ((Bundle)msg.obj).getString("Info") : "" );

                    onNotifyProblem(msg.what, info);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    //========API

    public final void newScan() {

        if(!mBound){
            Log.d("UmitScanner","ScanClientActivity:newScan() not bound");
            return;
        }
        if(was_connected){
            int clientID = scan.clientID;
            scan = new Scan();
            scan.clientID=clientID;
        }
        if(scan.started){
            Log.d("UmitScanner","ScanClientActivity:newScan() scan already started");
            return;
        }
        tellService(RQST_NEW_SCAN, scan.clientID, 0, null, null);
    }

    public final void startScan(String scanArguments) {
        if(!mBound)
            return;

        if(scan.started)
            return;

        scan.scanArguments = scanArguments;

        Bundle bundle = new Bundle();
        bundle.putString("ScanArguments",scan.scanArguments);

        tellService(RQST_START_SCAN, scan.clientID, scan.id, bundle, null);
    }

    public final void stopScan() {
        if(!mBound)
            return;

        if(!scan.started)
            return;

        tellService(RQST_STOP_SCAN, scan.clientID, scan.id, null, null);
    }


    protected abstract void onRegisterClient(boolean rootAccess);

    protected abstract void onNewScan(int id);

    protected abstract void onScanStart();

    protected abstract void onScanStop();

    protected abstract void onRebindClient();

    protected abstract void onNotifyProgress(int progress);

    protected abstract void onNotifyProblem(int what, String info);

    protected abstract void onNotifyFinished(String scanResults);

    //=======\API

    private boolean tellService(int RQST_CODE,
                                int scanID,
                                int msg_arg2,
                                Bundle bundle,
                                Messenger replyTo){

        Log.d("UmitScanner","ScanActivity.tellService():RESP_CODE="+RQST_CODE);

        Message msg;

        if(bundle != null)
            msg = Message.obtain(null,RQST_CODE,scanID,msg_arg2,bundle);
        else
            msg = Message.obtain(null,RQST_CODE,scanID,msg_arg2);

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
}
