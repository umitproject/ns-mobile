package org.umit.ns.mobile.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.util.Log;
import org.umit.ns.mobile.R;
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

public class ScanService extends Service implements ScanCommunication{

    private NotificationManager mNM;
    private final Messenger msgrLocal = new Messenger(new ScanServiceHandler());
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private HashMap<Integer,Scan> scans = new HashMap<Integer, Scan>();
    private Random random=new Random();

    private boolean rootAccess=false;
    private boolean nativeInstalled=false;

    private boolean pending_RQST_SCAN_ID=false;

    private boolean serviceReady=false;

    private int serviceNotificationID;

    public IBinder onBind(Intent intent) {
        Log.d("UmitScanner","ScanService.onBind()");
        startService(new Intent(this, ScanService.class));
        return msgrLocal.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if(scans.size()==0)
            stopSelf();

        //Just in case some scans have Scan objects but aren't started. Ex. pending_RQST_SCAN_ID=true :)
        boolean stop = true;
        for(Scan scan : scans.values())
            if(scan.started){
                stop=false;
                break;
            }

        if(stop)
            stopSelf();

        return false; //TODO onRebind implement!
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("UmitScanner","ScanService.onCreate()");

        serviceNotificationID=random.nextInt();
        mNM=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(serviceNotificationID,getNotification(R.string.service_ready));

        Log.d("UmitScanner","Notification shown");

        //TODO this.startForeground
        executorService.execute(new RootAcquisitionRunnable(msgrLocal.getBinder()));
        mNM.notify(serviceNotificationID, getNotification(R.string.service_acquire_root));

        Log.d("UmitScanner","RootAcquisition fired");

        SharedPreferences settings = getSharedPreferences("native", 0);
        nativeInstalled = settings.getBoolean("nativeInstalled", false);
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d("UmitScanner","ScanService.onStartCommand()");
//        return START_REDELIVER_INTENT;
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("UmitScanner","ScanService.onDestroy()");
        //Stop all scans, remove all Scan objects
        for(Scan scan:scans.values())
            scan.stop();
        scans.clear();
        mNM.cancel(serviceNotificationID);

    }

