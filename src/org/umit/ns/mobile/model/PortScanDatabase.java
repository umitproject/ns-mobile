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
 * CRUD for Database.
 * 
 */

package org.umit.ns.mobile.model;

import org.umit.ns.mobile.nsandroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PortScanDatabase extends SQLiteOpenHelper {
    
    public static final String DATABASE_NAME = "port_scan";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_CREATE = "CREATE TABLE portscan (id INTEGER PRIMARY KEY autoincrement, time TEXT, date TEXT, profile TEXT, type TEXT, target TEXT, args TEXT, open TEXT, closed TEXT, range TEXT, filtered TEXT, protocol TEXT, total NUMERIC)";
    

    public PortScanDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        nsandroid.resultPublish("Upgrading databse from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS portscan");
        onCreate(db);
    }
}
