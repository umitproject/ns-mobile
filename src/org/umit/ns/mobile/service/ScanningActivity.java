package org.umit.ns.mobile.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.*;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.umit.ns.mobile.R;


public class ScanningActivity extends Activity {
    static final int MSG_START_SCAN=1;
    static final int MSG_STOP_SCAN=2;
    static final int MSG_GET_RESULTS=3;

    Messenger mService = null;
    boolean mBound;

    TextView cmd;
    static TextView results;
    static boolean started;
    static Button start;

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
        setContentView(R.layout.scanningactivity);

        start = (Button)findViewById(R.id.startNmap);
        start.setOnClickListener(startScan);

        cmd = (TextView)findViewById(R.id.nmapcmd);
        results = (TextView)findViewById(R.id.nmapOutput);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBound){
            Message msg = Message.obtain(null, MSG_STOP_SCAN, 0, 0);
            try {
                mService.send(msg);
                unbindService(mConnection);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private View.OnClickListener startScan = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };
}