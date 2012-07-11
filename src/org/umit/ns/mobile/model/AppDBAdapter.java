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

public class AppDBAdapter {
    
    /* Common */
    public static final String KEY_ROWID = "id";
    public static final String KEY_TIME = "time";
    public static final String KEY_NATIVE = "native";
    
    private static final String DATABASE_TABLE = "appdata";
    
    private Context context;
    private SQLiteDatabase database;
    private AppDatabase appdata_helper;

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public AppDBAdapter(Context context) {
        this.context = context;
    }
    
    public AppDBAdapter open() throws SQLException {
        appdata_helper = new AppDatabase(context);
        database = appdata_helper.getWritableDatabase();
        return this;
    }

    public void close() {
        appdata_helper.close();
    }

    public long save(String n) {
        ContentValues init = createContentValues(now(), n);
        return database.insert(DATABASE_TABLE, null, init);
    }
    
    private ContentValues createContentValues(String time, 
            String n) {
            
        ContentValues values = new ContentValues();
        values.put(KEY_TIME, time);
        values.put(KEY_NATIVE, n);
        return values;
    }
    
    public boolean deleteRow(long rowId) {
        return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public Cursor fetchAll() {
        return database.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TIME, 
                KEY_NATIVE} , null, null, null, null, null);
    }
    
    public Cursor fetch(long rowId) {
        Cursor mCursor = database.query(true, DATABASE_TABLE, new String[] {
                KEY_ROWID, KEY_TIME, KEY_NATIVE },
                KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
}