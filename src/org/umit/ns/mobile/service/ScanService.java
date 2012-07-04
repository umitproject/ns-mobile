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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//TODO Handle Runtime Changes (may kill the thread) temporarily portrait mode
//TODO Handle Redelivered Intent
//TODO Notification for Service or Notification for each scan, but first design GUI with Ad and check with Adriano

public class ScanService extends Service implements ScanCommunication{

    private NotificationManager mNM;
    private final Messenger msgrLocal = new Messenger(new ScanServiceHandler());
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private HashMap<Integer,Client> clients = new HashMap<Integer, Client>();
    private HashMap<Integer,Integer> scanID_clientID = new HashMap<Integer, Integer>();

    private Random random=new Random();

    private boolean rootAccess=false;
    private boolean rootAcqisitionFinished = false;
    private boolean nativeInstalled=false;
    private String nativeInstallDir;
    private String scanResultsPath;

    private boolean pending_RQST_REG_CLIENT=false;

    private int serviceNotificationID;

    public IBinder onBind(Intent intent) {
        Log.d("UmitScanner","ScanService.onBind()");
        startService(new Intent(this, ScanService.class));
        return msgrLocal.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        //If there are no clients, kill the service
        if(clients.size()==0)
            stopSelf();

        //if no running scans and all clients unbound then stop
        boolean running = false;
        for(Client client:clients.values())
            if(!client.noScan()){
                running=true;
                break;
            }

        if(!running)
            stopSelf();

        return false; //TODO onRebind implement!
    }



    @Override
    public void onCreate() {
        super.onCreate();
//        android.os.Debug.waitForDebugger();
        Log.d("UmitScanner","ScanService.onCreate()");
        //Check if the native binaries are already extracted
        SharedPreferences settings = getSharedPreferences("native", 0);
        nativeInstalled = settings.getBoolean("nativeInstalled", false);
        nativeInstallDir = getString(R.string.native_install_dir);
        scanResultsPath = nativeInstallDir + "/scanresults/";

        //Setup notifications and foreground
        serviceNotificationID=Math.abs(random.nextInt()); //Always positive, need it for file creation
        mNM=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(serviceNotificationID,getNotification(R.string.service_ready));

        executorService.execute(new RootAcquisitionRunnable(msgrLocal.getBinder()));
        if(!nativeInstalled) {
            executorService.execute(
                    new SetupNativeRunnable(getApplicationContext(),msgrLocal.getBinder(), nativeInstallDir) );
            mNM.notify(serviceNotificationID,getNotification(R.string.service_setup_native));
        }

        //Only useful to the programmers so nothing we can do about it runtime

        if(0 != nativeInstallDir.compareTo(getFilesDir().toString()))
            Log.e("UmitScanner", "Critical fault! nmap_install_dir string resource not matching package:" +
                    getFilesDir().toString()+":"+nativeInstallDir);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("UmitScanner","ScanService.onDestroy()");
        //Stop all scans, remove all Scan objects
        for(Client client:clients.values())
            client.stopAllScans();
        clients.clear();

        mNM.cancel(serviceNotificationID);

    }

    //TODO take account of bound activities? (for notification PendingIntent!!!)
    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RQST_REG_CLIENT:{
                    Log.d("UmitScanner","ScanService:RQST_REG_CLIENT");
                    if(msg.replyTo==null){
                        Log.d("UmitScanner","ScanService:RQST_REG_CLIENT no msg.replyTo present");
                        break;
                    }

                    Client client = new Client(msg.replyTo);
                    clients.put(client.id,client);

                    if(nativeInstalled && rootAcqisitionFinished) {
                        client.sendMsg(RESP_REG_CLIENT_OK, client.id, (rootAccess?1:0),null);
                    } else
                        pending_RQST_REG_CLIENT = true;

                    break;
                }
                case NOTIFY_ROOT_ACCESS:{
                    rootAccess = (msg.arg1==1);
                    rootAcqisitionFinished=true;
                    Log.d("UmitScanner","ScanService:NOTIFY_ROOT_ACCESS="+(rootAccess?"Yes":"No"));

                    if(nativeInstalled) {
                        if(pending_RQST_REG_CLIENT){
                            for(Client client:clients.values())
                                client.sendMsg(RESP_REG_CLIENT_OK, client.id, (rootAccess ? 1 : 0), null);
                            pending_RQST_REG_CLIENT=false;
                        }
                    }
                    break;
                }
                case NOTIFY_NATIVE_SETUP:{
                    String info = ( ((Bundle)msg.obj).containsKey("Info") ? ((Bundle)msg.obj).getString("Info") : "" );
                    Log.d("UmitScanner","ScanService:NOTIFY_NATIVE_SETUP:RESP="+msg.arg1+";Info="+info);

                    //If unsuccessful native_setup
                    if(msg.arg1==NATIVE_SETUP_FAIL){
                        if(pending_RQST_REG_CLIENT){
                            for(Client client:clients.values())
                                client.sendMsg(RESP_REG_CLIENT_ERR,0,0, "Native binaries setup has failed:" + ((Bundle)msg.obj).getString("Info"));
                            pending_RQST_REG_CLIENT=false;
                        }
                        //No native, no scanning service
                        stopSelf();
                    }

                    //NATIVE OK
                    nativeInstalled = true;
                    SharedPreferences settings = getSharedPreferences("native", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("nativeInstalled", nativeInstalled);
                    editor.commit();

                    mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));

