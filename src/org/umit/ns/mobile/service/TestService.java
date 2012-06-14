package org.umit.ns.mobile.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.view.View;
import android.widget.Button;
import org.umit.ns.mobile.R;


public class TestService extends Activity {
    static final int MSG_SAY_HELLO = 1;
    static final int MSG_SCAN = 2;
    static final int MSG_STOP_SCAN = 3;
    Messenger mService = null;
    boolean mBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testservice);

        Button button = (Button)findViewById(R.id.start);
        button.setOnClickListener(mStartListener);

        button = (Button)findViewById(R.id.bind);
        button.setOnClickListener(mBindListener);

        button = (Button)findViewById(R.id.sendmessage);
        button.setOnClickListener(mSendMessage);

        button = (Button)findViewById(R.id.unbind);
        button.setOnClickListener(mUnbindListener);

        button = (Button)findViewById(R.id.stop);
        button.setOnClickListener(mStopListener);


    }

    private View.OnClickListener mStartListener = new View.OnClickListener() {
        public void onClick(View v) {
            startService(new Intent(
                    "org.umit.ns.mobile.service.ScanService"));
        }
    };

    private View.OnClickListener mBindListener = new View.OnClickListener() {
        public void onClick(View v) {
            bindService(
                    new Intent("org.umit.ns.mobile.service.ScanService"),
                    mConnection,
                    Context.BIND_AUTO_CREATE);
        }
    };

    private View.OnClickListener mSendMessage= new View.OnClickListener() {
        public void onClick(View v) {
            if(mBound){
                Message msg = Message.obtain(null, MSG_SCAN, 0, 0);
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };;


    private View.OnClickListener mUnbindListener = new View.OnClickListener() {
        public void onClick(View v) {
            if(mBound) {
                unbindService(mConnection);
                mBound=false;
            }
        }
    };

    private View.OnClickListener mStopListener = new View.OnClickListener() {
        public void onClick(View v) {
            stopService(new Intent(
                    "org.umit.ns.mobile.service.ScanService"));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBound){
            Message msg = Message.obtain(null, MSG_STOP_SCAN, 0, 0);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}