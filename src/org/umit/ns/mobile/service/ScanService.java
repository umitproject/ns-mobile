package org.umit.ns.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import org.umit.ns.mobile.api.ScanCommunication;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//TODO Handle Runtime Changes (may kill the thread)
//TODO Handle Redelivered Intent
//TODO Notification for Service or Notification for each scan, but first design GUI with Ad and check with Adriano

public class ScanService extends Service implements ScanCommunication {

    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private Random random=new Random();

    private HashMap<Integer,Scan> scans = new HashMap<Integer, Scan>();

    //---Thread-specific vars (one for each scanning thread/request activity)
    private class Scan {
        protected int id;
        protected boolean hasRoot;

        protected Messenger messengerActivity;
        public StringBuffer results;
        public Future<?> future;
        public String arguments;

        public boolean started;
        public boolean finished;

        Scan(Messenger messengerActivity, boolean hasRoot) {
            id = random.nextInt();
            results = new StringBuffer();
            this.messengerActivity=messengerActivity;
            this.hasRoot=hasRoot;
            started = false;
            finished = false;
        }

        public boolean tellActivity(int RESP_CODE){
            try{
                messengerActivity.send(Message.obtain(null,RESP_CODE,id,0));
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                return false;
            }
            return true;
        }

        public boolean tellActivity(int RESP_CODE, String info){
            Bundle bundle = new Bundle();
            bundle.putString("Info", info);
            Log.d("UmitScanner", "tellActivity():"+info);
            try{
                messengerActivity.send(Message.obtain(null, RESP_CODE, id, 0, bundle));
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                return false;
            }
            return true;
        }

        public boolean tellActivity(int RESP_CODE, Bundle bundle){
            Log.d("UmitScanner","tellActivity():Bundle");
            try{
                messengerActivity.send(Message.obtain(null, RESP_CODE, id, 0, bundle));
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                return false;
            }
            return true;
        }

        public boolean tellActivity(int RESP_CODE, int msg_arg2){
            Log.d("UmitScanner","tellActivity():msg.arg2="+msg_arg2);
            try{
                messengerActivity.send(Message.obtain(null, RESP_CODE, id, msg_arg2));
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                return false;
            }
            return true;
        }

        public void start(String scanArguments) {
            this.arguments = scanArguments;

            //Start scan with submit so we can call futureScan.cancel() if we want to stop it
            future = executorService.submit(
                    new NmapScanServiceRunnable(id, mMessenger.getBinder(),arguments,results,hasRoot));

            started=true;
            finished=false;
        }

        public boolean stop() {
            started = false;
            finished = false;
            return future.cancel(true);
        }

        public int progress() {
            //TODO implement progress
            return 1;
        }
    }
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
        //Stop all scans, remove all Scan objects
        Iterator it = scans.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            Scan scan = (Scan)pairs.getValue();
            scan.stop();
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    //----BINDING-----
    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RQST_SCAN_ID:{
                    //Check if Activity has sent a messenger in replyTo
                    if(msg.replyTo==null){
                        Log.d("UmitScanner","ScanService:RQST_SCAN_ID no msg.replyTo present");
                        break;
                    }
                    if(msg.obj==null || !((Bundle)msg.obj).containsKey("HasRoot")){
                        Log.d("UmitScanner","ScanService:RQST_SCAN_ID no msg.obj:HasRoot boolean present");
                        break;
                    }

                    boolean hasRoot = ((Bundle)msg.obj).getBoolean("HasRoot");
                    Scan scan = new Scan(msg.replyTo,hasRoot);
                    scans.put(scan.id, scan);
                    //Scan ID Response
                    scan.tellActivity(RESP_SCAN_ID);
                    break;
                }

                case RQST_START_SCAN: {
                    Log.d("UmitScanner","ScanService:RQST_START_SCAN");

                    Scan scan = scans.get(msg.arg1);
                    if(scan == null){
                        scan.tellActivity(RESP_START_SCAN_ERR, "ScanService:RQST_START_SCAN no scan with specified id present");
                        break;
                    }
                    if(scan.started) {
                        scan.tellActivity(RESP_START_SCAN_ERR,"ScanService:RQST_START_SCAN Scan already started");
                        break;
                    }
                    if(msg.obj==null || !((Bundle)msg.obj).containsKey("ScanArguments")) {
                        scan.tellActivity(RESP_START_SCAN_ERR, "ScanService:RQST_START_SCAN no msg.obj:ScanArguments string present");
                        break;
                    }

                    String scanArguments = ((Bundle)msg.obj).getString("ScanArguments");
                    scan.start(scanArguments);
                    scan.tellActivity(RESP_START_SCAN_OK);
                    break;
                }

                case RQST_STOP_SCAN:{
                    Log.d("UmitScanner","ScanService:RQST_STOP_SCAN");
                    Scan scan = scans.get(msg.arg1);
                    if(scan==null) {
                        scan.tellActivity(RESP_STOP_SCAN_ERR,"ScanService:RQST_STOP_SCAN no scan with specified id present");
                        break;
                    }
                    if(!(scan.started)) {
                        scan.tellActivity(RESP_STOP_SCAN_ERR,"ScanService:RQST_STOP_SCAN scan is not started");
                        break;
                    }
                    scan.stop();
                    scan.tellActivity(RESP_STOP_SCAN_OK);
                    break;
                }

                case RQST_PROGRESS:{
                    Log.d("UmitScanner","ScanService:RQST_PROGRESS");
                    if(false) {
                        Scan scan = scans.get(msg.arg1);
                        if(scan==null) {
                            scan.tellActivity(RESP_PROGRESS_ERR,"ScanService:RESP_PROGRESS_ERR no scan with specified id present");
                            break;
                        }
                        if(!(scan.started)) {
                            scan.tellActivity(RESP_PROGRESS_ERR,"ScanService:RESP_PROGRESS_ERR scan is not started");
                            break;
                        }
                        scan.tellActivity(RESP_PROGRESS_OK,scan.progress());
                    }

                    break;
                }

                case RQST_RESULTS:{
                    Log.d("UmitScanner","ScanService:RQST_RESULTS");
                    //TODO test realtime results

                    Scan scan = scans.get(msg.arg1);
                    if(scan==null) {
                        scan.tellActivity(RESP_RESULTS_ERR,"ScanService:RESP_RESULTS_ERR no scan with specified id present");
                        break;
                    }
                    if(!(scan.started)) {
                        scan.tellActivity(RESP_RESULTS_ERR,"ScanService:RESP_RESULTS_ERR scan is not started");
                        break;
                    }
//                    if(!(scan.finished)) {
//                        scan.tellActivity(RESP_RESULTS_ERR,"ScanService:RESP_RESULTS_ERR scan has not finished");
//                        break;
//                    }

                    scan.tellActivity(RESP_RESULTS_OK,scan.results.toString());
                    //Once we've sent the results, we can dispense with them.
                    scans.remove(msg.arg1);
                    break;
                }

                case NOTIFY_SCAN_FINISHED:{
                    //Service receives it from ScanThread
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_FINISHED");
                    Scan scan = scans.get(msg.arg1);
                    scan.finished=true;
                    scan.tellActivity(NOTIFY_SCAN_FINISHED);
                    break;
                }

                case NOTIFY_SCAN_PROBLEM:{
                    //Service receives it from ScanThread
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_PROBLEM");
                    Scan scan = scans.get(msg.arg1);
                    scan.tellActivity(NOTIFY_SCAN_PROBLEM,((Bundle)msg.obj).getString("Info"));
                    break;
                }

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
