package org.umit.ns.mobile.service;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import org.umit.ns.mobile.api.ScanCommunication;

import java.io.DataInputStream;
import java.io.DataOutputStream;

class RootAcquisitionRunnable implements Runnable,ScanCommunication {

    private Messenger mService;

    RootAcquisitionRunnable (IBinder service) {
        this.mService = new Messenger(service);
    }

    public void run() {
        Log.d("UmitScanner", "RootAcquisitionRunnable:run()");
        java.lang.Process p;
        boolean root=false;

        try {
            // Preform su to get root privileges
            p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            DataInputStream is = new DataInputStream(p.getInputStream());

            if(null != os && null !=is) {
                os.writeBytes("id\n");
                os.flush();

                String currUid = is.readLine();
                boolean exitSu = false;
                if (null == currUid) {
                    root = false;
                    exitSu = false;
                    Log.d("UmitScanner", "Can't get root access or denied by user");
                }
                else if (true == currUid.contains("uid=0"))
                {
                    root = true;
                    exitSu = true;
                    Log.d("UmitScanner", "Root access granted");
                }
                else
                {
                    root = false;
                    exitSu = true;
                    Log.d("UmitScanner", "Root access rejected: " + currUid);
                }

                if (exitSu)
                {
                    os.writeBytes("exit\n");
                    os.flush();
                }
            }
        }
        catch (Exception e)
        {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output
            // stream after su failed, meaning that the device is not rooted

            root = false;
            Log.d("UmitScanner", "Root access rejected:" + e.getMessage());
        }

        int rootAccess = (root?1:0);
        Message msg = Message.obtain(null,NOTIFY_ROOT_ACCESS,rootAccess,0);

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d("UmitScanner",
                    "Caught Remote Exception while sending from ScanTask to ScanService:" + e.toString());
        }

    }
}
