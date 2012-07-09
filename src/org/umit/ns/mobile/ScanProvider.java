package org.umit.ns.mobile;

import android.content.*;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import org.umit.ns.mobile.model.ScanOverview;
import org.umit.ns.mobile.model.ScanOverview.Scan;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;

public class ScanProvider extends ContentProvider {

    private static final String TAG = "UmitScanner.ScanProvider";

    private static final String DATABASE_NAME = "scans.db";
    private static final int DATABASE_VERSION = 2;

    private static final String SCAN_OVERVIEW_TABLE_NAME="scanoverview";

    private static final UriMatcher uriMatcher;

    private static final int SCAN_OVERVIEW = 1;
    private static final int SCAN_RECORD = 2;

    private static HashMap<String,String> overviewProjection;
    private static HashMap<String,String> recordProjection;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ScanOverview.AUTHORITY, "scanoverview", SCAN_OVERVIEW);
        uriMatcher.addURI(ScanOverview.AUTHORITY, "#/#", SCAN_RECORD); //clientID/scanID

        overviewProjection=new HashMap<String, String>();
        overviewProjection.put(Scan._ID,Scan._ID);
        overviewProjection.put(Scan.CLIENT_ID,Scan.CLIENT_ID);
        overviewProjection.put(Scan.CLIENT_ACTION,Scan.CLIENT_ACTION);
        overviewProjection.put(Scan.SCAN_ID,Scan.SCAN_ID);
        overviewProjection.put(Scan.SCAN_PROGRESS,Scan.SCAN_PROGRESS);
        overviewProjection.put(Scan.SCAN_STATE,Scan.SCAN_STATE);

        recordProjection = new HashMap<String, String>();
        recordProjection.put(Scan._ID,Scan._ID );
        recordProjection.put(Scan.CLIENT_ID,Scan.CLIENT_ID );
        recordProjection.put(Scan.ROOT_ACCESS,Scan.ROOT_ACCESS );
        recordProjection.put(Scan.SCAN_ID,Scan.SCAN_ID );
        recordProjection.put(Scan.SCAN_ARGUMENTS,Scan.SCAN_ARGUMENTS );
        recordProjection.put(Scan.SCAN_PROGRESS,Scan.SCAN_PROGRESS );
        recordProjection.put(Scan.SCAN_STATE,Scan.SCAN_STATE );
        recordProjection.put(Scan.SCAN_RESULTS,Scan.SCAN_RESULTS );
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + SCAN_OVERVIEW_TABLE_NAME + " ("
                    + Scan._ID + " INTEGER PRIMARY KEY,"
                    + Scan.CLIENT_ID + " INTEGER,"
                    + Scan.CLIENT_ACTION + " TEXT,"
                    + Scan.ROOT_ACCESS + " INTEGER,"
                    + Scan.SCAN_ID + " INTEGER,"
                    + Scan.SCAN_ARGUMENTS + " TEXT,"
                    + Scan.SCAN_PROGRESS + " INTEGER,"
                    + Scan.SCAN_STATE + " INTEGER,"
                    + Scan.SCAN_RESULTS + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper databaseHelper;

    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SCAN_OVERVIEW_TABLE_NAME);

        switch(uriMatcher.match(uri)){
            case SCAN_OVERVIEW:
                qb.setProjectionMap(overviewProjection);
                break;
            case SCAN_RECORD:
                qb.setProjectionMap(recordProjection);
                qb.appendWhere( Scan.CLIENT_ID + "=" + uri.getPathSegments().get(0));
                qb.appendWhere(Scan.SCAN_ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                return null;
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = ScanOverview.DEFAULT_SORT_ORDER;
        } else {
            orderBy=sortOrder;
        }

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = qb.query(db,projection,selection,selectionArgs, null,null,orderBy);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case SCAN_OVERVIEW:
                return ScanOverview.CONTENT_TYPE;
            case SCAN_RECORD:
                return ScanOverview.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if(uriMatcher.match(uri) != SCAN_RECORD) {
            return null;
        }

        ContentValues values;
        if(initialValues !=null){
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        //TODO I should probably get these two from the uri
        if( ! values.containsKey(Scan.CLIENT_ID))
            return null;

        if( ! values.containsKey(Scan.SCAN_ID))
            return null;

        if( ! values.containsKey(Scan.CLIENT_ACTION))
            return null;

        if( ! values.containsKey(Scan.ROOT_ACCESS))
            return null;

        if( ! values.containsKey(Scan.SCAN_ARGUMENTS))
            return null;

        if( ! values.containsKey(Scan.SCAN_PROGRESS))
            return null;

        if( ! values.containsKey(Scan.SCAN_STATE))
            return null;

//        if( ! values.containsKey(Scan.SCAN_RESULTS))
//            return null;

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowID = db.insert(SCAN_OVERVIEW_TABLE_NAME, null, values);

        if(rowID > 0) {
            Uri scanUri = ContentUris.withAppendedId(Scan.SCANS_URI, rowID);
            getContext().getContentResolver().notifyChange(scanUri, null);
            return scanUri;
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if(uriMatcher.match(uri)!=SCAN_RECORD)
            return 0;

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int count;

        String scanID = uri.getPathSegments().get(0);
        String clientID = uri.getPathSegments().get(1);

        count = db.delete(SCAN_OVERVIEW_TABLE_NAME,
                Scan.CLIENT_ID + "=" + clientID + " AND " +
                Scan.SCAN_ID + "=" + scanID, whereArgs);

        getContext().getContentResolver().notifyChange(uri,null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if(uriMatcher.match(uri)!=SCAN_RECORD)
            return 0;

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int count;

        String clientID = uri.getPathSegments().get(0);
        String scanID = uri.getPathSegments().get(1);
        count = db.update(SCAN_OVERVIEW_TABLE_NAME, values,
                Scan.CLIENT_ID + "=" + clientID + " AND " +
                Scan.SCAN_ID + "=" + scanID, whereArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
