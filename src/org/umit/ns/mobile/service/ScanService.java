package org.umit.ns.mobile.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import org.apache.http.conn.ClientConnectionManager;
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
    private boolean nativeInstalationFinished;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("onBind()");

        int clientID = intent.getIntExtra("clientID",-1);
        if(clientID==-1) {
            log("onBind() no clientID in Intent. Finishing");
            return null;
        }

        Messenger messenger = intent.getParcelableExtra("messenger");
        if(messenger==null){
            log("onBind() no messenger in Intent. Finishing");
            return null;
        }

        Client client = clients.get(clientID);
        if(client==null){
            client = new Client(clientID, messenger);
            clients.put(clientID,client);
        } else {
            //Update the messenger
            client.setMessenger(messenger);
        }

        startService(new Intent(this, ScanService.class));
        return msgrLocal.getBinder();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private final class ScanServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case RQST_START_SCAN:{
                    int clientID = msg.arg1;
                    Client client = clients.get(clientID);
                    if(client==null) {
                        log("RQST_START_SCAN no such client.");
                        break;
                    }
                    if( rootAcqisitionFinished && nativeInstalled ){
                        client.startScan(msg.obj,rootAccess);
                    } else {
                        client.pendingStartScan=false;
                    }
                    break;
                }
                case RQST_STOP_SCAN:{
                    int clientID = msg.arg1;
                    Client client = clients.get(clientID);
                    if(client==null) {
                        log("RQST_START_SCAN no such client.");
                        break;
                    }

                    int scanID = msg.arg2;
                    client.stopScan(scanID);
                    break;
                }
                case NOTIFY_ROOT_ACCESS:{
                    break;
                }
                case NOTIFY_NATIVE_SETUP:{
                    break;
                }
                case NOTIFY_SCAN_PROGRESS:{
                    break;
                }
                case NOTIFY_SCAN_FINISHED:{
                    break;
                }
                case NOTIFY_SCAN_PROBLEM:{
                    break;
                }

                default:
                    super.handleMessage(msg);
            }
        }
    }

    public static void log(String logString) {
        android.util.Log.d("UmitScanner","ScanService."+logString);
    }

}

class Client {
    private int ID;
    private Messenger messenger;

    public Client(int clientID, Messenger messenger){
        this.ID=clientID;
        this.messenger=messenger;
    }

    public int getID() {
        return ID;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }

}