package org.umit.ns.mobile.service;

import android.content.Context;
import android.os.*;
import android.util.Log;
import org.umit.ns.mobile.R;
import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.api.ZipUtils;

import java.io.*;

//SetupNative doesn't require root access
class SetupNativeRunnable implements Runnable, ScanCommunication {

	private final Messenger service;
	private final Context context;
	private final String nativeInstallDir;

	SetupNativeRunnable(Context context, IBinder service, String nativeInstallDir) {
		this.service = new Messenger(service);
		this.context = context;
		this.nativeInstallDir = nativeInstallDir;
	}

	private void tellService(int status, String info) {
		Message msg;
		if (info == null)
			msg = Message.obtain(null, NOTIFY_NATIVE_SETUP, status, 0);
		else {
			Bundle bundle = (new Bundle());
			bundle.putString("Info", info);
			msg = Message.obtain(null, NOTIFY_NATIVE_SETUP, status, 0, bundle);
		}
		try {
			service.send(msg);
		} catch (RemoteException e) {
			Log.e("UmitScanner", e.toString());
		}
	}

	public void run() {
		Log.d("UmitScanner", "SetupNativeRunnable:run()");

		if (!CopyResourceToPath(context, nativeInstallDir + "/archive", R.raw.archive)) {
			//notification to the service is handled internally to CopyNative
			return;
		}

		ZipUtils zu = new ZipUtils();
		boolean success = zu.unzipArchive(new File(nativeInstallDir + "/archive"), new File(nativeInstallDir + "/"));

		if (!success) {
			tellService(NATIVE_SETUP_FAIL, "Failed unzipping archive with binary executables");
			return;
		}

		try {
			java.lang.Process p = Runtime.getRuntime().exec("sh");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("cd " + nativeInstallDir + "\n");

			os.writeBytes("mkdir " + nativeInstallDir + "/scanresults \n");

			os.writeBytes("chmod 755 " + nativeInstallDir + "/*" + "\n");
			os.writeBytes("chmod 755 " + nativeInstallDir + "/nmap/bin/*" + "\n");

			os.writeBytes("exit\n");
			os.flush();
			p.waitFor();
			os.close();

		} catch (IOException e) {
			tellService(NATIVE_SETUP_FAIL, "Unable to set some of the permissions. Please verify if you have root. Some of the features of the app will be disabled.");
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
			tellService(NATIVE_SETUP_FAIL, "Interrupted process for setting permissions");
			return;
		}

		tellService(NATIVE_SETUP_SUCCESS, "Succeeded setting up native executables");
	}

	protected boolean CopyResourceToPath(Context context, String path, int resource) {

		InputStream setdbStream = context.getResources().openRawResource(resource);

		try {
			byte[] bytes = new byte[setdbStream.available()];
			DataInputStream dis = new DataInputStream(setdbStream);
			dis.readFully(bytes);
			FileOutputStream setdbOutStream = new FileOutputStream(path);
			setdbOutStream.write(bytes);
			setdbOutStream.close();
		} catch (Exception e) {
			tellService(NATIVE_SETUP_FAIL, "Unable to Copy native binary. Reason: " + e.getMessage());
			return false;
		}
		return true;
	}
}
