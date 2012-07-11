package org.umit.ns.mobile;

import org.umit.ns.mobile.R;
import org.umit.ns.mobile.model.DiscoveryDBAdapter;
import org.umit.ns.mobile.model.PortScanDBAdapter;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SavedScans extends Activity {
    
    ListView lv;
    TextView list_host;
    DiscoveryDBAdapter discoverydb;
    PortScanDBAdapter portscandb;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.savedscans);
        
        discoverydb = new DiscoveryDBAdapter(nsandroid.defaultInstance);
        discoverydb.open();
        
        portscandb = new PortScanDBAdapter(nsandroid.defaultInstance);
        portscandb.open();
        
        Cursor all = discoverydb.fetchAll();
        startManagingCursor(all);

        String[] f = new String[] {DiscoveryDBAdapter.KEY_PROFILE};
        int[] t = new int[] { R.id.host };
        SimpleCursorAdapter saves = new SimpleCursorAdapter(this, R.layout.list_item, all, f, t);
        lv = (ListView) findViewById(R.id.listView1);
        lv.setAdapter(saves);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (discoverydb != null) {
            discoverydb.close();
        }
    }

}