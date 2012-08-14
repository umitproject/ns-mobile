package org.umit.ns.mobile.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.widget.Toast;
import org.umit.ns.mobile.R;
import org.umit.ns.mobile.ScanOverviewActivity;
import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.provider.Scanner;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanService extends Service implements ScanCommunication {
	private boolean enabled = true;

	private NotificationManager mNM;
	private final Messenger msgrLocal = new Messenger(new ScanServiceHandler());
	private static IBinder binderLocal;
	private static final ExecutorService executorService = Executors.newCachedThreadPool();

	private LinkedHashMap<Integer, ClientAdapter> clients = new LinkedHashMap<Integer, ClientAdapter>();

	private Random random = new Random();

	private boolean rootAccess = false;
	private boolean rootAcqisitionFinished = false;
	private boolean nativeInstalled = false;

	private static String nativeInstallDir;

	private static String scanResultsPath;
	private int serviceNotificationID;

	private ContentResolver contentResolver;

	private int scanCounter =0;

	@Override
	public void onCreate() {
		log("onCreate()");
		super.onCreate();
		android.os.Debug.waitForDebugger();

		binderLocal = msgrLocal.getBinder();

		//Check if the native binaries are already extracted
		SharedPreferences settings = getSharedPreferences("native", 0);
		nativeInstalled = settings.getBoolean("nativeInstalled", false);

		nativeInstallDir = getString(R.string.native_install_dir);
		scanResultsPath = nativeInstallDir + "/scanresults/";

		//Setup notifications and foreground
		serviceNotificationID = Math.abs(random.nextInt());
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		startForeground(serviceNotificationID, getNotification(R.string.service_ready));

		executorService.execute(new RootAcquisitionRunnable(msgrLocal.getBinder()));

		if (!nativeInstalled) {
			executorService.execute(
					new SetupNativeRunnable(getApplicationContext(), msgrLocal.getBinder(), nativeInstallDir));
			mNM.notify(serviceNotificationID, getNotification(R.string.service_setup_native));
		}

		//Only useful to the programmers so nothing we can do about it runtime

		if (0 != nativeInstallDir.compareTo(getFilesDir().toString()))
			log("Critical fault! nmap_install_dir string resource not matching package:" +
					getFilesDir().toString() + ":" + nativeInstallDir);

		contentResolver = getContentResolver();
	}

	@Override
	public IBinder onBind(Intent intent) {
		log("onBind()");
		if (!enabled)
			return null;

		int clientID = intent.getIntExtra("ClientID", -1);
		if (clientID == -1) {
			log("onBind() no ClientID in Intent. Finishing");
			return null;
		}
		log("onBind()-got ClientID!");

		Messenger messenger = intent.getParcelableExtra("Messenger");
		if (messenger == null) {
			log("onBind() no Messenger in Intent. Finishing");
			return null;
		}
		log("onBind()-got Messenger!");

		String action = intent.getStringExtra("Action");
		if(action==null){
			log("onBind() no Action in Intent. Finishing");
			return null;
		}
		log("onBind()-got Action! Oh yeah! :D");


		ClientAdapter client = clients.get(clientID);
		if (client == null) {
			client = new ClientAdapter(clientID, messenger,
					scanResultsPath,contentResolver,action);
			clients.put(clientID, client);
		} else {
			//Update the messenger and rebind (resend all messages in queue)
			client.rebind(messenger);
		}

		startService(new Intent(this, ScanService.class));
		return msgrLocal.getBinder();

	}

	@Override
	public boolean onUnbind(Intent intent) {
		log("onUnbind()");
		super.onUnbind(intent);
		if (!enabled)
			return false;

		int finishedScansCount = getContentResolver()
				.query(Scanner.SCANS_URI,null,null,null,null).getCount();

		if (clients.size() == 0 && scanCounter==0 && finishedScansCount==0)
			stopSelf();

		//Stop the service if there are no scans running
		boolean scan_running = false;
		for (ClientAdapter client : clients.values()) {
			if (client.scanRunning()) {
				scan_running = true;
				break;
			}
		}

		if (!scan_running)
			stopSelf();

		return false;
	}

	@Override
	public void onDestroy() {
		log("onDestroy()");
		super.onDestroy();
		if (!enabled)
			return;

		for (ClientAdapter client : clients.values())
			client.stopAllScans();
	}

	private final class ScanServiceHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (!enabled)
				return;

			switch (msg.what) {
				case RQST_START_SCAN: {
					int clientID = msg.arg1;
					ClientAdapter client = clients.get(clientID);
					if (client == null) {
						log("RQST_START_SCAN no client with ID" + clientID);
						break;
					}

					//A client cannot start two scans while Service is setting up
					//but on the other hand this is the first response from the service
					//and unless the activity manages the communication ok, with flags about
					//the message being sent or not, the user may click the start button many
					//times and in that way initiate many same scans
					if (client.pendingStartScan)
						break;

					Bundle bundle = (Bundle) msg.obj;

					String scanArguments = bundle.containsKey("ScanArguments") ?
							bundle.getString("ScanArguments") :
							null;

					if (rootAcqisitionFinished && nativeInstalled) {
						client.newScan(scanArguments);
						client.startScan(rootAccess);
						incrementScanCounter();
						mNM.notify(serviceNotificationID, getNotification("Running "+scanCounter+" scans."));
					} else {
						client.newScan(scanArguments);
						client.pendingStartScan = true;
					}

					break;
				}
				case RQST_STOP_SCAN: {
					int clientID = msg.arg1;
					ClientAdapter client = clients.get(clientID);
					if (client == null) {
						log("RQST_START_SCAN no client with ID" + clientID);
						break;
					}

					//Just in case some wild client sends it with a crazy scanID
					//Because otherwise if it doesn't get a scanID it doesn't
					//really have the logic to send stopScan ;-)
					if (client.pendingStartScan)
						break;

					int scanID = msg.arg2;
					boolean stopped = client.stopScan(scanID);
					if(stopped)
						decrementScanCounter();

					if(scanCounter==0)
						mNM.notify(serviceNotificationID,getNotification(R.string.service_ready));
					else
						mNM.notify(serviceNotificationID, getNotification("Running "+scanCounter+" scans."));

					break;
				}
				case NOTIFY_ROOT_ACCESS: {
					rootAccess = (msg.arg1 == 1);
					rootAcqisitionFinished = true;
					log("NOTIFY_ROOT_ACCESS=" + (rootAccess ? "Yes" : "No"));

					if(! rootAccess)
						Toast.makeText(getApplicationContext(),"Umit Network Scanner Service:\n" +
								"No root access, some options will be disabled.", Toast.LENGTH_SHORT).show();

					if (nativeInstalled) {
						for (ClientAdapter client : clients.values()) {
							if (client.pendingStartScan) {
								client.startScan(rootAccess);

								incrementScanCounter();
								mNM.notify(serviceNotificationID, getNotification("Running "+scanCounter+" scans."));

								client.pendingStartScan = false;
							}
						}
					}

					break;
				}
				case NOTIFY_NATIVE_SETUP: {
					String info = (((Bundle) msg.obj).containsKey("Info") ?
							((Bundle) msg.obj).getString("Info") : "");
					log("NOTIFY_NATIVE_SETUP:RESP=" + msg.arg1 + ";Info=" + info);

					if (msg.arg1 == NATIVE_SETUP_FAIL) {
						for (ClientAdapter client : clients.values()) {
							client.scanProblem(-1, "Cannot start scan, " +
									"native binaries setup has failed" + info);
							client.pendingStartScan = false;
						}
						//No native -> Disable the service
						stopSelf();
						clients.clear();
						enabled = false;
						mNM.notify(serviceNotificationID, getNotification(R.string.service_disabled));
						break;
					}

					nativeInstalled = true;
					SharedPreferences settings = getSharedPreferences("native", 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("nativeInstalled", nativeInstalled);
					editor.commit();

					mNM.notify(serviceNotificationID, getNotification(R.string.service_ready));

					if (rootAcqisitionFinished) {
						for (ClientAdapter client : clients.values()) {
							if (client.pendingStartScan) {
								client.startScan(rootAccess);

								incrementScanCounter();
								mNM.notify(serviceNotificationID, getNotification("Running "+scanCounter+" scans."));

								client.pendingStartScan = false;
							}
						}
					}
					break;
				}
				case NOTIFY_SCAN_PROGRESS: {
					//Unused because ContentProvider Provides :)
					break;
				}
				case NOTIFY_SCAN_FINISHED: {
					int scanID = msg.arg1;
					log("NOTIFY_SCAN_FINISHED: ScanID=" + scanID);
					ClientAdapter client = clients.get(ClientAdapter.getClientIDByScanID(scanID));
					if (client == null) {
						log("NOTIFY_SCAN_FINISHED: could not find client by that scanID");
						break;
					}
					client.scanFinished(scanID);

					decrementScanCounter();
					if(scanCounter==0)
						mNM.notify(serviceNotificationID,getNotification(R.string.service_ready));
					else
						mNM.notify(serviceNotificationID, getNotification("Running "+scanCounter+" scans."));

					break;
				}
				case NOTIFY_SCAN_PROBLEM: {
					int scanID = msg.arg1;
					ClientAdapter client = clients.get(ClientAdapter.getClientIDByScanID(scanID));
					if (client == null) {
						log("NOTIFY_SCAN_PROBLEM: could not find client by that scanID");
						break;
					}
					String info = ((Bundle) msg.obj).getString("Info");
					log("NOTIFY_SCAN_PROBLEM: info=" + info);
					client.scanProblem(scanID, info);

					decrementScanCounter();
					if(scanCounter==0)
						mNM.notify(serviceNotificationID,getNotification(R.string.service_ready));
					else
						mNM.notify(serviceNotificationID, getNotification("Running "+scanCounter+" scans."));

					break;
				}

				default:
					super.handleMessage(msg);
			}
		}
	}

	protected static IBinder getBinder() {
		return binderLocal;
	}

	protected static ExecutorService getExecutorService() {
		return executorService;
	}

	protected static String getNativeInstallDir() {
		return nativeInstallDir;
	}

	private static void log(String logString) {
		android.util.Log.d("UmitScanner", "ScanService." + logString);
	}

	private Notification getNotification(int resStringID) {
		CharSequence text = getText(resStringID);
		Notification notification = new Notification(R.drawable.icon_service, text,
				System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ScanOverviewActivity.class),0);
		notification.setLatestEventInfo(this, getText(R.string.service_scan_name),
				text, contentIntent);
		return notification;
	}

	private Notification getNotification(String resString) {
		CharSequence text = resString;
		Notification notification = new Notification(R.drawable.icon_service, text,
				System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ScanOverviewActivity.class),0);
		notification.setLatestEventInfo(this, getText(R.string.service_scan_name),
				text, contentIntent);
		return notification;
	}

	private void incrementScanCounter(){
		scanCounter++;
	}

	private void decrementScanCounter(){
		if(scanCounter>0)
			scanCounter--;
	}
}
