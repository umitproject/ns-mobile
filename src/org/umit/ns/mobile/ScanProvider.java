package org.umit.ns.mobile;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.umit.ns.mobile.model.Scanner;
import org.umit.ns.mobile.model.Scanner.Details;
import org.umit.ns.mobile.model.Scanner.Hosts;
import org.umit.ns.mobile.model.Scanner.Scans;
import org.umit.ns.mobile.util.StringToHex;

import java.lang.Integer;import java.lang.Override;import java.lang.String;import java.util.HashMap;

public class ScanProvider extends ContentProvider {
	private static final String LOG_TAG = "UmitScanner.ScanProvider";

	private static final String DATABASE_NAME = "scans.db";
	private static final int DATABASE_VERSION = 1;

	private static final String SCANS_TABLE_NAME = "scans";

	private DatabaseHelper databaseHelper;

	private static final UriMatcher uriMatcher;

	private static final int MATCH_URI_SCANS = 1;
	private static final int MATCH_URI_SCAN = 2;
	private static final int MATCH_URI_HOSTS = 3;
	private static final int MATCH_URI_HOST = 4;
	private static final int MATCH_URI_DETAILS = 5;
	private static final int MATCH_URI_DETAIL = 6;

	private static HashMap<String, String> scansProjection;
	private static HashMap<String, String> hostsProjection;
	private static HashMap<String, String> detailsProjection;

	private final static StringToHex sth = new StringToHex();


	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		uriMatcher.addURI(Scanner.AUTHORITY, "scans", MATCH_URI_SCANS);
		uriMatcher.addURI(Scanner.AUTHORITY, "scans/#/#", MATCH_URI_SCAN);

		uriMatcher.addURI(Scanner.AUTHORITY, "hosts/#/#", MATCH_URI_HOSTS); //clientID/scanID-hosts
		uriMatcher.addURI(Scanner.AUTHORITY, "hosts/#/#/*", MATCH_URI_HOST);

		uriMatcher.addURI(Scanner.AUTHORITY, "details/#/#/*", MATCH_URI_DETAILS);
		uriMatcher.addURI(Scanner.AUTHORITY, "details/#/#/*/*", MATCH_URI_DETAIL);

		scansProjection = new HashMap<String, String>();
		scansProjection.put(Scans._ID, Scans._ID);
		scansProjection.put(Scans.CLIENT_ID, Scans.CLIENT_ID);
		scansProjection.put(Scans.CLIENT_ACTION, Scans.CLIENT_ACTION);
		scansProjection.put(Scans.SCAN_ID, Scans.SCAN_ID);
		scansProjection.put(Scans.SCAN_STATE, Scans.SCAN_STATE);
		scansProjection.put(Scans.TASK_PROGRESS, Scans.TASK_PROGRESS);
		scansProjection.put(Scans.TASK, Scans.TASK);
		scansProjection.put(Scans.ROOT_ACCESS, Scans.ROOT_ACCESS);
		scansProjection.put(Scans.SCAN_ARGUMENTS, Scans.SCAN_ARGUMENTS);
		scansProjection.put(Scans.ERRORMESSAGE, Scans.ERRORMESSAGE);

		hostsProjection = new HashMap<String, String>();
		hostsProjection.put(Hosts._ID, Hosts._ID);
		hostsProjection.put(Hosts.IP, Hosts.IP);
		hostsProjection.put(Hosts.NAME, Hosts.NAME);
		hostsProjection.put(Hosts.STATE, Hosts.STATE);
		hostsProjection.put(Hosts.OS, Hosts.OS);
		hostsProjection.put(Hosts.DETAILS_TABLE_NAME, Hosts.DETAILS_TABLE_NAME);

