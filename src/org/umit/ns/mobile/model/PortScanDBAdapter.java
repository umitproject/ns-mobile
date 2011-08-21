/*
Android Network Scanner Umit Project
Copyright (C) 2011 Adriano Monteiro Marques

Author: Angad Singh <angad@angad.sg>

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 */

/**
 * @author angadsg
 * 
 * Model.
 * Implements a datastructure to save the scan results.
 * 
 * Saves in sqlite database.
 * 
 */

package org.umit.ns.mobile.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PortScanDBAdapter {
    
    /* Common */
    public static final String KEY_ROWID = "_id";
    public static final String KEY_TIME = "time";
    public static final String KEY_PROFILE = "profile";
    public static final String KEY_TYPE = "type";
    public static final String KEY_TARGET = "target";
    public static final String KEY_RANGE = "range";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_ARGS = "args";

    /* PortScan */
    public static final String KEY_OPEN = "open";
    public static final String KEY_FILTERED = "filtered";
    public static final String KEY_CLOSED = "closed";
    public static final String KEY_PROTOCOL = "protocol";
    //if portscan is started after host discovery, then it has a discovery_id
    //otherwise discovery_id = -1
    public static final String KEY_DISCOVERY_ID = "d_id"; 
    
    private static final String DATABASE_TABLE = "portscan";
        
    private Context context;
    private SQLiteDatabase database;
    private PortScanDatabase portscan_helper;

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public PortScanDBAdapter(Context context) {
        this.context = context;
    }

    public PortScanDBAdapter open() throws SQLException {
        portscan_helper = new PortScanDatabase(context);
        database = portscan_helper.getWritableDatabase();
        return this;
    }

    public void close() {
        portscan_helper.close();
    }

    public long save(String profile, String type, 
            String target, String range, String args, String open, String closed, 
            String filtered, String protocol, String d_id) {
        ContentValues init = createContentValues(now(), profile, type, 
                target, range, args, open, closed, filtered, protocol, d_id);        
        return database.insert(DATABASE_TABLE, null, init);
    }
    
    private ContentValues createContentValues(String time, 
            String profile, String type, String target,
            String range, String args, String open, 
            String closed, String filtered, 
            String protocol, String d_id) {
        ContentValues values = new ContentValues();
        values.put(KEY_TIME, time);
        values.put(KEY_PROFILE, profile);
        values.put(KEY_TYPE, type);
        values.put(KEY_TARGET, target);
        values.put(KEY_RANGE, range);
        values.put(KEY_ARGS, args);
        values.put(KEY_OPEN, open);
        values.put(KEY_CLOSED, closed);
        values.put(KEY_FILTERED, filtered);
        values.put(KEY_PROTOCOL, protocol);
        values.put(KEY_DISCOVERY_ID, d_id);
        return values;
    }
    
    public boolean deleteRow(long rowId) {
        return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public Cursor fetchAll() {
        return database.query(DATABASE_TABLE, new String[] { 
                KEY_ROWID, KEY_TIME, KEY_PROFILE, 
                KEY_TYPE, KEY_TARGET,  KEY_RANGE, 
                KEY_TOTAL, KEY_ARGS, KEY_OPEN, 
                KEY_CLOSED, KEY_FILTERED, KEY_PROTOCOL,
                KEY_DISCOVERY_ID}, null, null, null,
                null, null);
    }
    
    public Cursor fetch(long rowId) {
        Cursor mCursor = database.query(true, DATABASE_TABLE, new String[] {
                KEY_ROWID, KEY_TIME, KEY_PROFILE, KEY_TARGET, KEY_OPEN, KEY_CLOSED, KEY_FILTERED, KEY_ARGS, KEY_RANGE },
                KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
}