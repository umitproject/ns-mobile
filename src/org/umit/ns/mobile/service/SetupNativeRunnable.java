package org.umit.ns.mobile.service;

import android.content.Context;

import android.os.*;
import android.util.Log;
import org.umit.ns.mobile.R;
import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.api.ZipUtils;

import java.io.*;

//Will not execute unless root acquired
public class SetupNativeRunnable implements Runnable,ScanCommunication {

    private Messenger service;
    private Context context;

    SetupNativeRunnable(Context context, IBinder service) {
        this.service = new Messenger(service);
        this.context = context;
    }

    private void tellService(int status, String info) {
        Message msg;
        if (info==null)
            msg = Message.obtain(null, NOTIFY_NATIVE_SETUP,status,0);
        else {
            Bundle bundle = (new Bundle());
            bundle.putString("Info", info);
            msg = Message.obtain(null, NOTIFY_NATIVE_SETUP,status,0,bundle);
        }
        try {
            service.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Log.d("UmitScanner","SetupNativeRunnable:run()");

        try {
            java.lang.Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("chmod 777 /data/local" + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

        } catch (IOException e) {
            e.printStackTrace();
            tellService(PERMISSIONS_PROBLEM,"Unable to set some of the permissions. Please verify if you have root. Some of the features of the app will be disabled.");
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            tellService(UNZIPPING_FAILED,null);
            return;
        }

        //Copy the compressed file over
        if (! CopyNative(context, "/data/local/archive", R.raw.archive))
            return;

        //Extract the compressed file
        ZipUtils zu = new ZipUtils();
        boolean success = zu.unzipArchive(new File("/data/local/archive"), new File("/data/local/"));

        if(!success) {
            tellService(UNZIPPING_FAILED, "Some issue with extracting the zipfile. Contact the developer");
            return;
        }

        //Set to Executable permission
        try {
            java.lang.Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("chmod 755 /data/local/*" + "\n");
            os.writeBytes("chmod 755 /data/local/nmap/bin/nmap" + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            tellService(PERMISSIONS_PROBLEM,"Unable to set some of the permissions. Please verify if you have root. Some of the features of the app will be disabled.");
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            tellService(UNZIPPING_FAILED,null);
            return;
        }

        tellService(NATIVE_SETUP_SUCCESS,null);
    }

    protected boolean CopyNative(Context context, String path, int resource) {

        InputStream setdbStream = context.getResources().openRawResource(resource);
        try {
            byte[] bytes = new byte[setdbStream.available()];
            DataInputStream dis = new DataInputStream(setdbStream);
            dis.readFully(bytes);
            FileOutputStream setdbOutStream = new FileOutputStream(path);
            setdbOutStream.write(bytes);
            setdbOutStream.close();

            //Set executable permissions
            java.lang.Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("chmod 755 " + path + "\n");
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {
            tellService(UNZIPPING_FAILED, "Unable to Copy native binary " + e.getMessage());
            return false;
        }
        return true;
    }
}
