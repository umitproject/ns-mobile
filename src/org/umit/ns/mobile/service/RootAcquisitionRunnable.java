package org.umit.ns.mobile.service;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import org.umit.ns.mobile.api.ScanCommunication;

import java.io.DataOutputStream;
import java.io.IOException;

public class RootAcquisitionRunnable implements Runnable,ScanCommunication {
    private Messenger mService;
    RootAcquisitionRunnable (IBinder service) {
        this.mService = new Messenger(service);
    }
    public void run() {
        java.lang.Process p;
        boolean root;

        try {
            // Preform su to get root privileges
            p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("echo \"Do I have root?\" >/system/sd/UmitSuCheck.txt\n");

            // Close the terminal
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    root=true;
                }
                else {
                    root= false;
                }
            } catch (InterruptedException e) {
                root= false;
            }
        } catch (IOException e) {
            root =false;
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
