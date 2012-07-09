package org.umit.ns.mobile;

import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import org.umit.ns.mobile.provider.ScanOverview.Scan;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;

public class ScanOverviewActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Cursor cursor = getContentResolver().query(Scan.SCANS_URI, null, null, null, null);
        startManagingCursor(cursor);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                cursor,
                new String[] {Scan.SCAN_ID},
                new int[] {android.R.id.text1});
        setListAdapter(adapter);
    }

}