    //TODO if the activity dies, there should be no scan? (the tellActivity will return false)
    //TODO take account of bound activities?
    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RQST_SCAN_ID:{
                    Log.d("UmitScanner","ScanService:RQST_SCAN_ID");
                    if(msg.replyTo==null){
                        Log.d("UmitScanner","ScanService:RQST_SCAN_ID no msg.replyTo present");
                        break;
                    }

                    Scan scan = new Scan(msg.replyTo);
                    scans.put(scan.id, scan);

                    if(serviceReady)
                        scan.tellActivity(RESP_SCAN_ID_OK,(int)(rootAccess?1:0));
                    else
                        pending_RQST_SCAN_ID = true;

                    break;
                }
                case RQST_START_SCAN:{
                    Log.d("UmitScanner","ScanService:RQST_START_SCAN");
                    Scan scan = scans.get(msg.arg1);
                    if(scan == null){
                        if(msg.replyTo!=null){
                            scan = new Scan(msg.replyTo);
                            scan.id=msg.arg1;
                            scan.tellActivity(RESP_START_SCAN_ERR, "ScanService:RQST_START_SCAN no scan with specified id present");
                        }
                        break;
                    }
                    if(msg.replyTo!=null)
                        scan.messengerActivity=msg.replyTo;
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

                    //TODO TEMPORARY - NOTIFICATION METHOD ONLY SUPPORTS ONE SCAN
                    mNM.notify(serviceNotificationID, getNotification(R.string.service_scan_running));
                    break;
                }
                case RQST_STOP_SCAN:{
                    Log.d("UmitScanner","ScanService:RQST_STOP_SCAN");

                    Scan scan = scans.get(msg.arg1);
                    if(scan == null){
                        if(msg.replyTo!=null){
                            scan = new Scan(msg.replyTo);
                            scan.id=msg.arg1;
                            scan.tellActivity(RESP_STOP_SCAN_ERR, "ScanService:RQST_STOP_SCAN no scan with specified id present");
                        }
                        break;
                    }
                    if(msg.replyTo!=null)
                        scan.messengerActivity=msg.replyTo;
                    if(!scan.started) {
                        scan.tellActivity(RESP_STOP_SCAN_ERR,"ScanService:RQST_STOP_SCAN scan is not started");
                        break;
                    }

                    scan.stop();
                    scan.tellActivity(RESP_STOP_SCAN_OK);

                    //delete the scan
                    int scanID = scan.id;
                    scan = null;
                    scans.remove(scanID);

                    if(scans.size()==0)
                        stopSelf();

                    //TODO TEMPORARY - NOTIFICATION METHOD ONLY SUPPORTS ONE SCAN
                    mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));
                    break;
                }
                case RQST_PROGRESS:{
                    Log.d("UmitScanner","ScanService:RQST_PROGRESS");

                    if(false) {
                        Scan scan =scans.get(msg.arg1);
                        if(scan == null){
                            if(msg.replyTo!=null){
                                scan = new Scan(msg.replyTo);
                                scan.id=msg.arg1;
                                scan.tellActivity(RESP_PROGRESS_ERR, "ScanService:RQST_PROGRESS no scan with specified id present");
                            }
                            break;
                        }
                        if(!(scan.started)){
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
                    if(scan == null){
                        if(msg.replyTo!=null){
                            scan = new Scan(msg.replyTo);
                            scan.id=msg.arg1;
                            scan.tellActivity(RESP_RESULTS_ERR, "ScanService:RESP_RESULTS_ERR no scan with specified id present");
                        }
                        break;
                    }

                    if(!(scan.started)) {
                        scan.tellActivity(RESP_RESULTS_ERR,"ScanService:RESP_RESULTS_ERR scan is not started");
                        break;
                    }

                    scan.tellActivity(RESP_RESULTS_OK,scan.results.toString());

                    //If the scan is finished, no reason keeping the results in the backyard
                    if(scan.finished){
                        scan = null;
                        scans.remove(msg.arg1);
                        //TODO TEMPORARY - NOTIFICATION METHOD ONLY SUPPORTS ONE SCAN
                        mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));
                    }

                    if(scans.size()==0)
                        stopSelf();

                    break;
                }

                case NOTIFY_ROOT_ACCESS:{
                    Log.d("UmitScanner","ScanService:NOTIFY_ROOT_ACCESS");

                    rootAccess = (msg.arg1==1);
                    if(!rootAccess){
                        //TODO NOTIFICATION ROOT FAIL
                        if(!nativeInstalled){ //TODO Cannot setup native without root access
                            if(pending_RQST_SCAN_ID){
                                for(Scan scan:scans.values())
                                    scan.tellActivity(RESP_SCAN_ID_ERR, "No root access");
                                pending_RQST_SCAN_ID=false;
                            }
                            stopSelf();
                        }
                    }

                    //TODO NOTIFICATION ROOT OK
                    //If we get here we have root :)

                    if(nativeInstalled) {
                        if(pending_RQST_SCAN_ID){
                            for(Scan scan:scans.values())
                                scan.tellActivity(RESP_SCAN_ID_OK,(rootAccess?1:0));
                            pending_RQST_SCAN_ID=false;
                        }
                        serviceReady=true;
                        mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));
                    }
                    else {
                        mNM.notify(serviceNotificationID, getNotification(R.string.service_setup_native));
                        executorService.execute(new SetupNativeRunnable(getApplicationContext(),msgrLocal.getBinder()));
                    }

                    break;
                }
                case NOTIFY_NATIVE_SETUP:{
                    Log.d("UmitScanner","ScanService:NOTIFY_NATIVE_SETUP");

                    //Will not go in here unless root acquired and native is not set up
                    if(msg.arg2!=NATIVE_SETUP_SUCCESS){
                        //NATIVE FAIL
                        if(pending_RQST_SCAN_ID){
                            for(Scan scan:scans.values())
                                scan.tellActivity(RESP_SCAN_ID_ERR, "Native binaries setup has failed:" + ((Bundle)msg.obj).getString("Info"));
                            pending_RQST_SCAN_ID=false;
                        }
                        stopSelf();
                    }

                    //NATIVE OK

                    nativeInstalled = true;
                    SharedPreferences settings = getSharedPreferences("native", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("nativeInstalled", nativeInstalled);
                    editor.commit();

                    if(pending_RQST_SCAN_ID) {
                        for(Scan scan:scans.values())
                            scan.tellActivity(RESP_SCAN_ID_OK,(rootAccess?1:0));
                        pending_RQST_SCAN_ID=false;
                    }

                    serviceReady=true;
                    mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));

                    break;
                }
                case NOTIFY_SCAN_FINISHED:{
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_FINISHED");

                    mNM.notify(serviceNotificationID,getNotification(R.string.service_scan_finished));
                    Scan scan = scans.get(msg.arg1);
                    scan.finished=true;
                    scan.tellActivity(NOTIFY_SCAN_FINISHED);
                    break;
                }
                case NOTIFY_SCAN_PROBLEM:{
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_PROBLEM");
                    mNM.notify(serviceNotificationID,getNotification(R.string.service_scan_problem));
                    Scan scan = scans.get(msg.arg1);
                    scan.tellActivity(NOTIFY_SCAN_PROBLEM,((Bundle)msg.obj).getString("Info"));
                    break;
                }

                default:
                    super.handleMessage(msg);
            }
        }
    }

    //---Thread-specific vars
    private class Scan {
        protected int id;

        protected Messenger messengerActivity;
        public StringBuffer results;
        public Future<?> future;
        public String arguments;

        public boolean started;
        public boolean finished;

        Scan(Messenger messengerActivity) {
            id = random.nextInt();
            results = new StringBuffer();
            this.messengerActivity=messengerActivity;
            started = false;
            finished = false;
        }

        public boolean tellActivity(int RESP_CODE){
            Log.d("UmitScanner", "tellActivity():RESP_CODE=" + RESP_CODE + " ID=" + id);
            try{
                messengerActivity.send(Message.obtain(null, RESP_CODE, id, 0));
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                return false;
            }
            return true;
        }

        public boolean tellActivity(int RESP_CODE, String info){
            Bundle bundle = new Bundle();
            bundle.putString("Info", info);
            Log.d("UmitScanner","tellActivity():RESP_CODE="+RESP_CODE+ " ID="+id+" info="+info);

            try{
                messengerActivity.send(Message.obtain(null, RESP_CODE, id, 0, bundle));
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                return false;
            }
            return true;
        }

        public boolean tellActivity(int RESP_CODE, int msg_arg2){
            Log.d("UmitScanner","tellActivity():RESP_CODE="+RESP_CODE+ " ID="+id+" msg.arg2="+msg_arg2);
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
                    new NmapScanServiceRunnable(id, msgrLocal.getBinder(),arguments,results,rootAccess));
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

    private Notification getNotification(int resStringID) {
        CharSequence text = getText(resStringID);
        Notification notification = new Notification(R.drawable.ic_stat_icon, text,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,
                new Intent(this,org.umit.ns.mobile.nmap.class),0);
        notification.setLatestEventInfo(this, getText(R.string.service_scan_name),
                text, contentIntent);
        return notification;
    }
}