                    if(rootAcqisitionFinished) {
                        if(pending_RQST_REG_CLIENT) {
                            for(Client client:clients.values())
                                client.sendMsg(RESP_REG_CLIENT_OK, client.id, (rootAccess ? 1 : 0), null);
                            pending_RQST_REG_CLIENT=false;
                        }
                    }
                    break;
                }
                case RQST_NEW_SCAN:{
                    Log.d("UmitScanner","ScanService:RQST_NEW_SCAN");
                    Client client = clients.get(msg.arg1);
                    if (client==null) {
                        break;
                    }
                    int scanID = client.newScan();
                    scanID_clientID.put(scanID,client.id);
                    break;
                }
                case RQST_START_SCAN:{
                    Log.d("UmitScanner","ScanService:RQST_START_SCAN");
                    Client client = clients.get(msg.arg1);
                    if (client==null) {
                        break;
                    }
                    if(msg.obj==null || !((Bundle)msg.obj).containsKey("ScanArguments")) {
                        client.sendMsg(RESP_START_SCAN_ERR, msg.arg1, 0, "ScanService:RQST_START_SCAN no msg.obj:ScanArguments string present");
                        break;
                    }
                    boolean started = client.startScan(msg.arg2, ((Bundle)msg.obj).getString("ScanArguments"));

                    //TODO TEMPORARY - NOTIFICATION METHOD ONLY SUPPORTS ONE SCAN
                    mNM.notify(serviceNotificationID, getNotification(R.string.service_scan_running));
                    break;
                }
                case RQST_STOP_SCAN:{
                    Log.d("UmitScanner","ScanService:RQST_STOP_SCAN");
                    Client client = clients.get(msg.arg1);
                    if (client==null) {
                        break;
                    }
                    if(client.stopScan(msg.arg2)){
                        scanID_clientID.remove(msg.arg2);
                    }
                    //TODO TEMPORARY - NOTIFICATION METHOD ONLY SUPPORTS ONE SCAN
                    mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));
                    break;
                }
                case RQST_REBIND_CLIENT:{
                    Log.d("UmitScanner","ScanService:RQST_REBIND_CLIENT");

                    if(msg.replyTo==null){
                        Log.d("UmitScanner","ScanService:RQST_REBIND_CLIENT no msg.replyTo present");
                        break;
                    }

                    Client client = clients.get(msg.arg1);
                    if(client==null) {
                        client = new Client(msg.replyTo,msg.arg1);
                        clients.put(client.id,client);
                        Log.d("UmitScanner","ScanService:RQST_REBIND_CLIENT - No such client ID:"+msg.arg1+" Creating new one.");
                    } else {
                        client.rebind(msg.replyTo);
                    }
                    client.sendMsg(RESP_REBIND_CLIENT_OK,0,0,null);
                    break;
                }
                case NOTIFY_SCAN_FINISHED:{
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_FINISHED");
                    //TODO singlethreaded notification model
                    mNM.notify(serviceNotificationID,getNotification(R.string.service_scan_finished));

                    int clientID = scanID_clientID.get(msg.arg1);
                    Client client = clients.get(clientID);

                    if(client!=null){
                        client.finishScan(msg.arg1);
                    }
                    scanID_clientID.remove(msg.arg1);

                    break;
                }
                case NOTIFY_SCAN_PROBLEM:{
                    Log.d("UmitScanner","ScanService:NOTIFY_SCAN_PROBLEM");
                    mNM.notify(serviceNotificationID,getNotification(R.string.service_scan_problem));

                    int clientID = scanID_clientID.get(msg.arg1);
                    Client client = clients.get(clientID);

                    if(client!=null){
                        client.problemScan(msg.arg1, ((Bundle)msg.obj).getString("Info"));
                    }
                    break;
                }
                case NOTIFY_SCAN_PROGRESS:{
                    Log.d("UmitScanner", "ScanService:NOTIFY_SCAN_PROGRESS="+msg.arg2) ;
                    int clientID = scanID_clientID.get(msg.arg1);
                    Client client = clients.get(clientID);
                    if(client!=null)
                        client.sendMsg(NOTIFY_SCAN_PROGRESS, msg.arg1, msg.arg2, null);
                }
                case RQST_UNREG_CLIENT: {
                    Client client = clients.get(msg.arg1);
                    if(client!=null) {
                        client.sendMsg(RESP_UNREG_CLIENT_OK,0,0,null);
                        client.stopAllScans();
                        clients.remove(msg.arg1);
                    }
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class Client {
        public int id;
        protected Messenger messenger;

        private HashMap<Integer,Scan> scans = new HashMap<Integer, Scan>();
        public Queue<Message> unsentMsgs = new LinkedList<Message>();

        public Client(Messenger messenger){
            id=Math.abs(random.nextInt());
            this.messenger=messenger;
        }

        public Client(Messenger messenger,int id){
            this.id=id;
            this.messenger=messenger;
        }

        public int newScan(){
            Scan scan = new Scan();
            scans.put(scan.id, scan);
            sendMsg(RESP_NEW_SCAN_OK, scan.id, 0, scan.resultsFile);
            return scan.id;
        }

        public boolean startScan(int scanID, String scanArguments){
            Scan scan = scans.get(scanID);
            if(scan==null) {
                sendMsg(RESP_START_SCAN_ERR,scanID,0,"ScanService:RQST_START_SCAN no scan with specified id present");
                return false;
            }
            if(scan.started) {
                sendMsg(RESP_START_SCAN_ERR,scan.id,0,"ScanService:RQST_START_SCAN Scan already started");
                return false;
            }
            scan.start(scanArguments);
            sendMsg(RESP_START_SCAN_OK,scan.id,0,null);
            return true;
        }

        public boolean stopScan(int scanID){
            Scan scan = scans.get(scanID);
            if(scan==null) {
                sendMsg(RESP_STOP_SCAN_ERR,scanID,0,"ScanService:RQST_STOP_SCAN no scan with specified id present");
                return false;
            }
            scan.stop();
            sendMsg(RESP_STOP_SCAN_OK,scan.id,0,null);
            scans.remove(scanID);
            return true;
        }

        public void finishScan(int scanID) {
            sendMsg(NOTIFY_SCAN_FINISHED, scanID, 0, null);
            scans.remove(scanID);
        }

        public void sendMsg(int MSG_CODE, int MSG_ARG1, int MSG_ARG2, String info){
            Message msg;

            if(info==null){
                msg=Message.obtain(null,MSG_CODE,MSG_ARG1,MSG_ARG2);
                Log.d("UmitScanner","Sending message:"+MSG_CODE+":"+MSG_ARG1+":"+MSG_ARG2);
            }
            else {
                Bundle bundle = new Bundle();
                bundle.putString("Info",info);
                msg=Message.obtain(null,MSG_CODE,MSG_ARG1,MSG_ARG2,bundle);
                Log.d("UmitScanner","Sending message:"+MSG_CODE+":"+MSG_ARG1+":"+MSG_ARG2+":"+info+";");
            }

            try{
                messenger.send(msg);
            } catch(RemoteException e) {
                Log.d("UmitScanner", "ScanService could not send Message to Activity");
                if(MSG_CODE==NOTIFY_SCAN_PROGRESS) {
                    //We don't want thousands of Progress messages in the queue, just one.
                    Scan scan = scans.get(msg.arg1);
                    if(scan==null)
                        return;
                    scan.pendingProgress=true;
                    scan.setProgress(msg.arg2);
                } else {
                    unsentMsgs.offer(msg);
                }
            }
        }

        public void rebind(Messenger messenger) {
            this.messenger=messenger;
            //Clear out pendingProgress on all scans
            for(Scan scan:scans.values()){
               if(scan.pendingProgress) {
                   sendMsg(NOTIFY_SCAN_PROGRESS, scan.id, scan.getProgress(), null);
                   scan.pendingProgress=false;
               }
            }
            while(!unsentMsgs.isEmpty()){
                Message msg = unsentMsgs.poll();

                try{
                    messenger.send(msg);
                } catch(RemoteException e) {
                    Log.d("UmitScanner", "ScanService could not send Message to Client when rebinding");
                }
            }
        }

        public void problemScan(int scanID, String info) {
            sendMsg(NOTIFY_SCAN_PROBLEM, scanID, 0, info);
            scans.remove(scanID);
        }

        public void stopAllScans() {
            for(Scan scan:scans.values()){
                scan.stop();
            }
            scans.clear();
        }

        public boolean noScan() {
            return scans.size()==0;
        }
    }
    //---Thread-specific vars
    private class Scan {
        protected int id;

        protected String resultsFile;
        public Future<?> future;
        public String arguments;
        private int progress=0;
        private boolean pendingProgress=false;
        public boolean started;

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        Scan() {
            id = Math.abs(random.nextInt());
            resultsFile=scanResultsPath+id;
            started = false;
        }

        public void start(String scanArguments) {
            this.arguments = scanArguments;

            //Start scan with submit so we can call futureScan.cancel() if we want to stop it
            future = executorService.submit(
                    new NmapScanServiceRunnable(id, msgrLocal.getBinder(),arguments,resultsFile,rootAccess,nativeInstallDir));
            started=true;
        }

        public boolean stop() {
            started = false;
            return future.cancel(true);
        }

    }
    //--\Thread-specific vars

    private Notification getNotification(int resStringID) {
        CharSequence text = getText(resStringID);
        Notification notification = new Notification(R.drawable.icon_service, text,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,
                new Intent(this,org.umit.ns.mobile.nmap.class),0);
        notification.setLatestEventInfo(this, getText(R.string.service_scan_name),
                text, contentIntent);
        return notification;
    }
}