		detailsProjection = new HashMap<String, String>();
		detailsProjection.put(Details._ID, Details._ID);
		detailsProjection.put(Details.NAME, Details.NAME);
		detailsProjection.put(Details.DATA, Details.DATA);
		detailsProjection.put(Details.TYPE, Details.TYPE);
		detailsProjection.put(Details.STATE, Details.STATE);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + SCANS_TABLE_NAME + " ("
					+ Scans._ID + " INTEGER PRIMARY KEY,"
					+ Scans.CLIENT_ID + " INTEGER,"
					+ Scans.CLIENT_ACTION + " TEXT,"
					+ Scans.ROOT_ACCESS + " INTEGER,"
					+ Scans.SCAN_ID + " INTEGER,"
					+ Scans.SCAN_ARGUMENTS + " TEXT,"
					+ Scans.TASK_PROGRESS + " INTEGER,"
					+ Scans.SCAN_STATE + " INTEGER,"
					+ Scans.TASK + " TEXT,"
					+ Scans.ERRORMESSAGE + " TEXT,"
					+ Scans.HOSTS_TABLE_NAME + " TEXT"
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
			onCreate(db);
		}
	}


	@Override
	public boolean onCreate() {
		databaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		SQLiteDatabase db = databaseHelper.getReadableDatabase();

		switch (uriMatcher.match(uri)) {
			case MATCH_URI_SCANS: {
				qb.setTables(SCANS_TABLE_NAME);
				qb.setProjectionMap(scansProjection);
				break;
			}
			case MATCH_URI_SCAN: {
				qb.setTables(SCANS_TABLE_NAME);
				qb.setProjectionMap(scansProjection);
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				qb.appendWhere(Scans.CLIENT_ID + "=" + clientID + " AND " + Scans.SCAN_ID + "=" + scanID);
				break;
			}
			case MATCH_URI_HOSTS: {
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostsTableName = "h_"+clientID + "_" + scanID;
				//If the hosts table doesn't exits
				Cursor tmp = db.query("sqlite_master",new String[]{"name"},"type='table' and name='"+hostsTableName+"'",null,null,null,null);
				if(tmp.getCount()!=1)
					return null;
				qb.setTables(hostsTableName);
				qb.setProjectionMap(hostsProjection);
				break;
			}
			case MATCH_URI_HOST: {
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostsTableName = "h_"+clientID + "_" + scanID;
				//If the hosts table doesn't exits
				Cursor tmp = db.query("sqlite_master",new String[]{"name"},"type='table' and name='"+hostsTableName+"'",null,null,null,null);
				if(tmp.getCount()!=1)
					return null;

				qb.setTables(hostsTableName);
				qb.setProjectionMap(hostsProjection);
				String hostIP = uri.getPathSegments().get(3);
				qb.appendWhere(Hosts.IP + "='" + hostIP+"'");
				break;
			}
			case MATCH_URI_DETAILS: {
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostIP = uri.getPathSegments().get(3);
				String detailsTableName = "d_"+clientID + "_" + scanID + "_" + sth.convertStringToHex(hostIP);
				//If the hosts table doesn't exits
				Cursor tmp = db.query("sqlite_master",new String[]{"name"},"type='table' and name='"+detailsTableName+"'",null,null,null,null);
				if(tmp.getCount()!=1)
					return null;
				qb.setTables(detailsTableName);
				qb.setProjectionMap(detailsProjection);
				break;
			}
			case MATCH_URI_DETAIL: {
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostIP = uri.getPathSegments().get(3);
				String detailsTableName = "d_"+clientID + "_" + scanID + "_" + sth.convertStringToHex(hostIP);
				Cursor tmp = db.query("sqlite_master",new String[]{"name"},"type='table' and name='"+detailsTableName+"'",null,null,null,null);
				if(tmp.getCount()!=1)
					return null;
				qb.setTables(detailsTableName);
				qb.setProjectionMap(detailsProjection);
				qb.appendWhere(Details.NAME + "='" + uri.getPathSegments().get(4)+"'");
				break;
			}
			default:
				return null;
		}

		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = Scanner.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
			case MATCH_URI_SCANS: {
				return Scanner.SCANS_TYPE;
			}
			case MATCH_URI_SCAN: {
				return Scanner.SCAN_TYPE;
			}
			case MATCH_URI_HOSTS: {
				return Scanner.HOSTS_TYPE;
			}
			case MATCH_URI_HOST: {
				return Scanner.HOST_TYPE;
			}
			case MATCH_URI_DETAILS: {
				return Scanner.DETAILS_TYPE;
			}
			case MATCH_URI_DETAIL: {
				return Scanner.DETAIL_TYPE;
			}
			default:
				return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			Log.e(LOG_TAG, "insert: No ContentValues specified");
			return null;
		}

		switch (uriMatcher.match(uri)) {
			case MATCH_URI_SCAN: {
				if (false == values.containsKey(Scans.CLIENT_ACTION)) {
					Log.e(LOG_TAG, "scans.insert: No CLIENT_ACTION specified");
					return null;
				}
				if (false == values.containsKey(Scans.ROOT_ACCESS)) {
					Log.e(LOG_TAG, "scans.insert: No ROOT_ACCESS specified");
					return null;
				}
				if (false == values.containsKey(Scans.SCAN_STATE)) {
					Log.e(LOG_TAG, "scans.insert: No SCAN_STATE specified");
					return null;
				}
				if (false == values.containsKey(Scans.SCAN_ARGUMENTS)) {
					Log.e(LOG_TAG, "scans.insert: No SCAN_ARGUMENTS specified");
					return null;
				}

				String clientIDString = uri.getPathSegments().get(1);
				String scanIDString = uri.getPathSegments().get(2);
				String hostsTableName = "h_" + clientIDString + "_" + scanIDString;

				int clientID = Integer.parseInt(clientIDString);
				int scanID = Integer.parseInt(scanIDString);

				values.put(Scans.CLIENT_ID, clientID);
				values.put(Scans.SCAN_ID, scanID);
				values.put(Scans.HOSTS_TABLE_NAME, hostsTableName);

				long rowID = db.insert(SCANS_TABLE_NAME, null, values);
				if (rowID > 0) {

					//Create a hosts table too scanID_clientID
					db.execSQL("CREATE TABLE " + hostsTableName + " ("
							+ Hosts._ID + " INTEGER PRIMARY KEY,"
							+ Hosts.IP + " TEXT,"
							+ Hosts.NAME + " TEXT,"
							+ Hosts.STATE + " INTEGER,"
							+ Hosts.OS + " INTEGER,"
							+ Hosts.DETAILS_TABLE_NAME + " TEXT"
							+ ");");
					getContext().getContentResolver().notifyChange(uri, null);
					return uri;
				}

				return null;
			}
			case MATCH_URI_HOST: {
				if (false == values.containsKey(Hosts.IP)) {
					Log.e(LOG_TAG, "hosts.insert: No IP specified");
					return null;
				}
				if (false == values.containsKey(Hosts.STATE)) {
					Log.e(LOG_TAG, "hosts.insert: No STATE specified");
					return null;
				}
				if (false == values.containsKey(Hosts.NAME)) {
					values.put(Hosts.NAME,"");
				}
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostsTableName = "h_" + clientID + "_" + scanID;

				String hostIP = uri.getPathSegments().get(3);
				values.put(Hosts.IP, hostIP);

				String detailsTableName = "d_" + clientID + "_" + scanID + "_" + sth.convertStringToHex(hostIP);

				values.put(Hosts.DETAILS_TABLE_NAME, detailsTableName);

				long rowID = db.insert(hostsTableName, null, values);

				if (rowID > 0) {

					//If host is up create details table
//					if (values.containsKey(Hosts.STATE) &&
//							values.getAsInteger(Hosts.STATE) == Hosts.STATE_UP) {

						db.execSQL("CREATE TABLE " + detailsTableName + " ("
								+ Details._ID + " INTEGER PRIMARY KEY,"
								+ Details.NAME + " TEXT,"
								+ Details.DATA + " TEXT,"
								+ Details.TYPE + " TEXT,"
								+ Details.STATE + " INTEGER"
								+ ");");
//					}
					getContext().getContentResolver().notifyChange(uri, null);
					return uri;
				}

				return null;
			}
			case MATCH_URI_DETAIL: {
				if (false == values.containsKey(Details.NAME)) {
					Log.e(LOG_TAG, "details.insert: No NAME specified");
					return null;
				}
				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostIP = uri.getPathSegments().get(3);
				String detailName = uri.getPathSegments().get(4);
				String detailsTableName = "d_" + clientID + "_" + scanID + "_" + sth.convertStringToHex(hostIP);

				values.put(Details.NAME,detailName);

				long rowID = db.insert(detailsTableName, null, values);

				if (rowID > 0) {
					getContext().getContentResolver().notifyChange(uri, null);
					return uri;
				}
				return null;
			}
			default:
				return null;
		}
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		if (uriMatcher.match(uri) != MATCH_URI_SCAN) {
			Log.e(LOG_TAG, "delete. It is only available for hosts tables");
			return -1;
		}

		String clientIDString = uri.getPathSegments().get(1);
		String scanIDString = uri.getPathSegments().get(2);
		String hostsTableName = "h_" + clientIDString + "_" + scanIDString;

		SQLiteDatabase db = databaseHelper.getWritableDatabase();

		//Delete the details table for each host
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(hostsTableName);
		Cursor c = qb.query(db, new String[]{Hosts.DETAILS_TABLE_NAME}, null, null, null, null, null);
		if (null != c) {
			int index = c.getColumnIndex(Hosts.DETAILS_TABLE_NAME);
			if (index != -1)
				while (c.moveToNext()) {
					String detailsTableName = c.getString(index);
					db.execSQL("DROP TABLE IF EXISTS " + detailsTableName + ";");
				}
		}

		//Delete the hosts table
		db.execSQL("DROP TABLE IF EXISTS " + hostsTableName + ";");

		//Clean up a little :)
		db.execSQL("VACUUM;");

		//Delete entry from scans table
		int count;
		//It's ok to send the strings, the int's would be converted anyway
		count = db.delete(SCANS_TABLE_NAME,
				Scans.CLIENT_ID + "=" + clientIDString + " AND " +
						Scans.SCAN_ID + "=" + scanIDString, whereArgs);

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
			case MATCH_URI_SCAN: {
				Cursor c = query(uri,null,null,null,null);

				if(c.getCount()==0) {
					return (insert(uri,values)==null ? -1: 1);
				}

				String clientIDString = uri.getPathSegments().get(1);
				String scanIDString = uri.getPathSegments().get(2);

				count = db.update(SCANS_TABLE_NAME, values,
						Scans.CLIENT_ID + "=" + clientIDString +
								" AND " + Scans.SCAN_ID + "=" + scanIDString,
						whereArgs);
				break;
			}
			case MATCH_URI_HOST: {
				Cursor c = query(uri,null,null,null,null);
				if(c.getCount()==0){
					return (insert(uri,values)==null ? -1: 1);
				}

				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostsTableName = "h_" + clientID + "_" + scanID;

				String hostIP = uri.getPathSegments().get(3);

				count = db.update(hostsTableName, values,
						Hosts.IP + "='" + hostIP+"'", whereArgs);

				//If host is up create details table
				if (count > 0 && values.containsKey(Hosts.STATE) &&
						(values.getAsInteger(Hosts.STATE) == Hosts.STATE_UP)) {

					String detailsTableName = "d_" +clientID + "_" + scanID + "_" + sth.convertStringToHex(hostIP);

					db.execSQL("CREATE TABLE IF NOT EXISTS " + detailsTableName + " ("
							+ Details._ID + " INTEGER PRIMARY KEY,"
							+ Details.NAME + " TEXT,"
							+ Details.DATA + " TEXT,"
							+ Details.TYPE + " TEXT,"
							+ Details.STATE + " INTEGER"
							+ ");");
				}
				break;
			}
			case MATCH_URI_DETAIL: {
				Cursor c = query(uri,null,null,null,null);
				if(c.getCount()==0){
					return (insert(uri,values)==null ? -1: 1);
				}

				String clientID = uri.getPathSegments().get(1);
				String scanID = uri.getPathSegments().get(2);
				String hostIP = uri.getPathSegments().get(3);
				String detailName = uri.getPathSegments().get(4);
				String detailsTableName = "d_" + clientID + "_" + scanID + "_" + sth.convertStringToHex(hostIP);

				count = db.update(detailsTableName, values,
						Details.NAME + "='" + detailName+"'", whereArgs);
				break;
			}
			default:
				return -1;
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
